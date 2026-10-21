package com.docuagent.docuagent.exception;

import com.docuagent.docuagent.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.BindException;                        // ← 改
import org.springframework.web.servlet.resource.NoResourceFoundException;   // ← 改
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常：code={}, msg={}", e.getCode(), e.getMessage());
        return new Result<>(e.getCode(), e.getMessage(), null);
    }
    /**
     * 参数校验失败 → 400
     * MethodArgumentNotValidException 继承自 BindException，所以这一个能挡两种。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = (e.getBindingResult().getFieldError() != null)
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验失败：{}", msg);
        return new Result<>(400, msg, null);
    }

    /** 缺必填参数 → 400 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数：{}", e.getParameterName());
        return new Result<>(400, "缺少必填参数：" + e.getParameterName(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return new Result<>(400, "参数类型错误：" + e.getName(), null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class})
    public Result<Void> handleInvalidRequest(Exception e) {
        return new Result<>(400, "请求JSON格式、字段类型或参数范围不正确", null);
    }

    /** 路径不存在 → 404（不是 500） */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("接口不存在：{}", e.getResourcePath());
        return new Result<>(404, "接口不存在", null);
    }

    /** 兜底 → 500。堆栈只进日志，绝不出现在响应里 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未处理异常", e);
        return new Result<>(500, "服务器内部错误", null);
    }
}
