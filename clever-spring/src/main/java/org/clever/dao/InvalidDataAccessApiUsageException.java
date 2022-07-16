package org.clever.dao;

/**
 * API使用不当引发异常，例如未能“编译”执行前需要编译的查询对象。
 *
 * <p>这代表了我们的Java数据访问框架中的一个问题，而不是底层数据访问基础架构中的问题。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:13 <br/>
 */
public class InvalidDataAccessApiUsageException extends NonTransientDataAccessException {
    public InvalidDataAccessApiUsageException(String msg) {
        super(msg);
    }

    public InvalidDataAccessApiUsageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
