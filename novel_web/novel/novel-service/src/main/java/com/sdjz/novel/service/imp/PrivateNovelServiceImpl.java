package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.util.Batches;
import com.sdjz.novel.mapper.PrivateNovelMapper;
import com.sdjz.novel.pojo.dto.reader.PrivateProgressDTO;
import com.sdjz.novel.pojo.dto.reader.PrivateImportDTO;
import com.sdjz.novel.pojo.entity.UserNovel;
import com.sdjz.novel.pojo.entity.UserNovelChapter;
import com.sdjz.novel.pojo.vo.reader.BookBriefVO;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.service.PrivateNovelService;
import com.sdjz.novel.service.PublicationService;
import com.sdjz.novel.service.imports.PrivateNovelTextParser;
import com.sdjz.novel.service.imports.PrivateNovelTextParser.ParsedChapter;
import com.sdjz.novel.service.imports.PrivateNovelTextParser.ParsedNovel;
import com.sdjz.novel.service.storage.OssCoverStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivateNovelServiceImpl implements PrivateNovelService {
    private final PrivateNovelMapper mapper;
    private final PrivateNovelTextParser parser;
    private final PublicationService publicationService;
    private final OssCoverStorage coverStorage;
    private final TransactionTemplate transactionTemplate;

    @Override
    public PrivateNovelVO importTxt(Long userId, MultipartFile file) {
        return importTxt(userId, file, new PrivateImportDTO());
    }

    @Override
    public PrivateNovelVO importTxt(Long userId, MultipartFile file, PrivateImportDTO options) {
        return importTxt(userId, file, options, null);
    }

    /**
     * TXT 导入。
     *
     * <p>这里<b>刻意不加 @Transactional</b>。原来整个方法被一个事务包住，
     * 20 MB 文本解析、OSS 封面上传、最多 5000 条章节插入全在同一个事务里：
     * <ul>
     *   <li>OSS 上传是一次外部 HTTP 调用，慢起来几秒，期间数据库连接一直被占着；</li>
     *   <li>Hikari 的 connection-timeout 是 3 秒，并发导入时后面的请求直接拿不到连接；</li>
     *   <li>解析失败要做的是「不写库」，而不是「回滚」，本来就不需要事务。</li>
     * </ul>
     * 现在只有真正落库的那一段进事务（{@link TransactionTemplate}），
     * 并且用编程式事务而不是 @Transactional —— 因为同类内部调用不会走代理，
     * 拆成私有方法加注解是无效的。
     */
    @Override
    public PrivateNovelVO importTxt(Long userId, MultipartFile file, PrivateImportDTO options, MultipartFile cover) {
        PrivateImportDTO request = options == null ? new PrivateImportDTO() : options;
        String visibility = request.getVisibility() == null ? "PRIVATE" : request.getVisibility();
        if (!"PRIVATE".equals(visibility) && !"PUBLIC".equals(visibility)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "作品可见性不合法");
        }
        if ("PUBLIC".equals(visibility)) {
            publicationService.validatePublicFields(request.getBookName(), request.getAuthorName(),
                    request.getCategoryId(), request.getRightsConfirmed());
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择非空 TXT 文件");
        }
        if (file.getSize() > PrivateNovelTextParser.MAX_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能超过 20 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("B0000", "读取上传文件失败");
        }

        // 解析与上传都在事务外：失败了什么都还没写，不需要回滚
        ParsedNovel parsed = parser.parse(file.getOriginalFilename(), bytes);
        String coverUrl = coverStorage.upload(cover);

        Long novelId = transactionTemplate.execute(status ->
                persistNovel(userId, parsed, request, visibility, coverUrl));
        return getNovel(userId, novelId);
    }

    /** 只在事务内被调用：主表 + 章节 + 投稿记录，要么全成要么全不成。 */
    private Long persistNovel(Long userId, ParsedNovel parsed, PrivateImportDTO request,
                              String visibility, String coverUrl) {
        UserNovel novel = new UserNovel();
        novel.setUserId(userId);
        novel.setBookName(parsed.bookName());
        novel.setCoverUrl(coverUrl);
        novel.setOriginalFileName(parsed.originalFileName());
        novel.setEncoding(parsed.encoding());
        novel.setFileSize(parsed.fileSize());
        novel.setWordCount(parsed.wordCount());
        novel.setChapterCount(parsed.chapters().size());
        mapper.insertNovel(novel);

        writeChapters(novel.getId(), parsed.chapters());
        if ("PUBLIC".equals(visibility)) {
            publicationService.createSubmission(userId, novel.getId(), request, coverUrl);
        }
        return novel.getId();
    }

    /**
     * 分批写章节：5000 章从 5000 次往返降到 10 次左右。
     *
     * <p>切批规则见 {@link Batches#split(List, java.util.function.ToLongFunction)}：
     * 每批最多 500 条、正文累计不超过 2 MB，两者任一触顶就换下一批。
     */
    private void writeChapters(Long novelId, List<ParsedChapter> parsedChapters) {
        List<UserNovelChapter> chapters = new ArrayList<>(parsedChapters.size());
        int chapterNum = 1;
        for (ParsedChapter parsedChapter : parsedChapters) {
            UserNovelChapter chapter = new UserNovelChapter();
            chapter.setNovelId(novelId);
            chapter.setChapterNum(chapterNum++);
            chapter.setChapterName(parsedChapter.name());
            chapter.setContent(parsedChapter.content());
            chapter.setWordCount(parsedChapter.wordCount());
            chapters.add(chapter);
        }

        Long firstChapterId = null;
        for (List<UserNovelChapter> batch : Batches.split(chapters, PrivateNovelServiceImpl::contentBytes)) {
            mapper.insertChapters(batch);
            // 主键已由 useGeneratedKeys 回填，第一张取第一个即可
            if (firstChapterId == null && !batch.isEmpty()) {
                firstChapterId = batch.get(0).getId();
            }
        }
        if (firstChapterId != null) {
            mapper.setFirstChapter(novelId, firstChapterId);
        }
    }

    static long contentBytes(UserNovelChapter chapter) {
        return chapter.getContent() == null ? 0 : chapter.getContent().length();
    }

    @Override
    public PageVO<PrivateNovelVO> getPage(Long userId, int page, int size) {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = size < 1 ? 20 : Math.min(size, 100);
        long offset = ((long) normalizedPage - 1) * normalizedSize;
        List<PrivateNovelVO> list = mapper.selectPage(userId, offset, normalizedSize);
        list.forEach(this::signCover);
        return new PageVO<>(list, normalizedPage, normalizedSize, mapper.count(userId));
    }

    @Override
    public PrivateNovelVO getNovel(Long userId, Long novelId) {
        PrivateNovelVO novel = mapper.selectNovel(userId, novelId);
        if (novel == null) throw notFound();
        signCover(novel);
        return novel;
    }

    @Override
    public PageVO<ChapterSummaryVO> getChapters(Long userId, Long novelId, int page, int size) {
        getNovel(userId, novelId);
        int normalizedPage = Math.max(1, page);
        int normalizedSize = size < 1 ? 100 : Math.min(size, 200);
        long offset = ((long) normalizedPage - 1) * normalizedSize;
        List<ChapterSummaryVO> list = mapper.selectChapterPage(userId, novelId, offset, normalizedSize);
        return new PageVO<>(list, normalizedPage, normalizedSize, mapper.countChapters(userId, novelId));
    }

    @Override
    public ChapterContentVO getChapter(Long userId, Long novelId, Long chapterId) {
        PrivateNovelVO novel = getNovel(userId, novelId);
        UserNovelChapter chapter = mapper.selectChapter(userId, novelId, chapterId);
        if (chapter == null) throw notFound();

        BookBriefVO book = new BookBriefVO();
        book.setId(novel.getId());
        book.setBookName(novel.getBookName());
        ChapterSummaryVO summary = new ChapterSummaryVO();
        summary.setId(chapter.getId());
        summary.setBookId(novelId);
        summary.setChapterNum(chapter.getChapterNum());
        summary.setChapterName(chapter.getChapterName());
        summary.setWordCount(chapter.getWordCount());
        summary.setCreateTime(chapter.getCreateTime());
        summary.setUpdateTime(chapter.getUpdateTime());

        ChapterContentVO result = new ChapterContentVO();
        result.setBook(book);
        result.setChapter(summary);
        result.setContent(chapter.getContent());
        result.setPreviousChapterId(mapper.selectPreviousChapterId(novelId, chapter.getChapterNum()));
        result.setNextChapterId(mapper.selectNextChapterId(novelId, chapter.getChapterNum()));
        return result;
    }

    @Override
    public void updateProgress(Long userId, Long novelId, PrivateProgressDTO dto) {
        getNovel(userId, novelId);
        if (dto == null || dto.getChapterId() == null
                || mapper.selectChapterMeta(userId, novelId, dto.getChapterId()) == null) {
            throw notFound();
        }
        mapper.upsertProgress(userId, novelId, dto.getChapterId());
    }

    @Override
    @Transactional
    public void delete(Long userId, Long novelId) {
        publicationService.onPrivateDelete(userId, novelId);
        if (mapper.deleteNovel(userId, novelId) == 0) throw notFound();
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "作品或章节不存在");
    }

    private void signCover(PrivateNovelVO novel) {
        if (novel.getPicUrl() != null) novel.setPicUrl(coverStorage.signedUrl(novel.getPicUrl()));
    }
}
