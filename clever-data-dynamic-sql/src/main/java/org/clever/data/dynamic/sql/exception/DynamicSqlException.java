package org.clever.data.dynamic.sql.exception;

public class DynamicSqlException extends RuntimeException {
    public DynamicSqlException() {
        super();
    }

    public DynamicSqlException(String message) {
        super(message);
    }

    public DynamicSqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicSqlException(Throwable cause) {
        super(cause);
    }
}
