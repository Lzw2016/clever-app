package org.clever.jdbc.core;

import org.clever.jdbc.support.JdbcUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 要实现的接口，用于为标准{@code setObject}方法不支持的更复杂的数据库特定类型设置值。
 * 这实际上是{@link org.clever.jdbc.support.SqlValue}
 *
 * <p>实现执行设置实际值的实际工作。
 * 他们必须实现回调方法{@code setTypeValue}，该方法可以引发SQLException，调用代码将捕获并翻译这些异常。
 * 如果需要创建任何特定于数据库的对象，则此回调方法可以通过给定的PreparedStatement对象访问底层连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:47 <br/>
 *
 * @see java.sql.Types
 * @see PreparedStatement#setObject
 * @see JdbcOperations#update(String, Object[], int[])
 * @see org.clever.jdbc.support.SqlValue
 */
public interface SqlTypeValue {
    /**
     * 指示未知（或未指定）SQL类型的常量。如果原始操作方法未指定SQL类型，则传递到{@code setTypeValue}。
     *
     * @see java.sql.Types
     * @see JdbcOperations#update(String, Object[])
     */
    int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;

    /**
     * 在给定的PreparedStatement上设置类型值。
     *
     * @param ps         准备好的工作报表
     * @param paramIndex 需要为其设置值的参数的索引
     * @param sqlType    我们正在设置的参数的SQL类型
     * @param typeName   参数的类型名称（可选）
     * @throws SQLException 如果在设置参数值时遇到SQLException
     * @see java.sql.Types
     * @see PreparedStatement#setObject
     */
    void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException;
}
