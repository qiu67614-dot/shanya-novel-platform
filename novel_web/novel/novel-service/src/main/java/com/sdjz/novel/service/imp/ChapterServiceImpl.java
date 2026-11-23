package com.sdjz.novel.service.imp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.util.JsonRedisCache;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.mapper.ChapterMapper;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private static final Duration CHAPTER_TTL = Duration.ofMinutes(5);
    private static final TypeReference<ChapterContentVO> CHAPTER_TYPE = new TypeReference<>() {};

    private final ChapterMapper chapterMapper;
    private final BookMapper bookMapper;
    private final JsonRedisCache readerCache;

    @Override
    public PageVO<ChapterSummaryVO> getChapters(Long bookId, int page, int size) {
        // 先校验书籍是否存在且上架
        if (bookMapper.selectBookById(bookId) == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "作品不存在");
        }

        // 分页参数修正，目录支持大数量查询但设置上限
        page = page < 1 ? 1 : page;
        size = size < 1 ? 20 : Math.min(size, 10000);

        int offset = (page - 1) * size;
        List<ChapterSummaryVO> list = chapterMapper.selectChapterPage(bookId, offset, size);
        Long total = chapterMapper.countChapters(bookId);

        return new PageVO<>(list, page, size, total);
    }

    @Override
    public ChapterContentVO getChapterContent(Long chapterId) {
        ChapterContentVO content = readerCache.getOrLoad("chapter:" + chapterId, CHAPTER_TYPE, CHAPTER_TTL,
                () -> loadChapterContent(chapterId));
        if (content == null || content.getBook() == null
                || bookMapper.selectBookById(content.getBook().getId()) == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "章节不存在");
        }
        return content;
    }

    private ChapterContentVO loadChapterContent(Long chapterId) {
        ChapterContentVO chapterContent = chapterMapper.selectChapterContent(chapterId);
        if (chapterContent == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "章节不存在");
        }

        // 基于章节序号查询上下章，不依赖ID连续性
        Integer chapterNum = chapterContent.getChapter().getChapterNum();
        Long bookId = chapterContent.getChapter().getBookId();

        chapterContent.setPreviousChapterId(chapterMapper.selectPreviousChapterId(bookId, chapterNum));
        chapterContent.setNextChapterId(chapterMapper.selectNextChapterId(bookId, chapterNum));

        return chapterContent;
    }
}
