package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.mapper.BookshelfMapper;
import com.sdjz.novel.pojo.dto.reader.BookshelfProgressDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.BookshelfEntryVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.service.BookshelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookshelfServiceImpl implements BookshelfService {

    @Autowired
    private final BookshelfMapper bookshelfMapper;
    @Autowired
    private final BookMapper bookMapper;

    @Override
    public List<BookshelfEntryVO> list(Long userId) {
        return bookshelfMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public BookshelfEntryVO save(
            Long userId,
            Long bookId,
            BookshelfProgressDTO dto
    ) {
        BookSummaryVO book = bookMapper.selectBookById(bookId);

        if (book == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "作品不存在或已下架"
            );
        }

        Long chapterId = dto == null ? null : dto.getChapterId();
        String chapterName = null;

        if (chapterId != null) {
            ChapterSummaryVO chapter =
                    bookshelfMapper.selectChapterOfBook(bookId, chapterId);

            if (chapter == null) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "章节不存在或不属于该作品"
                );
            }

            chapterName = chapter.getChapterName();
        }

        bookshelfMapper.upsert(
                userId,
                bookId,
                chapterId,
                chapterName
        );

        return bookshelfMapper.selectOne(userId, bookId);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long bookId) {
        bookshelfMapper.deleteByUserAndBook(userId, bookId);
    }
}