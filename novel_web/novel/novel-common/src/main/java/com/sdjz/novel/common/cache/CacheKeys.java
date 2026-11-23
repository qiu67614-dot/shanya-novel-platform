package com.sdjz.novel.common.cache;

import java.util.List;

/**
 * 读者端缓存 key 的唯一来源。
 *
 * <p>以前 key 是散落在各个 Service 里的字符串字面量，写缓存用 {@code "ranks:visit"}、
 * 清缓存写成 {@code "rank:visit"}，编译能过、运行不报错，只是缓存永远不失效——
 * 这类 bug 在线上极难发现。集中到一处之后，拼错就是编译错误。
 */
public final class CacheKeys {

    /** 读者端缓存统一前缀，带版本号，便于整体升级数据格式时一次性失效 */
    public static final String PREFIX = "novel:reader:v1:";

    /** 单飞锁前缀，与数据 key 分开，避免误删 */
    public static final String LOCK_PREFIX = "novel:lock:reader:v1:";

    public static final String HOME = "home";
    public static final String CATEGORIES_ALL = "categories:all";

    private static final String RANKS_PREFIX = "ranks:";
    private static final String CATEGORIES_PREFIX = "categories:";
    private static final String CHAPTER_PREFIX = "chapter:";

    /** 首页与三个榜单，作品上/下架后需要一起失效 */
    public static final List<String> PUBLIC_BOOK_KEYS = List.of(
            HOME,
            ranks("visit"),
            ranks("update"),
            ranks("new"));

    private CacheKeys() {
    }

    public static String ranks(String type) {
        return RANKS_PREFIX + type;
    }

    public static String categories(Integer direction) {
        return direction == null ? CATEGORIES_ALL : CATEGORIES_PREFIX + direction;
    }

    public static String chapter(Long chapterId) {
        return CHAPTER_PREFIX + chapterId;
    }
}
