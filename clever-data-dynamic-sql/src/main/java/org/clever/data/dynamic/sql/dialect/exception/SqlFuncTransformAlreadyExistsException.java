package org.clever.data.dynamic.sql.dialect.exception;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/12 19:56 <br/>
 */
public class SqlFuncTransformAlreadyExistsException extends RuntimeException {

    public SqlFuncTransformAlreadyExistsException() {
        super();
    }

    public SqlFuncTransformAlreadyExistsException(String message) {
        super(message);
    }

    public SqlFuncTransformAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlFuncTransformAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
