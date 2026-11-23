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

    /** 通用兜底 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public RestResp<Void> handleOther(Exception e){
        log.error("系统内部异常", e);
        return RestResp.fail("B0000","系统内部异常");
    }
}
