package com.sdjz.novel.common.ratelimit;

/**
 * 接口分组常量。
 *
 * <p>用常量而不是裸字符串，避免注解里拼错导致配置读不到、限流静默失效——
 * 那种 bug 不会报错，只会「看起来加了限流但没生效」。
 *
 * <p>分组依据是<b>接口代价</b>而不是 URL 前缀：读一次缓存里的榜单和解析一个 20 MB
 * 的 TXT，成本差几百倍，不能共用阈值。具体阈值在 {@code application.yml} 的
 * {@code rate-limit.groups} 下配置，改阈值不需要改代码。
 */
public final class RateLimitGroup {

    private RateLimitGroup() {
    }

    /** 公开读接口：首页、榜单、分类、目录。 */
    public static final String READ = "read";

    /** 章节正文：单请求响应体最大，单独一档便于观测缓存命中与限流比例。 */
    public static final String CHAPTER = "chapter";

    /** 搜索：条件查询 + count，代价高于普通读。 */
    public static final String SEARCH = "search";

    /** 认证：登录、注册、验证码。BCrypt 慢哈希是 CPU 放大器，必须严格。 */
    public static final String AUTH = "auth";

    /** 一般写操作：评论、书架、历史、关注、阅读进度。 */
    public static final String WRITE = "write";

    /** 重型操作：TXT 导入、投稿、审核。 */
    public static final String HEAVY = "heavy";

    /** 管理端接口。 */
    public static final String ADMIN = "admin";

    /** 全局闸门（RateLimitFilter 使用），不用于接口注解。 */
    public static final String GATE = "gate";
}
