package com.sdjz.novel.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdjz.novel.common.cache.CacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 读者端 JSON 缓存。
 *
 * <p>三件事共同保证「缓存失效瞬间不把数据库打穿」：
 * <ul>
 *   <li><b>单飞（single-flight）</b>：同一个 key 只放一个请求回源，其余请求等它算完复用结果；</li>
 *   <li><b>空值缓存</b>：查不到的 id 也写一个短 TTL 的哨兵，挡住对不存在数据的反复穿透；</li>
 *   <li><b>TTL 抖动</b>：过期时间上下浮动 10%，避免同一批 key 在同一秒集体失效。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonRedisCache {

    /** 空值哨兵：与「没查到缓存」区分开，前者可以直接返回 null，后者必须回源 */
    private static final String NULL_SENTINEL = "null";
    /** 空值只缓存很短时间，既能挡住穿透，又不会让新数据等太久 */
    private static final Duration NULL_TTL = Duration.ofSeconds(30);

    /** 锁的存活时间，必须大于一次回源的最长耗时，否则锁提前过期等于没加 */
    private static final long LOCK_TTL_SECONDS = 3;
    /** 抢不到锁时最多等 10 轮 × 50ms = 500ms，超时就自己回源（降级，不把请求拖死） */
    private static final int WAIT_ROUNDS = 10;
    private static final long WAIT_STEP_MILLIS = 50;
    /** 同 JVM 内等「领头线程」的上限，比跨实例等待略长 */
    private static final long LOCAL_WAIT_MILLIS = 800;
    /** TTL 抖动幅度：±10% */
    private static final double JITTER_RATIO = 0.1;

    private enum LockState {ACQUIRED, BUSY, UNAVAILABLE}

    private record Lookup<T>(boolean hit, T value) {
        static <T> Lookup<T> miss() {
            return new Lookup<>(false, null);
        }
    }

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /** 同 JVM 内的单飞表：key → 正在回源的那个线程的结果句柄 */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlight = new ConcurrentHashMap<>();

    @Value("${reader.cache.enabled:true}")
    private boolean enabled = true;

    public void evict(String key) {
        if (!enabled) {
            return;
        }
        try {
            redisUtil.delete(CacheKeys.PREFIX + key);
        } catch (RuntimeException e) {
            log.warn("Reader cache eviction failed for {}", key, e);
        }
    }

    /**
     * 读缓存，未命中则回源并回填。
     *
     * <p>加载函数返回 {@code null} 时不会抛异常，而是写入空值哨兵；
     * 调用方如果需要「不存在」语义，自行判断返回值。
     */
    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }

        String redisKey = CacheKeys.PREFIX + key;
        Lookup<T> cached = peek(redisKey, type);
        if (cached.hit()) {
            return cached.value();
        }
        return loadSingleFlight(key, redisKey, type, ttl, loader);
    }

    /**
     * 同一 key 只允许一个线程真正回源。
     *
     * <p>先做进程内合并（零网络开销），再由 Redis 锁做跨实例互斥。
     * 两层缺一不可：只有本地锁，多实例部署时每个实例仍会打一次库；
     * 只有 Redis 锁，每个请求都要多一次网络往返。
     */
    private <T> T loadSingleFlight(String key, String redisKey, TypeReference<T> type,
                                   Duration ttl, Supplier<T> loader) {
        CompletableFuture<Object> mine = new CompletableFuture<>();
        CompletableFuture<Object> running = inFlight.putIfAbsent(redisKey, mine);

        if (running != null) {
            return awaitLeader(running, redisKey, loader);
        }

        try {
            T value = loadAcrossInstances(key, redisKey, type, ttl, loader);
            mine.complete(value);
            return value;
        } catch (RuntimeException | Error e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(redisKey, mine);
        }
    }

    /** 等待领头线程的结果；等不到或它失败了，就自己回源一次（宁可多查，不可挂死）。 */
    @SuppressWarnings("unchecked")
    private <T> T awaitLeader(CompletableFuture<Object> running, String redisKey, Supplier<T> fallback) {
        try {
            return (T) running.get(LOCAL_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback.get();
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Single-flight fallback for {}: {}", redisKey, e.toString());
            return fallback.get();
        }
    }

    private <T> T loadAcrossInstances(String key, String redisKey, TypeReference<T> type,
                                      Duration ttl, Supplier<T> loader) {
        String lockKey = CacheKeys.LOCK_PREFIX + key;
        String token = UUID.randomUUID().toString();

        LockState state = acquire(lockKey, token);
        if (state == LockState.UNAVAILABLE) {
            // Redis 挂了就别谈单飞了，直接回源并尽力回填
            return loadAndCache(redisKey, ttl, loader);
        }
        if (state == LockState.BUSY) {
            Lookup<T> filled = waitForFill(redisKey, type);
            return filled.hit() ? filled.value() : loadAndCache(redisKey, ttl, loader);
        }

        try {
            // 抢到锁后再查一次：排队期间上一个持锁者可能已经把缓存填好了
            Lookup<T> again = peek(redisKey, type);
            if (again.hit()) {
                return again.value();
            }
            return loadAndCache(redisKey, ttl, loader);
        } finally {
            release(lockKey, token);
        }
    }

    private LockState acquire(String lockKey, String token) {
        try {
            return Boolean.TRUE.equals(redisUtil.setIfAbsent(lockKey, token, LOCK_TTL_SECONDS, TimeUnit.SECONDS))
                    ? LockState.ACQUIRED : LockState.BUSY;
        } catch (RuntimeException e) {
            log.warn("Single-flight lock unavailable for {}", lockKey, e);
            return LockState.UNAVAILABLE;
        }
    }

    private void release(String lockKey, String token) {
        try {
            redisUtil.deleteIfMatch(lockKey, token);
        } catch (RuntimeException e) {
            // 释放失败不影响结果，锁会随 TTL 自动消失
            log.warn("Single-flight unlock failed for {}", lockKey, e);
        }
    }

    private <T> Lookup<T> waitForFill(String redisKey, TypeReference<T> type) {
        for (int i = 0; i < WAIT_ROUNDS; i++) {
            try {
                Thread.sleep(WAIT_STEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Lookup.miss();
            }
            Lookup<T> lookup = peek(redisKey, type);
            if (lookup.hit()) {
                return lookup;
            }
        }
        return Lookup.miss();
    }

    private <T> T loadAndCache(String redisKey, Duration ttl, Supplier<T> loader) {
        T value = loader.get();
        write(redisKey, value, ttl);
        return value;
    }

    private <T> void write(String redisKey, T value, Duration ttl) {
        Duration effective = value == null ? min(ttl, NULL_TTL) : ttl;
        try {
            String json = value == null ? NULL_SENTINEL : objectMapper.writeValueAsString(value);
            redisUtil.set(redisKey, json, jitterMillis(effective), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Reader cache write failed for {}", redisKey, e);
        }
    }

    private <T> Lookup<T> peek(String redisKey, TypeReference<T> type) {
        String raw;
        try {
            raw = redisUtil.get(redisKey);
        } catch (RuntimeException e) {
            log.warn("Reader cache read failed for {}", redisKey, e);
            return Lookup.miss();
        }
        if (raw == null) {
            return Lookup.miss();
        }
        if (NULL_SENTINEL.equals(raw)) {
            // 命中了空值哨兵：确实是「没有这条数据」，不需要回源
            return new Lookup<>(true, null);
        }
        try {
            T value = objectMapper.readValue(raw, type);
            return value == null ? new Lookup<>(true, null) : new Lookup<>(true, value);
        } catch (JsonProcessingException e) {
            log.warn("Reader cache value is invalid for {}", redisKey, e);
            return Lookup.miss();
        }
    }

    /** 过期时间加 ±10% 抖动，避免一批 key 同时失效造成雪崩 */
    private long jitterMillis(Duration ttl) {
        long base = Math.max(ttl.toMillis(), 1000L);
        long delta = Math.max((long) (base * JITTER_RATIO), 1L);
        return base - delta + ThreadLocalRandom.current().nextLong(delta * 2 + 1);
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
