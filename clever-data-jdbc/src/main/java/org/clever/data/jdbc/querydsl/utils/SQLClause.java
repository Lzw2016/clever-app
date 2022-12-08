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
 * SQLClause 工具类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/18 19:24 <br/>
 */
@SuppressWarnings("ALL")
public class SQLClause {
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

    public static Object getFieldValue(Path<?> path, Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        Class<?> fieldType = path.getType();
        if (fieldValue instanceof Boolean && Number.class.isAssignableFrom(fieldType)) {
            fieldValue = ((Boolean) fieldValue) ? 1 : 0;
        } else if (fieldType.isAssignableFrom(Number.class)) {
            fieldValue = Conv.asDecimal(fieldValue);
        } else if (fieldType.isAssignableFrom(Short.class) || fieldType.isAssignableFrom(short.class)) {
            fieldValue = Conv.asShort(fieldValue);
        } else if (fieldType.isAssignableFrom(Integer.class) || fieldType.isAssignableFrom(int.class)) {
            fieldValue = Conv.asInteger(fieldValue);
        } else if (fieldType.isAssignableFrom(Long.class) || fieldType.isAssignableFrom(long.class)) {
            fieldValue = Conv.asLong(fieldValue);
        } else if (fieldType.isAssignableFrom(Float.class) || fieldType.isAssignableFrom(float.class)) {
            fieldValue = Conv.asFloat(fieldValue);
        } else if (fieldType.isAssignableFrom(Double.class) || fieldType.isAssignableFrom(double.class)) {
            fieldValue = Conv.asDouble(fieldValue);
        } else if (fieldType.isAssignableFrom(Boolean.class) || fieldType.isAssignableFrom(boolean.class)) {
            fieldValue = Conv.asBoolean(fieldValue);
        } else if (fieldType.isAssignableFrom(BigDecimal.class)) {
            fieldValue = Conv.asDecimal(fieldValue);
        } else if (fieldType.isAssignableFrom(CharSequence.class)) {
            fieldValue = Conv.asString(fieldValue);
        } else if (fieldType.isAssignableFrom(String.class)) {
            fieldValue = Conv.asString(fieldValue);
        } else if (fieldType.isAssignableFrom(Date.class)) {
            fieldValue = Conv.asDate(fieldValue);
        } else if (fieldType.isAssignableFrom(Timestamp.class)) {
            fieldValue = Conv.asTimestamp(fieldValue);
        }
        return fieldValue;
    }
}
