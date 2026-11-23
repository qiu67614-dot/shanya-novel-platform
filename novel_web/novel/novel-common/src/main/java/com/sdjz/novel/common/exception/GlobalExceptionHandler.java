package com.sdjz.novel.common.exception;

import com.sdjz.novel.common.response.RestResp;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
