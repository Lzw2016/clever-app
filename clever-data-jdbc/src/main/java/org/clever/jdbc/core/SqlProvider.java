package org.clever.jdbc.core;

/**
 * 接口由可以提供SQL字符串的对象实现。
 *
 * <p>通常由PreparedStatementCreators、CallableStatementCreators和StatementCallbacks实现，
 * 它们希望公开用于创建语句的SQL，以便在出现异常时提供更好的上下文信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:40 <br/>
 *
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see StatementCallback
 */
public interface SqlProvider {
    /**
     * 返回此对象的SQL字符串，即通常用于创建语句的SQL。
     *
     * @return SQL字符串，如果不可用，则为null
     */
    String getSql();
}
