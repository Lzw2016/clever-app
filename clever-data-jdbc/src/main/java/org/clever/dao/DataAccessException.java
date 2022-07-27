package org.clever.dao;

import org.clever.core.NestedRuntimeException;

/**
 * 专家一对一J2EE设计和开发中讨论的数据访问异常层次结构的根源。
 * 此异常层次结构旨在让用户代码在不知道所使用的特定数据访问API（例如JDBC）的详细信息的情况下查找和处理遇到的错误类型。
 * 因此，可以在不知道正在使用JDBC的情况下对乐观锁定故障作出反应。
 * 由于这个类是一个运行时异常，如果任何错误被认为是致命的（通常情况下），则用户代码不需要捕捉它或子类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:48 <br/>
 */
public abstract class DataAccessException extends NestedRuntimeException {
    public DataAccessException(String msg) {
        super(msg);
    }

    public DataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
