package com.sdjz.novel.common.context;

/**
 * 当前登录用户上下文，ThreadLocal 线程隔离
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /** 设置当前登录用户id */
    public static void setUid(Long uid) {
        USER_ID_HOLDER.set(uid);
    }

    /** 获取当前登录用户id；未登录返回null */
    public static Long getUid() {
        return USER_ID_HOLDER.get();
    }

    /** 清除上下文，必须执行，防止内存泄漏 */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}

