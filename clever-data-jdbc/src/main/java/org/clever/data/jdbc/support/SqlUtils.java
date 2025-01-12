package org.clever.data.jdbc.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.support.sqlparser.CountSqlOptimizer;
import org.clever.data.jdbc.support.sqlparser.JSqlParserCountSqlOptimizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/10 09:16 <br/>
 */
public class SqlUtils {
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String COMMA = ",";
    /**
     * count sql优化器
     */
    private final static CountSqlOptimizer COUNT_SQL_OPTIMIZER = new JSqlParserCountSqlOptimizer();
    /**
     * Count Sql 缓存(最大4096条数据)
     */
    private static final Cache<String, String> COUNT_SQL_CACHE = CacheBuilder.newBuilder().maximumSize(4096).initialCapacity(256).build();

    /**
     * 获取优化后的count查询sql语句
     *
     * @param sql 原始sql
     */
    public static String getCountSql(String sql) {
        String countSql = COUNT_SQL_CACHE.getIfPresent(StringUtils.trim(sql));
        if (StringUtils.isBlank(countSql)) {
            countSql = COUNT_SQL_OPTIMIZER.getCountSql(sql, CountSqlOptimizer.DEFAULT_OPTIONS);
            COUNT_SQL_CACHE.put(sql, countSql);
        }
        return countSql;
    }

    /**
     * 查询SQL拼接Order By
     *
     * @param originalSql 需要拼接的SQL
     * @param queryBySort 排序对象
     * @return ignore
     */
    public static String concatOrderBy(String originalSql, QueryBySort queryBySort) {
        if (null != queryBySort && queryBySort.getOrderFields() != null && !queryBySort.getOrderFields().isEmpty()) {
            List<String> orderFields = queryBySort.getOrderFieldsSql();
            List<String> sorts = queryBySort.getSortsSql();
            StringBuilder buildSql = new StringBuilder(originalSql);
            StringBuilder orderBySql = new StringBuilder();
            for (int index = 0; index < orderFields.size(); index++) {
                String orderField = orderFields.get(index);
                if (orderField != null) {
                    orderField = orderField.trim();
                }
                if (orderField == null || orderField.length() <= 0) {
                    continue;
                }
                String sort = ASC;
                if (sorts.size() > index) {
                    sort = sorts.get(index);
                    if (sort != null) {
                        sort = sort.trim();
                    }
                    if (!DESC.equalsIgnoreCase(sort) && !ASC.equalsIgnoreCase(sort)) {
                        sort = ASC;
                    }
                }
                String orderByStr = concatOrderBuilder(orderField, sort.toUpperCase());
                if (StringUtils.isNotBlank(orderByStr)) {
                    if (!orderBySql.isEmpty()) {
                        orderBySql.append(COMMA).append(' ');
                    }
                    orderBySql.append(orderByStr.trim());
                }
            }
            if (!orderBySql.isEmpty()) {
                buildSql.append(" ORDER BY ").append(orderBySql);
            }
            return buildSql.toString();
        }
        return originalSql;
    }

    /**
     * 生成更新table的sql
     *
     * @param tableName    表名称
     * @param fields       字段值
     * @param whereMap     where条件(and条件，只支持=)
     * @param paramsRename fields与whereMap字段名重命名策略
     */
    public static TupleTwo<String, Map<String, Object>> updateSql(String tableName, Map<String, Object> fields, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        Map<String, Object> paramMap = new HashMap<>(fields.size() + (whereMap == null ? 0 : whereMap.size()));
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(tableName).append(" set");
        int index = 0;
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            String fieldName = field.getKey();
            Object value = field.getValue();
            String fieldParam = "set_" + fieldName;
            if (index == 0) {
                sb.append(' ');
            } else {
                sb.append(", ");
            }
            sb.append(NamingUtils.rename(fieldName, paramsRename)).append("=:").append(fieldParam);
            paramMap.put(fieldParam, value);
            index++;
        }
        TupleTwo<String, Map<String, Object>> whereSql = getWhereSql(whereMap, paramsRename);
        sb.append(whereSql.getValue1());
        paramMap.putAll(whereSql.getValue2());
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    /**
     * 生成删除table的sql
     *
     * @param tableName    表名称
     * @param whereMap     where条件(and条件，只支持=)
     * @param paramsRename whereMap字段名重命名策略
     */
    public static TupleTwo<String, Map<String, Object>> deleteSql(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        Map<String, Object> paramMap = new HashMap<>((whereMap == null ? 0 : whereMap.size()));
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(tableName);
        TupleTwo<String, Map<String, Object>> whereSql = getWhereSql(whereMap, paramsRename);
        sb.append(whereSql.getValue1());
        paramMap.putAll(whereSql.getValue2());
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    /**
     * @param tableName    表名称
     * @param fields       字段名称
     * @param paramsRename fields字段名重命名策略
     */
    public static TupleTwo<String, Map<String, Object>> insertSql(String tableName, Map<String, Object> fields, RenameStrategy paramsRename) {
        Map<String, Object> paramMap = new HashMap<>(fields.size());
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(tableName).append(" (");
        int index = 0;
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            String fieldName = field.getKey();
            if (index != 0) {
                sb.append(", ");
            }
            sb.append(NamingUtils.rename(fieldName, paramsRename));
            index++;
        }
        sb.append(") values (");
        index = 0;
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            String fieldName = field.getKey();
            Object value = field.getValue();
            if (index != 0) {
                sb.append(", ");
            }
            sb.append(":").append(fieldName);
            paramMap.put(fieldName, value);
            index++;
        }
        sb.append(")");
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    /**
     * 生成查询table的sql
     *
     * @param tableName    表名称
     * @param whereMap     where条件(and条件，只支持=)
     * @param paramsRename whereMap字段名重命名策略
     */
    public static TupleTwo<String, Map<String, Object>> selectSql(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        Map<String, Object> paramMap = new HashMap<>((whereMap == null ? 0 : whereMap.size()));
        StringBuilder sb = new StringBuilder();
        sb.append("select * from ").append(tableName);
        TupleTwo<String, Map<String, Object>> whereSql = getWhereSql(whereMap, paramsRename);
        sb.append(whereSql.getValue1());
        paramMap.putAll(whereSql.getValue2());
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    /**
     * 生成存储过程调用SQL
     *
     * @param procedureName 存储过程名称
     * @param params        参数
     */
    public static TupleTwo<String, Map<String, Object>> getCallSql(String procedureName, List<Object> params) {
        Map<String, Object> paramMap = new HashMap<>((params == null ? 0 : params.size()));
        StringBuilder sb = new StringBuilder();
        sb.append("call ").append(StringUtils.trim(procedureName)).append("(");
        if (params != null && !params.isEmpty()) {
            for (int i = 0; i < params.size(); i++) {
                String name = "param_" + i;
                Object value = params.get(i);
                paramMap.put(name, value);
                sb.append(":").append(name);
                if ((i + 1) < params.size()) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    /**
     * 根据where参数生成where sql字符串
     */
    private static TupleTwo<String, Map<String, Object>> getWhereSql(Map<String, Object> whereMap, RenameStrategy paramsRename) {
        Map<String, Object> paramMap = new HashMap<>((whereMap == null ? 0 : whereMap.size()));
        StringBuilder sb = new StringBuilder();
        if (whereMap != null && !whereMap.isEmpty()) {
            sb.append(" where");
            int index = 0;
            for (Map.Entry<String, ?> where : whereMap.entrySet()) {
                String fieldName = where.getKey();
                Object value = where.getValue();
                String fieldParam = "where_" + fieldName;
                if (index == 0) {
                    sb.append(' ');
                } else {
                    sb.append(" and ");
                }
                sb.append(NamingUtils.rename(fieldName, paramsRename)).append("=:").append(fieldParam);
                paramMap.put(fieldParam, value);
                index++;
            }
        }
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    /**
     * 拼接多个排序方法
     *
     * @param column    ignore
     * @param orderWord ignore
     */
    private static String concatOrderBuilder(String column, String orderWord) {
        if (StringUtils.isNotBlank(column)) {
            return column + ' ' + orderWord;
        }
        return StringUtils.EMPTY;
    }
}
