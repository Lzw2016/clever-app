package org.clever.jdbc.core.namedparam;

import org.clever.jdbc.core.SqlParameterValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 类，该类提供了使用{@link SqlParameterSource}的助手方法，特别是{@link NamedParameterJdbcTemplate}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:54 <br/>
 */
public abstract class SqlParameterSourceUtils {
    /**
     * 创建一个{@link SqlParameterSource}对象数组，其中填充了来自传入值的数据（{@link Map}或bean对象）。
     * 这将定义批处理操作中包含的内容。
     *
     * @param candidates 包含要使用的值的对象的对象数组
     * @return {@link SqlParameterSource} 数组
     * @see MapSqlParameterSource
     * @see BeanPropertySqlParameterSource
     * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
     */
    public static SqlParameterSource[] createBatch(Object... candidates) {
        return createBatch(Arrays.asList(candidates));
    }

    /**
     * 创建一个{@link SqlParameterSource}对象数组，其中填充了来自传入值的数据（{@link Map}或bean对象）。
     * 这将定义批处理操作中包含的内容。
     *
     * @param candidates 包含要使用的值的对象集合
     * @return {@link SqlParameterSource} 数组
     * @see MapSqlParameterSource
     * @see BeanPropertySqlParameterSource
     * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
     */
    @SuppressWarnings("unchecked")
    public static SqlParameterSource[] createBatch(Collection<?> candidates) {
        SqlParameterSource[] batch = new SqlParameterSource[candidates.size()];
        int i = 0;
        for (Object candidate : candidates) {
            batch[i] = (candidate instanceof Map ?
                    new MapSqlParameterSource((Map<String, ?>) candidate) :
                    new BeanPropertySqlParameterSource(candidate)
            );
            i++;
        }
        return batch;
    }

    /**
     * 创建一个{@link MapSqlParameterSource}对象数组，其中填充了来自传入值的数据。
     * 这将定义批处理操作中包含的内容。
     *
     * @param valueMaps 包含要使用的值的{@link Map}实例数组
     * @return {@link SqlParameterSource} 数组
     * @see MapSqlParameterSource
     * @see NamedParameterJdbcTemplate#batchUpdate(String, Map[])
     */
    public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
        SqlParameterSource[] batch = new SqlParameterSource[valueMaps.length];
        for (int i = 0; i < valueMaps.length; i++) {
            batch[i] = new MapSqlParameterSource(valueMaps[i]);
        }
        return batch;
    }

    /**
     * 如果参数具有类型信息，则创建包装值，如果没有，则创建普通对象。
     *
     * @param source        参数值和类型信息的来源
     * @param parameterName 参数的名称
     * @return value对象
     * @see SqlParameterValue
     */
    public static Object getTypedValue(SqlParameterSource source, String parameterName) {
        int sqlType = source.getSqlType(parameterName);
        if (sqlType != SqlParameterSource.TYPE_UNKNOWN) {
            return new SqlParameterValue(sqlType, source.getTypeName(parameterName), source.getValue(parameterName));
        } else {
            return source.getValue(parameterName);
        }
    }

    /**
     * 创建不区分大小写的参数名称与原始名称的Map。
     *
     * @param parameterSource 参数名称的来源
     * @return 可用于参数名称不区分大小写匹配的Map
     */
    public static Map<String, String> extractCaseInsensitiveParameterNames(SqlParameterSource parameterSource) {
        Map<String, String> caseInsensitiveParameterNames = new HashMap<>();
        String[] paramNames = parameterSource.getParameterNames();
        if (paramNames != null) {
            for (String name : paramNames) {
                caseInsensitiveParameterNames.put(name.toLowerCase(), name);
            }
        }
        return caseInsensitiveParameterNames;
    }
}
