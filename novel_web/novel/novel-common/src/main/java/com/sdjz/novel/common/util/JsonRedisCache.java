package com.sdjz.novel.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonRedisCache {

    private static final String KEY_PREFIX = "novel:reader:v1:";

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Value("${reader.cache.enabled:true}")
    private boolean enabled = true;

    public void evict(String key) {
        if (!enabled) return;
        try {
            redisUtil.delete(KEY_PREFIX + key);
        } catch (RuntimeException e) {
            log.warn("Reader cache eviction failed for {}", key, e);
        }
    }

    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }

        String redisKey = KEY_PREFIX + key;
        String cached;
        try {
            cached = redisUtil.get(redisKey);
        } catch (RuntimeException e) {
            log.warn("Reader cache read failed for {}", redisKey, e);
            return loader.get();
        }

        if (cached != null) {
            try {
                return objectMapper.readValue(cached, type);
            } catch (JsonProcessingException e) {
                log.warn("Reader cache value is invalid for {}", redisKey, e);
            }
        }

        T value = loader.get();
        if (value != null) {
            try {
                redisUtil.set(redisKey, objectMapper.writeValueAsString(value), ttl.toSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Reader cache write failed for {}", redisKey, e);
            }
        }
        return value;
    }
}
