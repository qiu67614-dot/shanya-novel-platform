package com.sdjz.novelservice;

import com.sdjz.novel.common.util.JsonRedisCache;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.mapper.BookCategoryMapper;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.mapper.ChapterMapper;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.BookBriefVO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.service.imp.BookServiceImpl;
import com.sdjz.novel.service.imp.ChapterServiceImpl;
import com.sdjz.novel.service.support.BookVisitCounter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReaderServiceTest {
    @Test
    void homeProvidesThreeReaderSections() {
        BookMapper books = mock(BookMapper.class);
        JsonRedisCache cache = loadingCache();
        when(books.selectHomeLatest()).thenReturn(List.of());
        when(books.selectHomeHot()).thenReturn(List.of());
        when(books.selectHomeUpdated()).thenReturn(List.of());

        var home = new BookServiceImpl(books, mock(BookCategoryMapper.class), cache,
                mock(BookVisitCounter.class)).getHomeBooks();
        assertNotNull(home.getLatest());
        assertNotNull(home.getHot());
        assertNotNull(home.getUpdated());
    }

    @Test
    void chapterContainsAdjacentNavigation() {
        ChapterMapper chapters = mock(ChapterMapper.class);
        ChapterSummaryVO summary = new ChapterSummaryVO();
        summary.setBookId(1L);
        summary.setChapterNum(2);
        ChapterContentVO content = new ChapterContentVO();
        BookBriefVO brief = new BookBriefVO();
        brief.setId(1L);
        content.setBook(brief);
        content.setChapter(summary);
        content.setContent("正文");
        when(chapters.selectChapterContent(100L)).thenReturn(content);
        when(chapters.selectPreviousChapterId(1L, 2)).thenReturn(99L);
        when(chapters.selectNextChapterId(1L, 2)).thenReturn(101L);

        BookMapper books = mock(BookMapper.class);
        when(books.selectBookById(1L)).thenReturn(new BookSummaryVO());
        var result = new ChapterServiceImpl(chapters, books, loadingCache())
                .getChapterContent(100L);
        assertEquals(99L, result.getPreviousChapterId());
        assertEquals(101L, result.getNextChapterId());
        assertEquals("正文", result.getContent());
    }

    @Test
    void takenDownBookChapterIsNotFoundWhenLoading() {
        ChapterMapper chapters = mock(ChapterMapper.class);
        // selectChapterContent 的 SQL 带 book_info.status = 0，作品下架后这里就是 null
        when(chapters.selectChapterContent(100L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ChapterServiceImpl(chapters, mock(BookMapper.class), loadingCache())
                        .getChapterContent(100L));
        assertEquals("B0003", error.getCode());
        verify(chapters, never()).selectPreviousChapterId(anyLong(), anyInt());
    }

    private JsonRedisCache loadingCache() {
        JsonRedisCache cache = mock(JsonRedisCache.class);
        when(cache.getOrLoad(anyString(), any(), any(), any())).thenAnswer(invocation -> {
            Supplier<?> loader = invocation.getArgument(3);
            return loader.get();
        });
        return cache;
    }
}
