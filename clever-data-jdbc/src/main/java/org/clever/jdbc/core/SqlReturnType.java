package org.clever.jdbc.core;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * 要实现的接口，用于检索标准{@code CallableStatement.getObject}方法不支持的更复杂数据库特定类型的值。
 *
 * <p>实现执行获取实际值的实际工作。
 * 他们必须实现回调方法{@code getTypeValue}，该方法可以抛出SQLException，调用代码将捕获并翻译这些异常。
 * 如果需要创建任何特定于数据库的对象，则此回调方法可以通过给定的CallableStatement对象访问底层连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:04 <br/>
 *
 * @see java.sql.Types
 * @see CallableStatement#getObject
 */
public interface SqlReturnType {
    /**
     * 指示未知（或未指定）SQL类型的常量。
     * 如果原始操作方法未指定SQL类型，则传递到setTypeValue。
     *
     * @see java.sql.Types
     * @see JdbcOperations#update(String, Object[])
     */
    int TYPE_UNKNOWN = Integer.MIN_VALUE;

    /**
     * 从特定对象获取类型值。
     *
     * @param cs         要操作的CallableStatement
     * @param paramIndex 需要为其设置值的参数的索引
     * @param sqlType    我们正在设置的参数的SQL类型
     * @param typeName   参数的类型名称（可选）
     * @return 目标值
     * @throws SQLException 如果在设置参数值时遇到SQLException（即，不需要捕捉SQLException）
     * @see java.sql.Types
     * @see CallableStatement#getObject
     */
    Object getTypeValue(CallableStatement cs, int paramIndex, int sqlType, String typeName) throws SQLException;
}
