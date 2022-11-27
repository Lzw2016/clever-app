package org.clever.data.jdbc.querydsl.utils;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import org.clever.core.Conv;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/18 19:24 <br/>
 */
public class SQLClause {
    @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
    public static void setx(SQLInsertClause insertClause, Path<?> path, Object value) {
        if (value == null) {
            insertClause.setNull(path);
        } else if (value instanceof Expression) {
            insertClause.set(path, (Expression) value);
        } else {
            value = getFieldValue(path, value);
            insertClause.set((Path<Object>) path, value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
    public static void setx(SQLUpdateClause updateClause, Path<?> path, Object value) {
        if (value == null) {
            updateClause.setNull(path);
        } else if (value instanceof Expression) {
            updateClause.set(path, (Expression) value);
        } else {
            value = getFieldValue(path, value);
            updateClause.set((Path<Object>) path, value);
        }
    }

    public static Object getFieldValue(final Path<?> path, final Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        Class<?> fieldType = path.getType();
        Object value = null;
        if (fieldType.isAssignableFrom(Number.class)) {
            value = Conv.asDecimal(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Short.class) || fieldType.isAssignableFrom(short.class)) {
            value = Conv.asShort(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Integer.class) || fieldType.isAssignableFrom(int.class)) {
            value = Conv.asInteger(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Long.class) || fieldType.isAssignableFrom(long.class)) {
            value = Conv.asLong(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Float.class) || fieldType.isAssignableFrom(float.class)) {
            value = Conv.asFloat(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Double.class) || fieldType.isAssignableFrom(double.class)) {
            value = Conv.asDouble(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Boolean.class) || fieldType.isAssignableFrom(boolean.class)) {
            value = Conv.asBoolean(fieldValue, null);
        } else if (fieldType.isAssignableFrom(BigDecimal.class)) {
            value = Conv.asDecimal(fieldValue, null);
        } else if (fieldType.isAssignableFrom(CharSequence.class)) {
            value = Conv.asString(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Date.class)) {
            value = Conv.asDate(fieldValue, null);
        } else if (fieldType.isAssignableFrom(Timestamp.class)) {
            value = Conv.asTimestamp(fieldValue, null);
        }
        if (value == null) {
            value = fieldValue;
        }
        return value;
    }
}
