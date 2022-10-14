package org.clever.data.dynamic.sql.dialect.exception;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/21 12:25 <br/>
 */
public class ParseSqlFuncException extends RuntimeException {
    public ParseSqlFuncException() {
    }

    public ParseSqlFuncException(String message) {
        super(message);
    }

    public ParseSqlFuncException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseSqlFuncException(Throwable cause) {
        super(cause);
    }
}
