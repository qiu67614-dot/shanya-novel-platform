package com.sdjz.novel.common.context;

public final class AdminContext {
    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    private AdminContext() {}

    public static void setAdminId(Long id) { ADMIN_ID.set(id); }
    public static Long getAdminId() { return ADMIN_ID.get(); }
    public static void clear() { ADMIN_ID.remove(); }
}
