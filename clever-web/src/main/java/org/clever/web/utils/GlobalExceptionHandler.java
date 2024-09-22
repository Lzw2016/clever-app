package org.clever.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Assert;
import org.clever.core.exception.BusinessException;
import org.clever.core.exception.NotImplementedException;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.model.response.ErrorResponse;
import org.clever.core.validator.BaseValidatorUtils;
import org.clever.web.exception.GenericHttpException;
import org.clever.web.http.HttpStatus;
import org.clever.web.http.MediaType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.*;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 全局异常处理工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 21:44 <br/>
 */
@SuppressWarnings("unchecked")
@Slf4j
public abstract class GlobalExceptionHandler {
    private static final ConcurrentMap<Class<? extends Throwable>, ExceptionHandler<? extends Throwable>> HANDLERS = new ConcurrentHashMap<>();
    private static final ExceptionHandler<Throwable> DEFAULT_HANDLER;

    static {
        DEFAULT_HANDLER = SimpleExceptionHandlerWrapper.create((exception, request, response) -> newErrorResponse(request, exception));
        // Throwable.class
        setHandle(Throwable.class, DEFAULT_HANDLER);
        // BusinessException.class
        setHandle(BusinessException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(exception.getStatus());
            res.setMessage("业务处理失败");
            return res;
        }));
        // IllegalArgumentException
        setHandle(IllegalArgumentException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            res.setMessage(exception.getMessage());
            return res;
        }));
        // NotImplementedException.class
        setHandle(NotImplementedException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
            res.setMessage("功能未实现");
            return res;
        }));
        // GenericHttpException.class
        setHandle(GenericHttpException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(exception.getStatus());
            res.setMessage(exception.getMessage());
            return res;
        }));
        // HttpMessageNotReadableException
        ExceptionHandler<?> mvcArgErrHandler = SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.BAD_REQUEST.value());
            res.setMessage("缺少必须的请求数据");
            return res;
        });
        setHandle(HttpMessageNotReadableException.class, (ExceptionHandler<HttpMessageNotReadableException>) mvcArgErrHandler);
        setHandle(MissingRequestCookieException.class, (ExceptionHandler<MissingRequestCookieException>) mvcArgErrHandler);
        setHandle(MissingRequestHeaderException.class, (ExceptionHandler<MissingRequestHeaderException>) mvcArgErrHandler);
        setHandle(MissingRequestValueException.class, (ExceptionHandler<MissingRequestValueException>) mvcArgErrHandler);
        setHandle(MissingServletRequestParameterException.class, (ExceptionHandler<MissingServletRequestParameterException>) mvcArgErrHandler);
        setHandle(MissingServletRequestPartException.class, (ExceptionHandler<MissingServletRequestPartException>) mvcArgErrHandler);
        setHandle(MultipartException.class, (ExceptionHandler<MultipartException>) mvcArgErrHandler);
        setHandle(ServletRequestBindingException.class, (ExceptionHandler<ServletRequestBindingException>) mvcArgErrHandler);
        // InvalidMediaTypeException.class
        setHandle(InvalidMediaTypeException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
            res.setMessage("不支持的Content-Type");
            return res;
        }));
        // MaxUploadSizeExceededException.class
        setHandle(MaxUploadSizeExceededException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            res.setMessage("上传数据大小超过最大限制");
            return res;
        }));
        ExceptionHandler<?> mvcConvErrHandler = SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.BAD_REQUEST.value());
            res.setMessage("请求数据类型转换错误");
            return res;
        });
        setHandle(MethodArgumentConversionNotSupportedException.class, (ExceptionHandler<MethodArgumentConversionNotSupportedException>) mvcConvErrHandler);
        setHandle(MethodArgumentTypeMismatchException.class, (ExceptionHandler<MethodArgumentTypeMismatchException>) mvcConvErrHandler);
        // ConstraintViolationException.class 请求参数校验异常
        setHandle(ConstraintViolationException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.BAD_REQUEST.value());
            res.setMessage("请求数据验证失败");
            Set<ConstraintViolation<?>> constraints = exception.getConstraintViolations();
            if (constraints != null && !constraints.isEmpty()) {
                constraints.stream().map(BaseValidatorUtils::createFieldError).forEach(res::addFieldError);
            }
            return res;
        }));
        // DuplicateKeyException.class
        setHandle(DuplicateKeyException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            res.setMessage("保存数据失败，存在重复的数据");
            return res;
        }));
        // ExcelAnalysisException 解析Excel文件异常
    }

    /**
     * 针对特定的异常设置异常处理程序
     *
     * @param exceptionClass   异常类型
     * @param exceptionHandler 异常处理程序
     */
    public static <T extends Throwable> void setHandle(Class<T> exceptionClass, ExceptionHandler<T> exceptionHandler) {
        Assert.notNull(exceptionClass, "参数 exceptionClass 不能为 null");
        Assert.notNull(exceptionHandler, "参数 exceptionHandler 不能为 null");
        HANDLERS.put(exceptionClass, exceptionHandler);
    }

    /**
     * 针对特定的异常设置异常处理程序
     *
     * @param exceptionClass   异常类型
     * @param exceptionHandler 异常处理程序
     */
    public static <T extends Throwable> void setHandle(Class<T> exceptionClass, SimpleExceptionHandler<T> exceptionHandler) {
        Assert.notNull(exceptionClass, "参数 exceptionClass 不能为 null");
        Assert.notNull(exceptionHandler, "参数 exceptionHandler 不能为 null");
        HANDLERS.put(exceptionClass, SimpleExceptionHandlerWrapper.create(exceptionHandler));
    }

    /**
     * 处理服务端异常
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void handle(Throwable exception, HttpServletRequest request, HttpServletResponse response) {
        if (exception == null || response.isCommitted()) {
            return;
        }
        // RuntimeException 找到实的异常类型
        if (exception.getClass().getName().equals(RuntimeException.class.getName())) {
            Throwable cause = exception;
            for (int i = 0; i < 64; i++) {
                cause = cause.getCause();
                if (cause == null) {
                    break;
                }
                if (!cause.getClass().getName().equals(RuntimeException.class.getName())) {
                    exception = cause;
                    break;
                }
            }
        }
        // 异常处理
        ExceptionHandler handler = HANDLERS.get(exception.getClass());
        if (handler == null) {
            handler = DEFAULT_HANDLER;
        }
        handler.handle(exception, request, response);
    }

    /**
     * 对于返回结果大部分情况下只需要设置 “message” 和 “status” 两个字段
     */
    public static ErrorResponse newErrorResponse(HttpServletRequest request, Throwable exception) {
        ErrorResponse res = new ErrorResponse();
        res.setError(exception.getMessage());
        res.setException(exception.getClass().getName());
        res.setMessage("服务器内部错误");
        res.setPath(request.getRequestURI());
        return res;
    }

    public interface SimpleExceptionHandler<T extends Throwable> {
        /***
         * 处理异常
         * @param exception 服务端异常
         * @param request   请求
         * @param response  响应
         */
        ErrorResponse handle(T exception, HttpServletRequest request, HttpServletResponse response);
    }

    public static class SimpleExceptionHandlerWrapper<T extends Throwable> implements ExceptionHandler<T> {
        public static <T extends Throwable> SimpleExceptionHandlerWrapper<T> create(SimpleExceptionHandler<T> handler) {
            return new SimpleExceptionHandlerWrapper<>(handler);
        }

        private final SimpleExceptionHandler<T> handler;

        public SimpleExceptionHandlerWrapper(SimpleExceptionHandler<T> handler) {
            this.handler = handler;
        }

        @Override
        public void handle(T exception, HttpServletRequest request, HttpServletResponse response) {
            ErrorResponse errorResponse = handler.handle(exception, request, response);
            response.setStatus(errorResponse.getStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try {
                response.getWriter().print(JacksonMapper.getInstance().toJson(errorResponse));
                response.getWriter().flush();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
