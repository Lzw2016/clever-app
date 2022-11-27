package org.clever.data.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ByteTypeHandler extends BaseTypeHandler<Byte> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Byte parameter, JdbcType jdbcType) throws SQLException {
        ps.setByte(i, parameter);
    }

    @Override
    public Byte getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte result = rs.getByte(columnName);
        return result == 0 && rs.wasNull() ? null : result;
    }

    @Override
    public Byte getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte result = rs.getByte(columnIndex);
        return result == 0 && rs.wasNull() ? null : result;
    }

    @Override
    public Byte getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte result = cs.getByte(columnIndex);
        return result == 0 && cs.wasNull() ? null : result;
    }
}
