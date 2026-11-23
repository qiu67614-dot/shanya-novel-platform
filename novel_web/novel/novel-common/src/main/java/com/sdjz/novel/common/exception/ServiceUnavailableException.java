package com.sdjz.novel.common.exception;

/**
 * 依赖的中间件（Redis、对象存储等）不可用。
 *
 * <p>与 {@link BusinessException} 的区别在于 HTTP 状态码：业务异常返回 200 + body 里的 code，
 * 这是「请求本身有问题」；而这里是「服务暂时不具备处理条件」，必须让调用方（网关、前端、
 * 监控）看到 <b>HTTP 503</b>，否则会被当成正常响应统计，故障被藏在成功率里。
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
