package com.hnust.examai.common.exception;

import com.hnust.examai.common.result.R;
import com.hnust.examai.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.StringJoiner;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@Valid 注解在 RequestBody 上） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        StringJoiner joiner = new StringJoiner("; ");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            joiner.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        log.warn("参数校验失败: {}", joiner);
        return R.fail(ResultCode.PARAM_ERROR, joiner.toString());
    }

    /** 参数校验失败（@Valid 注解在表单/路径参数上） */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        StringJoiner joiner = new StringJoiner("; ");
        for (ConstraintViolation<?> cv : e.getConstraintViolations()) {
            joiner.add(cv.getPropertyPath() + ": " + cv.getMessage());
        }
        return R.fail(ResultCode.PARAM_ERROR, joiner.toString());
    }

    /** 绑定异常（表单提交） */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        StringJoiner joiner = new StringJoiner("; ");
        for (FieldError fieldError : e.getFieldErrors()) {
            joiner.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        return R.fail(ResultCode.PARAM_ERROR, joiner.toString());
    }

    /** Spring Security 认证异常（未登录） */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthenticationException(AuthenticationException e) {
        return R.fail(ResultCode.UNAUTHORIZED, e.getMessage());
    }

    /** Spring Security 权限异常（无权限） */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN);
    }

    /** 文件超出大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return R.fail(ResultCode.FILE_SIZE_EXCEEDED);
    }

    /** 兜底：其他未知异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return R.fail(ResultCode.INTERNAL_ERROR, "服务器内部错误，请联系管理员");
    }
}
