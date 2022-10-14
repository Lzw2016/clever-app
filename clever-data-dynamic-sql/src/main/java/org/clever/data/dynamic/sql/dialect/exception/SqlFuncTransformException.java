package org.clever.data.dynamic.sql.dialect.exception;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/21 14:08 <br/>
 */
public class SqlFuncTransformException extends RuntimeException {
    public SqlFuncTransformException() {
    }

    public SqlFuncTransformException(String message) {
        super(message);
    }

    public SqlFuncTransformException(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlFuncTransformException(Throwable cause) {
        super(cause);
    }
}
