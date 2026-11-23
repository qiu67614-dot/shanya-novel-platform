package com.sdjz.novel.common.ratelimit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流配置。阈值全部来自配置，改阈值只改 yml，不需要重新打包。
 *
 * <pre>
 * rate-limit:
 *   enabled: true
 *   gate:                       # RateLimitFilter 的全局 IP 粗筛
 *     enabled: true
 *     permits: 900
 *     window: 60
 *   groups:
 *     auth:
 *       permits: 20
 *       window: 60
 *       fallback: OPEN
 * </pre>
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /** 总开关。压测对比、灰度排障时一行配置就能全部关掉。 */
    private boolean enabled = true;

    /** 全局闸门（Filter 层，按 IP 粗筛）。 */
    private Gate gate = new Gate();

    /** 按分组配置的规则，key 见 {@link RateLimitGroup}。 */
    private Map<String, Rule> groups = new HashMap<>();

    /**
     * 取分组规则；缺失时返回一个保守的默认值并告警，避免「配置漏写」变成「完全不限流」。
     */
    public Rule ruleOf(String group) {
        Rule rule = groups.get(group);
        if (rule == null) {
            log.warn("限流分组 [{}] 未配置，使用默认 60 次/分钟（OPEN 降级）", group);
            Rule fallback = new Rule();
            fallback.setPermits(60);
            fallback.setWindow(60);
            fallback.setFallback(RateLimitFallback.OPEN);
            return fallback;
        }
        return rule;
    }

    @Data
    public static class Rule {
        /** 窗口内允许的次数（有身份时，即按 uid 计数）。 */
        private int permits = 60;

        /**
         * 游客/按 IP 计数时的额度。不配置（<=0）则与 {@link #permits} 相同。
         *
         * <p>必须能分开配：读接口按单人 60 次/分是合理的，但游客会降级到 IP 桶，
         * 而一个校园网出口后面可能有几十人——同一个数字用在两种口径上，
         * 要么放得太松，要么把整栋楼卡死。
         */
        private int guestPermits = -1;

        /** 窗口长度，单位秒。令牌桶的补充速率 = permits / window。 */
        private int window = 60;

        /** Redis 不可用时的策略。 */
        private RateLimitFallback fallback = RateLimitFallback.OPEN;

        public int effectiveGuestPermits() {
            return guestPermits > 0 ? guestPermits : permits;
        }
    }

    @Data
    public static class Gate {
        private boolean enabled = true;
        private int permits = 900;
        private int window = 60;
    }
}
