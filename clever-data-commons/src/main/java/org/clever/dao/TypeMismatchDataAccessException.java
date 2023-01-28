package org.clever.dao;

/**
 * Java类型和数据库类型不匹配时引发异常：例如，试图在RDBMS列中设置错误类型的对象时。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:07 <br/>
 */
public class TypeMismatchDataAccessException extends InvalidDataAccessResourceUsageException {
    public TypeMismatchDataAccessException(String msg) {
        super(msg);
    }

    public TypeMismatchDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
