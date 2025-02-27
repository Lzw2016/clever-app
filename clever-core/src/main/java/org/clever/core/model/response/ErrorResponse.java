package org.clever.core.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.exception.BusinessException;
import org.clever.core.validator.FieldError;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 系统异常时返回的数据
 * <p>
 * 作者：lzw <br/>
 * 创建时间：2017-09-03 13:31 <br/>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
@Data
public class ErrorResponse extends BaseResponse {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时间戳
     */
    private final Date timestamp = new Date();
    /**
     * 异常消息(exception.message)
     */
    private String error;
    /**
     * 响应状态码(HTTP 状态码)
     */
    private int status;
    /**
     * 异常类型，异常的具体类型
     */
    private String exception;
    /**
     * 错误消息，用于前端显示
     */
    private String message;
    /**
     * 请求路径，当前请求的路径
     */
    private String path;
    /**
     * 请求数据验证的错误消息
     */
    private List<FieldError> fieldErrors;

    public ErrorResponse() {
        this(null, null, 500, null);
    }

    /**
     * @param message 错误消息，用于前端显示
     */
    public ErrorResponse(String message) {
        this(message, null, 500, null);
    }

    /**
     * @param message 错误消息，用于前端显示
     * @param status  响应状态码
     */
    public ErrorResponse(String message, int status) {
        this(message, null, status, null);
    }

    /**
     * @param message   错误消息，用于前端显示
     * @param exception 异常
     */
    public ErrorResponse(String message, Throwable exception) {
        this(message, exception, (exception instanceof BusinessException) ? 400 : 500, null);
    }

    /**
     * @param message   错误消息，用于前端显示
     * @param exception 异常
     * @param path      请求路径
     */
    public ErrorResponse(String message, Throwable exception, String path) {
        this(message, exception, (exception instanceof BusinessException) ? 400 : 500, path);
    }

    /**
     * @param message   错误消息，用于前端显示
     * @param exception 异常
     * @param status    响应状态码(HTTP 状态码)
     */
    public ErrorResponse(String message, Throwable exception, int status) {
        this(message, exception, status, null);
    }

    /**
     * @param message   错误消息，用于前端显示
     * @param status    响应状态码(HTTP 状态码)
     * @param exception 异常
     * @param path      请求路径
     */
    public ErrorResponse(String message, Throwable exception, int status, String path) {
        this.message = message;
        this.status = status;
        if (exception != null) {
            this.exception = exception.getClass().getName();
            this.error = exception.getMessage();
        } else {
            this.error = message;
        }
        this.path = path;
    }

    /**
     * 增加验证错误消息<br/>
     */
    public ErrorResponse addFieldError(FieldError fieldError) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new ArrayList<>();
        }
        this.fieldErrors.add(fieldError);
        return this;
    }
}
