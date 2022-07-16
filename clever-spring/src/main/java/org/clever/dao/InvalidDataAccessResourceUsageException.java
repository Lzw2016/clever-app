package org.clever.dao;

/**
 * 错误使用数据访问资源时引发的异常的根。例如，在使用RDBMS时指定错误的SQL时抛出。
 * 特定于资源的子类由具体的数据访问包提供。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:07 <br/>
 */
public class InvalidDataAccessResourceUsageException extends NonTransientDataAccessException {
    public InvalidDataAccessResourceUsageException(String msg) {
        super(msg);
    }

    public InvalidDataAccessResourceUsageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
