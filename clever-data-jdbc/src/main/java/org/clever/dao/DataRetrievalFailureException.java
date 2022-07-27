package org.clever.dao;

/**
 * 如果无法检索某些预期数据，例如通过已知标识符查找特定数据时，引发异常。
 * 此异常将由或O/R映射工具或DAO实现引发。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:08 <br/>
 */
public class DataRetrievalFailureException extends NonTransientDataAccessException {
    public DataRetrievalFailureException(String msg) {
        super(msg);
    }

    public DataRetrievalFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
