package org.clever.data.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface TypeHandler<T> {
    /**
     * 设置SQL参数
     */
    void setParameter(PreparedStatement ps, int idx, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 根据 column name 读取SQL返回值
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 根据 column index 读取SQL返回值
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 根据 column index 读取存储过程返回值
     */
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;
}
