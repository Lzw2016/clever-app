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
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

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
        if ((fieldValue instanceof Boolean || "false".equals(fieldValue) || "true".equals(fieldValue)) && Number.class.isAssignableFrom(fieldType)) {
            fieldValue = (Objects.equals(fieldValue, true) || Objects.equals(fieldValue, "true")) ? 1 : 0;
        } else if (fieldType.isAssignableFrom(Number.class)) {
            fieldValue = Conv.asDecimal(fieldValue);
        } else if (fieldType.isAssignableFrom(Byte.class) || fieldType.isAssignableFrom(byte.class)) {
            fieldValue = Conv.asByte(fieldValue);
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
        } else if (fieldType.isAssignableFrom(java.sql.Date.class)) {
            Date date = Conv.asDate(fieldValue);
            fieldValue = new java.sql.Date(date.getTime());
        } else if (fieldType.isAssignableFrom(Timestamp.class)) {
            fieldValue = Conv.asTimestamp(fieldValue);
        } else if (fieldType.isAssignableFrom(Time.class)) {
            fieldValue = Conv.asTime(fieldValue);
        } else if (fieldType.isAssignableFrom(Instant.class)) {
            fieldValue = Conv.asDate(fieldValue).toInstant();
        } else if (fieldType.isAssignableFrom(LocalDate.class)) {
            Date date = Conv.asDate(fieldValue);
            fieldValue = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (fieldType.isAssignableFrom(LocalTime.class)) {
            Date date = Conv.asDate(fieldValue);
            fieldValue = date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        } else if (fieldType.isAssignableFrom(LocalDateTime.class)) {
            Date date = Conv.asDate(fieldValue);
            fieldValue = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (fieldType.isAssignableFrom(ZonedDateTime.class)) {
            Date date = Conv.asDate(fieldValue);
            fieldValue = date.toInstant().atZone(ZoneId.systemDefault());
        } else if (fieldType.isAssignableFrom(OffsetTime.class)) {
            Date date = Conv.asDate(fieldValue);
            ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
            fieldValue = zonedDateTime.toLocalTime().atOffset(zonedDateTime.getOffset());
        } else if (fieldType.isAssignableFrom(OffsetDateTime.class)) {
            Date date = Conv.asDate(fieldValue);
            ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
            fieldValue = zonedDateTime.toLocalDateTime().atOffset(zonedDateTime.getOffset());
        }
        return fieldValue;
    }
}
