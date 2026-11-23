package com.sdjz.novel.service.support;

import com.sdjz.novel.common.util.RedisUtil;
import com.sdjz.novel.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 阅读量计数器：请求只写 Redis，定时批量回写数据库。
 *
 * <p>阅读量是典型的「写多、允许延迟、丢了不致命」的数据。原来每打开一章就是一条
 * {@code UPDATE book_info SET visit_count = visit_count + 1}，热门作品会把同一行的行锁打成串行，
 * 还会产生大量 redo/binlog。现在把 N 次自增合并成一次累加写：
 *
 * <pre>
 *   请求 → INCR 内存计数（O(1)，不碰磁盘）→ 每 10s 汇总 → 一条 UPDATE ... +N
 * </pre>
 *
 * <p>计数写在 Redis 里，进程崩溃最多丢 10 秒的量；Redis 整个不可用时退化成直写数据库，
 * 功能不受影响，只是慢回去。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookVisitCounter {

    private static final String BUFFER_PREFIX = "novel:visit:buf:";
    private static final String DIRTY_SET = "novel:visit:dirty";
    /** 缓冲 key 的兜底过期时间：即便回写线程长时间没跑，也不会永久占内存 */
    private static final long BUFFER_TTL_SECONDS = 3600;

    /**
     * 自增 + 记录待回写队列，一次往返原子完成。
     *
     * <p>SADD 必须和 INCRBY 在同一个脚本里：分开执行的话，INCR 成功、SADD 失败时
     * 这次自增永远不会被回写，计数就静静地对不上了。
     */
    private static final RedisScript<Long> RECORD = new DefaultRedisScript<>(
            "local v = redis.call('INCRBY', KEYS[1], ARGV[3]) "
                    + "if v == tonumber(ARGV[3]) then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "redis.call('SADD', KEYS[2], ARGV[2]) "
                    + "return v",
            Long.class);

    /**
     * 取出并清零，同时把 id 移出待回写队列。
     *
     * <p>GET 后立刻 DEL，两步必须原子——否则两个实例同时 flush 会把同一批增量写两遍。
     */
    private static final RedisScript<Long> TAKE = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]) "
                    + "redis.call('DEL', KEYS[1]) "
                    + "redis.call('SREM', KEYS[2], ARGV[1]) "
                    + "if not v then return 0 end "
                    + "return tonumber(v)",
            Long.class);

    private final RedisUtil redisUtil;
    private final BookMapper bookMapper;

    /** 记一次阅读。Redis 不可用时直写数据库，保证功能不丢。 */
    public void record(Long bookId) {
        try {
            redisUtil.execute(RECORD, List.of(BUFFER_PREFIX + bookId, DIRTY_SET),
                    String.valueOf(BUFFER_TTL_SECONDS), String.valueOf(bookId), "1");
        } catch (RuntimeException e) {
            log.warn("Visit buffer unavailable, writing through for book {}", bookId, e);
            bookMapper.increaseVisit(bookId);
        }
    }

    /** 定时把缓冲区里的增量合并成一条 UPDATE。 */
    @Scheduled(fixedDelayString = "${reader.visit.flush-interval-ms:10000}")
    public void flush() {
        Set<String> dirty;
        try {
            dirty = redisUtil.members(DIRTY_SET);
        } catch (RuntimeException e) {
            log.warn("Visit flush skipped, Redis unavailable", e);
            return;
        }
        if (dirty.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (String rawId : dirty) {
            Long bookId = parseId(rawId);
            if (bookId == null) {
                redisUtil.removeFromSet(DIRTY_SET, rawId);
                continue;
            }
            long delta = take(bookId);
            if (delta <= 0) {
                continue;
            }
            try {
                bookMapper.increaseVisitBy(bookId, delta);
                flushed++;
            } catch (RuntimeException e) {
                returnToBuffer(bookId, delta);
                log.warn("Visit writeback failed for book {}, {} views returned to buffer", bookId, delta, e);
            }
        }
        if (flushed > 0) {
            log.debug("Flushed visit counts for {} books", flushed);
        }
    }

    private long take(Long bookId) {
        Long value = redisUtil.execute(TAKE, List.of(BUFFER_PREFIX + bookId, DIRTY_SET),
                String.valueOf(bookId));
        return value == null ? 0L : value;
    }

    /** 写库失败时把增量放回去，下个周期重试；一并补回待回写队列。 */
    private void returnToBuffer(Long bookId, long delta) {
        try {
            redisUtil.execute(RECORD, List.of(BUFFER_PREFIX + bookId, DIRTY_SET),
                    String.valueOf(BUFFER_TTL_SECONDS), String.valueOf(bookId), String.valueOf(delta));
        } catch (RuntimeException e) {
            log.warn("Visit buffer restore failed for book {}, {} views lost", bookId, delta, e);
        }
    }

    private static Long parseId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            log.warn("Dropping malformed visit buffer id: {}", raw);
            return null;
        }
    }
}
