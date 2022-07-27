package org.clever.jdbc.core.namedparam;

import org.clever.jdbc.core.SqlParameterValue;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 包含给定参数Map的{@link SqlParameterSource}实现。
 *
 * <p>此类用于将参数值的简单Map传递给{@link NamedParameterJdbcTemplate}类的方法。
 *
 * <p>这个类上的{@code addValue}方法将使添加几个值变得更容易。
 * 这些方法返回对{@link MapSqlParameterSource}本身的引用，因此可以在单个语句中将多个方法调用链接在一起。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:53 <br/>
 *
 * @see #addValue(String, Object)
 * @see #addValue(String, Object, int)
 * @see #registerSqlType
 * @see NamedParameterJdbcTemplate
 */
public class MapSqlParameterSource extends AbstractSqlParameterSource {
    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * 创建一个空的MapSqlParameterSource，其中的值将通过 {@code addValue}.
     *
     * @see #addValue(String, Object)
     */
    public MapSqlParameterSource() {
    }

    /**
     * 创建一个新的MapSqlParameterSource，其中一个值由提供的参数组成。
     *
     * @param paramName 参数的名称
     * @param value     参数的值
     * @see #addValue(String, Object)
     */
    public MapSqlParameterSource(String paramName, Object value) {
        addValue(paramName, value);
    }

    /**
     * 基于Map创建新的MapSqlParameterSource。
     *
     * @param values 包含现有参数值的Map（可以为null）
     */
    public MapSqlParameterSource(Map<String, ?> values) {
        addValues(values);
    }

    /**
     * 将参数添加到此参数源。
     *
     * @param paramName 参数的名称
     * @param value     参数的值
     * @return 对该参数源的引用，因此可以将多个调用链接在一起
     */
    public MapSqlParameterSource addValue(String paramName, Object value) {
        Assert.notNull(paramName, "Parameter name must not be null");
        this.values.put(paramName, value);
        if (value instanceof SqlParameterValue) {
            registerSqlType(paramName, ((SqlParameterValue) value).getSqlType());
        }
        return this;
    }

    /**
     * 将参数添加到此参数源。
     *
     * @param paramName 参数的名称
     * @param value     参数的值
     * @param sqlType   参数的SQL类型
     * @return 对该参数源的引用，因此可以将多个调用链接在一起
     */
    public MapSqlParameterSource addValue(String paramName, Object value, int sqlType) {
        Assert.notNull(paramName, "Parameter name must not be null");
        this.values.put(paramName, value);
        registerSqlType(paramName, sqlType);
        return this;
    }

    /**
     * 将参数添加到此参数源。
     *
     * @param paramName 参数的名称
     * @param value     参数的值
     * @param sqlType   参数的SQL类型
     * @param typeName  参数的类型名称
     * @return 对该参数源的引用，因此可以将多个调用链接在一起
     */
    public MapSqlParameterSource addValue(String paramName, Object value, int sqlType, String typeName) {
        Assert.notNull(paramName, "Parameter name must not be null");
        this.values.put(paramName, value);
        registerSqlType(paramName, sqlType);
        registerTypeName(paramName, typeName);
        return this;
    }

    /**
     * 将参数Map添加到此参数源。
     *
     * @param values 包含现有参数值的Map（可以为null）
     * @return 对该参数源的引用，因此可以将多个调用链接在一起
     */
    @SuppressWarnings("UnusedReturnValue")
    public MapSqlParameterSource addValues(Map<String, ?> values) {
        if (values != null) {
            values.forEach((key, value) -> {
                this.values.put(key, value);
                if (value instanceof SqlParameterValue) {
                    registerSqlType(key, ((SqlParameterValue) value).getSqlType());
                }
            });
        }
        return this;
    }

    /**
     * 将当前参数值公开为只读Map。
     */
    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(this.values);
    }

    @Override
    public boolean hasValue(String paramName) {
        return this.values.containsKey(paramName);
    }

    @Override
    public Object getValue(String paramName) {
        if (!hasValue(paramName)) {
            throw new IllegalArgumentException("No value registered for key '" + paramName + "'");
        }
        return this.values.get(paramName);
    }

    @Override
    public String[] getParameterNames() {
        return StringUtils.toStringArray(this.values.keySet());
    }
}
