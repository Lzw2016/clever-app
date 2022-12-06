package org.clever.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * 当需要根据连接自定义参数时实现该接口。<br/>
 * 我们可能需要这样做才能使用专有功能，这些功能仅适用于特定的连接类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:03 <br/>
 *
 * @see CallableStatementCreatorFactory#newCallableStatementCreator(ParameterMapper)
 */
@FunctionalInterface
public interface ParameterMapper {
    /**
     * 创建输入参数的映射，按名称键入
     *
     * @param con JDBC 连接。如果我们需要使用专有的 Connection 实现类执行特定于 RDBMS 的操作，这将很有用（也是此接口的目的）。此类隐藏了此类专有细节。但是，最好尽可能避免使用此类专有 RDBMS 功能
     * @return a Map of input parameters, keyed by name (never {@code null})
     * @throws SQLException 如果在设置参数值时遇到 SQLException（即不需要捕获 SQLException）
     */
    Map<String, ?> createMap(Connection con) throws SQLException;
}
