package org.clever.web.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务端错误时的响应结构
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/27 22:26 <br/>
 */
@Data
public class ErrorResponse implements Serializable {
    /**
     * 时间戳
     */
    private Date timestamp = new Date();
    /**
     * 响应状态码(HTTP 状态码)
     */
    private int status = 500;
    /**
     * 请求路径，当前请求的路径
     */
    private String path;
    /**
     * 错误消息，用于前端显示
     */
    private String message;
    /**
     * 异常消息(exception.message)
     */
    private String error;
    /**
     * 异常类型，异常的具体类型
     */
    private String exception;
    /**
     * 异常栈
     */
    private final Map<String, Object> details = new LinkedHashMap<>(4);

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String path) {
        this.status = status;
        this.path = path;
    }

    public ErrorResponse(int status, String path, String message) {
        this.status = status;
        this.path = path;
        this.message = message;
    }

    public ErrorResponse(int status, String path, String message, String error) {
        this.status = status;
        this.path = path;
        this.message = message;
        this.error = error;
    }

    public ErrorResponse(int status, String path, String message, String error, String exception) {
        this.status = status;
        this.path = path;
        this.message = message;
        this.error = error;
        this.exception = exception;
    }

    public ErrorResponse(String message, String error, String exception) {
        this.message = message;
        this.error = error;
        this.exception = exception;
    }
}
