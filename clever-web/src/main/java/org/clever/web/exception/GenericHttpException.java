package org.clever.web.exception;

import jakarta.servlet.ServletException;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 22:43 <br/>
 */
public class GenericHttpException extends ServletException {
    /**
     * 响应状态码(HTTP 状态码)
     */
    private int status = 500;

    public GenericHttpException() {
        this("Internal Server Error");
    }

    public GenericHttpException(int status) {
        this();
        this.status = status;
    }

    public GenericHttpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public GenericHttpException(String message) {
        super(message);
    }

    public GenericHttpException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public GenericHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericHttpException(int status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    public GenericHttpException(Throwable cause) {
        super(cause);
    }

    public int getStatus() {
        return status;
    }
}
