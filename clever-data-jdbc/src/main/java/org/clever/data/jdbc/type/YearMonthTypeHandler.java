package org.clever.data.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;

/**
 * Type Handler for {@link YearMonth}.
 * <p>
 * YearMonthTypeHandler relies upon
 * {@link YearMonth#parse YearMonth.parse}. Therefore column values
 * are expected as strings. The format must be uuuu-MM. Example: "2016-08"
 */
public class YearMonthTypeHandler extends BaseTypeHandler<YearMonth> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, YearMonth yearMonth, JdbcType jt) throws SQLException {
        ps.setString(i, yearMonth.toString());
    }

    @Override
    public YearMonth getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : YearMonth.parse(value);
    }

    @Override
    public YearMonth getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : YearMonth.parse(value);
    }

    @Override
    public YearMonth getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : YearMonth.parse(value);
    }
}
