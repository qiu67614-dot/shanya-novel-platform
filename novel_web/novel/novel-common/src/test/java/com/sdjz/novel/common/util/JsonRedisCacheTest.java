package com.sdjz.novel.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdjz.novel.common.config.JacksonConfig;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.*;

class JsonRedisCacheTest {

    private static final String KEY = "novel:reader:v1:categories:all";
    private static final String LOCK_KEY = "novel:lock:reader:v1:categories:all";
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    private RedisUtil redisUtil;
    private JsonRedisCache cache;

    @BeforeEach
    void setUp() {
        redisUtil = mock(RedisUtil.class);
        cache = new JsonRedisCache(redisUtil, new ObjectMapper());
    }

    @Test
    void returnsCachedValueWithoutCallingLoader() {
        when(redisUtil.get(KEY)).thenReturn("[\"玄幻\"]");
        AtomicInteger loads = new AtomicInteger();

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> {
            loads.incrementAndGet();
            return List.of("都市");
        });

        assertEquals(List.of("玄幻"), result);
        assertEquals(0, loads.get());
        verify(redisUtil, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void storesLoadedValueWithJitteredExpiry() {
        givenLockAcquired();

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
        // 600s ±10% 的抖动，落在 [540s, 660s] 之间
        verify(redisUtil).set(eq(KEY), eq("[\"都市\"]"),
                longThat(millis -> millis >= 540_000L && millis <= 660_000L),
                eq(TimeUnit.MILLISECONDS));
        verify(redisUtil).deleteIfMatch(eq(LOCK_KEY), anyString());
    }

    @Test
    void cachesNullResultWithShortTtlToBlockPenetration() {
        givenLockAcquired();

        assertNull(cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> null));
        // 空值只允许缓存 30 秒以内，避免新数据被挡住太久
        verify(redisUtil).set(eq(KEY), eq("null"), longThat(ttl -> ttl <= 33_000L), eq(TimeUnit.MILLISECONDS));

        when(redisUtil.get(KEY)).thenReturn("null");
        AtomicInteger loads = new AtomicInteger();

        assertNull(cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> {
            loads.incrementAndGet();
            return List.of("都市");
        }));
        assertEquals(0, loads.get(), "空值哨兵命中时不应回源");
    }

    @Test
    void concurrentMissesLoadOnlyOnce() throws Exception {
        givenLockAcquired();
        cache = new JsonRedisCache(redisUtil, new ObjectMapper());
        AtomicInteger loads = new AtomicInteger();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<String> result = List.of("都市");
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> {
                        loads.incrementAndGet();
                        sleepQuietly(200);
                        return result;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, loads.get(), "同一 key 的并发未命中只应有一次真正回源");
    }

    @Test
    void waitsForOtherInstanceInsteadOfHittingDatabase() {
        // 第一次读是未命中，等到 50ms 后对方实例已经把结果写好
        when(redisUtil.get(KEY)).thenReturn(null, "[\"玄幻\"]");
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        AtomicInteger loads = new AtomicInteger();

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> {
            loads.incrementAndGet();
            return List.of("都市");
        });

        assertEquals(List.of("玄幻"), result);
        assertEquals(0, loads.get());
    }

    @Test
    void loadsDirectlyWhenRedisIsUnavailable() {
        when(redisUtil.get(KEY)).thenThrow(new IllegalStateException("Redis unavailable"));
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
    }

    @Test
    void replacesMalformedCachedJson() {
        when(redisUtil.get(KEY)).thenReturn("not-json");
        givenLockAcquired();

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
        verify(redisUtil).set(eq(KEY), eq("[\"都市\"]"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void returnsLoadedValueWhenRedisWriteFails() {
        givenLockAcquired();
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(redisUtil).set(eq(KEY), eq("[\"都市\"]"), anyLong(), any());

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, TEN_MINUTES, () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
    }

    @Test
    void readsChapterWithConfiguredDateTimeFormat() throws Exception {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().jackson2ObjectMapperBuilderCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();
        cache = new JsonRedisCache(redisUtil, mapper);

        ChapterSummaryVO chapter = new ChapterSummaryVO();
        chapter.setId(100L);
        chapter.setUpdateTime(LocalDateTime.of(2026, 11, 14, 20, 0));
        ChapterContentVO content = new ChapterContentVO();
        content.setChapter(chapter);
        content.setContent("正文");
        when(redisUtil.get("novel:reader:v1:chapter:100")).thenReturn(mapper.writeValueAsString(content));

        ChapterContentVO result = cache.getOrLoad("chapter:100", new TypeReference<>() {},
                Duration.ofMinutes(5), () -> { throw new AssertionError("cache miss"); });

        assertEquals("正文", result.getContent());
        assertEquals(chapter.getUpdateTime(), result.getChapter().getUpdateTime());
    }

    /** 模拟「本实例抢到了分布式锁」：读未命中 + SETNX 成功 */
    private void givenLockAcquired() {
        when(redisUtil.get(anyString())).thenReturn(null);
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
