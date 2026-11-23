package com.sdjz.novel.service.support;

import com.sdjz.novel.common.util.RedisUtil;
import com.sdjz.novel.mapper.BookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookVisitCounterTest {

    private static final String DIRTY_SET = "novel:visit:dirty";

    private RedisUtil redisUtil;
    private BookMapper bookMapper;
    private BookVisitCounter counter;

    @BeforeEach
    void setUp() {
        redisUtil = mock(RedisUtil.class);
        bookMapper = mock(BookMapper.class);
        counter = new BookVisitCounter(redisUtil, bookMapper);
    }

    @Test
    void recordFallsBackToDatabaseWhenRedisFails() {
        when(redisUtil.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("Redis down"));

        counter.record(7L);

        verify(bookMapper).increaseVisit(7L);
    }

    @Test
    void flushMergesBufferedCountsIntoOneUpdatePerBook() {
        when(redisUtil.members(DIRTY_SET)).thenReturn(new LinkedHashSet<>(Set.of("1", "2")));
        when(redisUtil.execute(any(), eq(bufferKeys(1L)), any(Object[].class))).thenReturn(5L);
        when(redisUtil.execute(any(), eq(bufferKeys(2L)), any(Object[].class))).thenReturn(3L);

        counter.flush();

        verify(bookMapper).increaseVisitBy(1L, 5L);
        verify(bookMapper).increaseVisitBy(2L, 3L);
    }

    @Test
    void failedWritebackReturnsCountsToBufferInsteadOfLosingThem() {
        when(redisUtil.members(anyString())).thenReturn(new LinkedHashSet<>(Set.of("1")));
        when(redisUtil.execute(any(), anyList(), any(Object[].class))).thenReturn(4L);
        when(bookMapper.increaseVisitBy(1L, 4L)).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(counter::flush);

        // 一次取走、一次放回，增量不会凭空消失
        verify(redisUtil, times(2)).execute(any(), anyList(), any(Object[].class));
    }

    private static List<String> bufferKeys(long bookId) {
        return List.of("novel:visit:buf:" + bookId, DIRTY_SET);
    }
}
