package org.clever.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.exception.BusinessException;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.model.response.ErrorResponse;
import org.clever.util.Assert;
import org.clever.web.exception.GenericHttpException;
import org.clever.web.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 全局异常处理工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 21:44 <br/>
 */
@Slf4j
public abstract class GlobalExceptionHandler {
    private static final ConcurrentMap<Class<? extends Throwable>, ExceptionHandler<? extends Throwable>> HANDLERS = new ConcurrentHashMap<>();
    private static final ExceptionHandler<Throwable> DEFAULT_HANDLER;

    static {
        DEFAULT_HANDLER = SimpleExceptionHandlerWrapper.create((exception, request, response) -> newErrorResponse(request, exception));
        // Throwable.class
        setHandle(Throwable.class, DEFAULT_HANDLER);
        // GenericHttpException.class
        setHandle(GenericHttpException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(exception.getStatus());
            res.setMessage(exception.getMessage());
            return res;
        }));
        // BusinessException.class
        setHandle(BusinessException.class, SimpleExceptionHandlerWrapper.create((exception, request, response) -> {
            ErrorResponse res = newErrorResponse(request, exception);
            res.setStatus(exception.getStatus());
            res.setMessage("业务处理失败");
            return res;
        }));
        // ValidationException 请求参数校验异常
        // HttpMessageConversionException 请求参数转换异常
        // BindException 请求参数校验失败
        // MethodArgumentNotValidException 请求参数校验失败
        // ConstraintViolationException 请求参数校验失败
        // MaxUploadSizeExceededException 上传文件大小超限
        // ExcelAnalysisException 解析Excel文件异常
        // ExcelAnalysisException 上传文件大小超限
        // DuplicateKeyException 保存数据失败，数据已经存在
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
        if (response.isCommitted()) {
            return;
        }
        // RuntimeException 找到实的异常类型
        if (exception instanceof RuntimeException) {
            Throwable cause = exception;
            for (int i = 0; i < 64; i++) {
                cause = cause.getCause();
                if (!(cause instanceof RuntimeException)) {
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
    private static ErrorResponse newErrorResponse(HttpServletRequest request, Throwable exception) {
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
         * @param request 请求
         * @param response 响应
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
