package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivateNovelServiceImpl implements PrivateNovelService {
    private final PrivateNovelMapper mapper;
    private final PrivateNovelTextParser parser;
    private final PublicationService publicationService;
    private final OssCoverStorage coverStorage;

    @Override
    @Transactional
    public PrivateNovelVO importTxt(Long userId, MultipartFile file) {
        return importTxt(userId, file, new PrivateImportDTO());
    }

    @Override
    @Transactional
    public PrivateNovelVO importTxt(Long userId, MultipartFile file, PrivateImportDTO options) {
        return importTxt(userId, file, options, null);
    }

    @Override
    @Transactional
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
        ParsedNovel parsed = parser.parse(file.getOriginalFilename(), bytes);
        String coverUrl = coverStorage.upload(cover);

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

        int chapterNum = 1;
        for (ParsedChapter parsedChapter : parsed.chapters()) {
            UserNovelChapter chapter = new UserNovelChapter();
            chapter.setNovelId(novel.getId());
            chapter.setChapterNum(chapterNum++);
            chapter.setChapterName(parsedChapter.name());
            chapter.setContent(parsedChapter.content());
            chapter.setWordCount(parsedChapter.wordCount());
            mapper.insertChapter(chapter);
            if (chapterNum == 2) mapper.setFirstChapter(novel.getId(), chapter.getId());
        }
        if ("PUBLIC".equals(visibility)) publicationService.createSubmission(userId, novel.getId(), request, coverUrl);
        return getNovel(userId, novel.getId());
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
