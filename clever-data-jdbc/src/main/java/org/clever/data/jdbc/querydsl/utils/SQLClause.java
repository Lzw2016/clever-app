package org.clever.data.jdbc.querydsl.utils;

import com.querydsl.core.dml.StoreClause;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.NullExpression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.Mapper;
import org.clever.core.Conv;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * SQLClause 工具类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/18 19:24 <br/>
 */
public class SQLClause {
    /**
     * StoreClause 原始的 set 实现
     */
    public interface StoreClauseRawSet {
        <T> void set(Path<T> path, @Nullable T value);

        <T> void set(Path<T> path, Expression<? extends T> expression);

        <T> void setNull(Path<T> path);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void populate(StoreClause<?> storeClause, RelationalPath<?> entity, T obj, Mapper<T> mapper) {
        Map<Path<?>, Object> values = mapper.createMap(entity, obj);
        for (Map.Entry<Path<?>, Object> entry : values.entrySet()) {
            storeClause.set((Path) entry.getKey(), entry.getValue());
        }
    }

    public static <T> void set(StoreClauseRawSet storeClause, Path<T> path, T value) {
        if (path != null
            && value != null
            && path.getType() != null
            && !path.getType().isAssignableFrom(value.getClass())) {
            setx(storeClause, path, value);
        } else {
            storeClause.set(path, value);
        }
    }

    public static <T> void set(StoreClauseRawSet storeClause, Path<T> path, Expression<? extends T> expression) {
        if (path != null
            && expression != null
            && path.getType() != null
            && expression.getType() != null
            && !path.getType().isAssignableFrom(expression.getType())) {
            setx(storeClause, path, expression);
        } else {
            if (expression == null) {
                storeClause.setNull(path);
            } else {
                storeClause.set(path, expression);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setx(StoreClauseRawSet storeClause, Path<?> path, Object value) {
        if (value == null) {
            storeClause.setNull(path);
        } else if (value instanceof NullExpression) {
            storeClause.setNull(path);
        } else if (value instanceof Expression) {
            storeClause.set(path, (Expression) value);
        } else {
            value = getFieldValue(path, value);
            storeClause.set((Path<Object>) path, value);
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
