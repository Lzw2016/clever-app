package org.clever.dao;

/**
 * 当我们不能区分任何比“something went wrong with the underlying resource”更具体的东西时，
 * 使用普通的超类：例如，我们无法更精确地确定来自JDBC的SQLException。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:12 <br/>
 */
public abstract class UncategorizedDataAccessException extends NonTransientDataAccessException {
    public UncategorizedDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
