package com.sdjz.novel.common.cache;

import com.sdjz.novel.common.util.JsonRedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存失效的统一入口，解决两个经典时序问题。
 *
 * <p><b>问题一：删早了。</b>在事务里直接删缓存，事务还没提交，别的请求已经把旧数据读出来又写回缓存了，
 * 结果是「删了个寂寞」。所以必须挂到 {@code afterCommit} 上，提交之后才删。
 *
 * <p><b>问题二：删完又被写回。</b>提交瞬间仍在执行的读请求，可能带着提交前的旧数据回填缓存。
 * 延迟 500ms 再删一次，把这段窗口内被写回的旧值清掉。这叫「延迟双删」。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheEvictor {

    /** 二次删除的延迟，需大于一次「查库 + 回填」的耗时 */
    private static final long DELAY_MILLIS = 500;

    private final JsonRedisCache cache;

    private final ScheduledExecutorService delayed =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cache-evict");
                t.setDaemon(true);
                return t;
            });

    /**
     * 事务提交后删除缓存，并在 500ms 后补删一次。
     *
     * <p>不在事务里调用（比如纯查询后的手动刷新）时会立即删除，不会丢事件。
     */
    public void evictAfterCommit(String... keys) {
        if (keys.length == 0) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictThenDelay(keys);
                }
            });
        } else {
            evictThenDelay(keys);
        }
    }

    private void evictThenDelay(String[] keys) {
        evictAll(keys);
        delayed.schedule(() -> evictAll(keys), DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void evictAll(String[] keys) {
        for (String key : keys) {
            cache.evict(key);
        }
        log.debug("Evicted reader cache keys: {}", String.join(",", keys));
    }
}
