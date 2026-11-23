package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.mapper.HistoryMapper;
import com.sdjz.novel.pojo.dto.reader.ProgressUpdateDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.HistoryEntryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final HistoryMapper historyMapper;
    private final BookMapper bookMapper;

    @Override
    public PageVO<HistoryEntryVO> getHistory(Long userId, Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1
                ? DEFAULT_SIZE
                : Math.min(size, MAX_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;

        List<HistoryEntryVO> list = historyMapper.selectPage(
                userId,
                offset,
                normalizedSize
        );
        Long total = historyMapper.count(userId);

        return new PageVO<>(list, normalizedPage, normalizedSize, total);
    }

    @Override
    @Transactional
    public void updateProgress(Long userId, ProgressUpdateDTO dto) {
        BookSummaryVO book = bookMapper.selectBookById(dto.getBookId());
        if (book == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "作品不存在或已下架"
            );
        }

        ChapterSummaryVO chapter = historyMapper.selectChapterOfBook(
                dto.getBookId(),
                dto.getChapterId()
        );
        if (chapter == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "章节不存在或不属于该作品"
            );
        }

        historyMapper.upsert(
                userId,
                dto.getBookId(),
                dto.getChapterId(),
                chapter.getChapterName()
        );

        // 只同步已存在的书架记录，不因阅读自动加入书架。
        historyMapper.updateBookshelfProgress(
                userId,
                dto.getBookId(),
                dto.getChapterId(),
                chapter.getChapterName()
        );
    }
}
