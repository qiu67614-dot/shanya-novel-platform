package com.sdjz.novel.common.exception;

public class ErrorCode {
    public static final String SUCCESS = "00000";
    /** 登录过期 */
    public static final String TOKEN_EXPIRE = "A0230";
    /** 用户名密码错误 */
    public static final String LOGIN_ERROR = "A0001";
    /** 验证码错误 */
    public static final String CODE_ERROR = "A0002";
    /** 手机号已注册 */
    public static final String PHONE_EXIST = "A0003";
    /** 参数校验失败 */
    public static final String PARAM_ERROR = "A0400";
    /** 资源不存在 */
    public static final String RESOURCE_NOT_FOUND = "B0003";
    /** 榜单类型不合法 */
    public static final String INVALID_PARAM = "B0004";
    /** 权限不足 */
    public static final String FORBIDDEN = "A0403";
    /** 请求过于频繁（对应 HTTP 429） */
    public static final String TOO_MANY_REQUESTS = "A0429";
    /** 账号因连续登录失败被临时锁定 */
    public static final String LOGIN_LOCKED = "A0429_LOCK";
    /** 依赖的中间件暂不可用（对应 HTTP 503） */
    public static final String SERVICE_UNAVAILABLE = "A0503";

}
