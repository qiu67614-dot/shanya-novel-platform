package com.sdjz.novel.common.util;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;

    /**
     * INCR 与 EXPIRE 必须原子执行。
     *
     * <p>拆成两次 Java 调用的话，一旦进程在 INCR 之后、EXPIRE 之前崩溃或重启，
     * 这个 key 就<b>永不过期</b>——用户会被永久锁定，而且日志里看不出任何异常。
     * 只在 v == 1（首次创建）时设过期，后续自增不重置 TTL。
     */
    private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local v = redis.call('INCRBY', KEYS[1], ARGV[2]) "
                    + "if v == tonumber(ARGV[2]) then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return v",
            Long.class);

    public void set(String key,String value,long timeout, TimeUnit unit){
        redisTemplate.opsForValue().set(key,value,timeout,unit);
    }
    public String get(String key){
        return redisTemplate.opsForValue().get(key);
    }
    public void delete(String key){
        redisTemplate.delete(key);
    }

    /**
     * SET NX EX 的原子封装。用于幂等键、单飞锁、登录失败计数初始化。
     *
     * @return true 表示抢到（key 原本不存在）
     */
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /** 自增并返回新值。注意本方法<b>不设过期</b>，只用于已有 TTL 的 key。 */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** 剩余存活秒数；key 不存在或未设过期时返回 -1 / -2，这里统一收敛为非负。 */
    public long getExpireSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }

    /** 自增并按需设置过期时间，原子执行。返回自增后的值。 */
    public long incrementWithTtl(String key, long delta, long ttlSeconds) {
        Long result = redisTemplate.execute(
                INCR_WITH_TTL, List.of(key), String.valueOf(ttlSeconds), String.valueOf(delta));
        return result == null ? 0L : result;
    }

    /** 执行 Lua 脚本。限流令牌桶依赖它保证「读→算→写」整段原子。 */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }
}

