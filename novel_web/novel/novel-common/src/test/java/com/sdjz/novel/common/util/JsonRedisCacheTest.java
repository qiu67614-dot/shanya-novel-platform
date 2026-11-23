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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JsonRedisCacheTest {

    private static final String KEY = "novel:reader:v1:categories:all";
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

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

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE, Duration.ofMinutes(10), () -> {
            loads.incrementAndGet();
            return List.of("都市");
        });

        assertEquals(List.of("玄幻"), result);
        assertEquals(0, loads.get());
        verify(redisUtil, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void storesLoadedValueWithExpiry() {
        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE,
                Duration.ofMinutes(10), () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
        verify(redisUtil).set(KEY, "[\"都市\"]", 600, TimeUnit.SECONDS);
    }

    @Test
    void fallsBackToDatabaseWhenRedisReadFails() {
        when(redisUtil.get(KEY)).thenThrow(new IllegalStateException("Redis unavailable"));

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE,
                Duration.ofMinutes(10), () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
        verify(redisUtil, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void replacesMalformedCachedJson() {
        when(redisUtil.get(KEY)).thenReturn("not-json");

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE,
                Duration.ofMinutes(10), () -> List.of("都市"));

        assertEquals(List.of("都市"), result);
        verify(redisUtil).set(KEY, "[\"都市\"]", 600, TimeUnit.SECONDS);
    }

    @Test
    void returnsLoadedValueWhenRedisWriteFails() {
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(redisUtil).set(KEY, "[\"都市\"]", 600, TimeUnit.SECONDS);

        List<String> result = cache.getOrLoad("categories:all", LIST_TYPE,
                Duration.ofMinutes(10), () -> List.of("都市"));

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
}
