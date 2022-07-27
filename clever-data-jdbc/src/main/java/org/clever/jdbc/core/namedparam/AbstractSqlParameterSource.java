package org.clever.jdbc.core.namedparam;

import org.clever.jdbc.core.SqlParameterValue;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * {@link SqlParameterSource}实现的抽象基类。
 * 为每个参数提供SQL类型注册，并提供友好的{@link #toString() toString}表示，
 * 枚举实现{@link #getParameterNames()}）的{@code SqlParameterSource}的所有参数。
 * 具体的子类必须实现{@link #hasValue} 和 {@link #getValue}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:53 <br/>
 *
 * @see #hasValue(String)
 * @see #getValue(String)
 * @see #getParameterNames()
 */
public abstract class AbstractSqlParameterSource implements SqlParameterSource {
    private final Map<String, Integer> sqlTypes = new HashMap<>();
    private final Map<String, String> typeNames = new HashMap<>();

    /**
     * 为给定参数注册SQL类型。
     *
     * @param paramName 参数的名称
     * @param sqlType   参数的SQL类型
     */
    public void registerSqlType(String paramName, int sqlType) {
        Assert.notNull(paramName, "Parameter name must not be null");
        this.sqlTypes.put(paramName, sqlType);
    }

    /**
     * 为给定参数注册SQL类型。
     *
     * @param paramName 参数的名称
     * @param typeName  参数的类型名称
     */
    public void registerTypeName(String paramName, String typeName) {
        Assert.notNull(paramName, "Parameter name must not be null");
        this.typeNames.put(paramName, typeName);
    }

    /**
     * 如果已注册，则返回给定参数的SQL类型。
     *
     * @param paramName 参数的名称
     * @return 参数的SQL类型，如果未注册，则{@code TYPE_UNKNOWN}
     */
    @Override
    public int getSqlType(String paramName) {
        Assert.notNull(paramName, "Parameter name must not be null");
        return this.sqlTypes.getOrDefault(paramName, TYPE_UNKNOWN);
    }

    /**
     * 如果已注册，则返回给定参数的类型名称。
     *
     * @param paramName 参数的名称
     * @return 参数的类型名，如果未注册，则为null
     */
    @Override
    public String getTypeName(String paramName) {
        Assert.notNull(paramName, "Parameter name must not be null");
        return this.typeNames.get(paramName);
    }

    /**
     * 枚举参数名称和值及其相应的SQL类型（如果可用），否则只返回简单的{@code SqlParameterSource}实现类名。
     *
     * @see #getParameterNames()
     */
    @Override
    public String toString() {
        String[] parameterNames = getParameterNames();
        if (parameterNames != null) {
            StringJoiner result = new StringJoiner(", ", getClass().getSimpleName() + " {", "}");
            for (String parameterName : parameterNames) {
                Object value = getValue(parameterName);
                if (value instanceof SqlParameterValue) {
                    value = ((SqlParameterValue) value).getValue();
                }
                String typeName = getTypeName(parameterName);
                if (typeName == null) {
                    int sqlType = getSqlType(parameterName);
                    if (sqlType != TYPE_UNKNOWN) {
                        typeName = JdbcUtils.resolveTypeName(sqlType);
                        if (typeName == null) {
                            typeName = String.valueOf(sqlType);
                        }
                    }
                }
                StringBuilder entry = new StringBuilder();
                entry.append(parameterName).append('=').append(value);
                if (typeName != null) {
                    entry.append(" (type:").append(typeName).append(')');
                }
                result.add(entry);
            }
            return result.toString();
        } else {
            return getClass().getSimpleName();
        }
    }
}
