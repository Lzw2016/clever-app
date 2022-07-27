package org.clever.web.plugin;

import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import org.clever.core.exception.BusinessException;
import org.clever.web.model.ErrorResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/27 22:11 <br/>
 */
public class ExceptionHandlerPlugin implements Plugin {
    public static final ExceptionHandlerPlugin INSTANCE = new ExceptionHandlerPlugin();

    private ExceptionHandlerPlugin() {
    }

    @Override
    public void apply(@NotNull Javalin app) {
        app.exception(Exception.class, (exception, ctx) -> {
            ErrorResponse response = newErrorResponse(exception);
            response.setPath(ctx.path());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            response.setMessage("服务器内部错误");
            ctx.status(response.getStatus()).json(response);
        });
        app.exception(BusinessException.class, (exception, ctx) -> {
            ErrorResponse response = newErrorResponse(exception);
            response.setPath(ctx.path());
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            response.setError("业务处理失败");
            ctx.status(response.getStatus()).json(response);
        });
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

    private ErrorResponse newErrorResponse(Exception exception) {
        ErrorResponse response = new ErrorResponse(
                exception.getMessage(),
                exception.getMessage(),
                exception.getClass().getName()
        );
        Throwable cause = exception;
        while (cause != null) {
            response.getDetails().put(cause.getClass().getName(), cause.getMessage());
            cause = cause.getCause();
        }
        return response;
    }
}
