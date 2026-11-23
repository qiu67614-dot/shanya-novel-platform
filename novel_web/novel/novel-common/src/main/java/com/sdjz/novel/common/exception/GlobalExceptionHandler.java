package com.sdjz.novel.common.exception;

import com.sdjz.novel.common.response.RestResp;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 限流拒绝：返回真正的 HTTP 429 + Retry-After。
     *
     * <p>刻意不复用 {@link BusinessException} 那条路径（HTTP 200 + body 里的 code）：
     * 那样 axios 拿到的 status 永远是 200，前端无法进入 429 分支，也就读不到
     * Retry-After，只能提示「网络异常」并继续狂点。
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<RestResp<Void>> handleRateLimit(RateLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(RestResp.fail(ErrorCode.TOO_MANY_REQUESTS, e.getMessage()));
    }

    /**
     * 中间件不可用：HTTP 503。
     *
     * <p>兜底处理器把一切异常都转成 200 + B0000，这对业务异常没问题，
     * 但「Redis 连不上」也走这条路的话，监控看到的成功率仍是 100%，
     * 用户得到的提示却是「系统内部异常」，定位成本极高。503 才能被网关和监控识别。
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<RestResp<Void>> handleUnavailable(ServiceUnavailableException e) {
        log.error("依赖服务不可用: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(RestResp.fail(ErrorCode.SERVICE_UNAVAILABLE, e.getMessage()));
    }

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public RestResp<Void> handleBusiness(BusinessException e){
        return RestResp.fail(e.getCode(),e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler({BindException.class, ConstraintViolationException.class,
            MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.OK)
    public RestResp<Void> handleValid(Exception e){
        return RestResp.fail(ErrorCode.PARAM_ERROR,"参数校验失败");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public RestResp<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        return RestResp.fail(ErrorCode.PARAM_ERROR, "文件不能超过 20 MB");
    }

    /**
     * 路径不存在：返回真正的 404。
     *
     * <p>兜底处理器把一切异常都压成 200 + B0000，连「URL 写错了」都返回 200，
     * 监控里看到的是「请求成功、业务码异常」，排查时几乎不会想到是自己路径拼错。
     * 这个坑是真实踩到的：本地用 {@code /api/front/books/home} 冒烟测了半天，
     * 其实应用没有 /api 前缀（由 Nginx 剥离），一直在访问不存在的路由。
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<RestResp<Void>> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResp.fail(ErrorCode.RESOURCE_NOT_FOUND, "接口不存在"));
    }

    /** 通用兜底 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public RestResp<Void> handleOther(Exception e){
        log.error("系统内部异常", e);
        return RestResp.fail("B0000","系统内部异常");
    }
}
