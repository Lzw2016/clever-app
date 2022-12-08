package org.clever.data.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.dialects.DialectFactory;
import org.clever.data.jdbc.dialects.IDialect;
import org.clever.data.jdbc.support.SqlUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/07/22 15:56 <br/>
 */
@SuppressWarnings("ALL")
public class SQLBuilder {
    /**
     * fields与whereMap字段名默认的重命名策略
     */
    private static final RenameStrategy DEFAULT_PARAMS_RENAME = RenameStrategy.ToUnderline;

    private static final String SPACE = StringUtils.SPACE;
    private static final String COMMA = ",";
    private static final String WHERE_AND = "AND";

    /**
     * 创建一个Select SQL构建器
     */
    public static SelectBuilder newSelectBuilder() {
        return new SelectBuilder();
    }

    /**
     * 创建一个Update SQL构建器
     */
    public static UpdateBuilder newUpdateBuilder() {
        return new UpdateBuilder();
    }

    /**
     * 创建一个Insert SQL构建器
     */
    public static InsertBuilder newInsertBuilder() {
        return new InsertBuilder();
    }

    /**
     * 创建一个Delete SQL构建器
     */
    public static DeleteBuilder newDeleteBuilder() {
        return new DeleteBuilder();
    }

    /**
     * 当最后一个非空白字符是{delStr}时，将其删除
     */
    private static void delLast(StringBuilder sb, String delStr) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            char c = sb.charAt(i);
            if (Character.isWhitespace(c)) {
                sb.deleteCharAt(i);
                continue;
            }
            if (sb.toString().endsWith(delStr)) {
                sb.delete(sb.length() - delStr.length(), sb.length());
            }
            break;
        }
    }

    /**
     * 获取Update Sql中set段
     *
     * @param fields       字段值
     * @param paramsRename fields字段名重命名策略
     */
    private static TupleTwo<String, Map<String, Object>> getUpdateSql(Map<String, Object> fields, RenameStrategy paramsRename) {
        Map<String, Object> paramMap = new HashMap<>(fields.size());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            String fieldName = field.getKey();
            Object value = field.getValue();
            String fieldParam = "set_" + fieldName;
            if (sb.length() > 0) {
                sb.append(SPACE);
            }
            sb.append(NamingUtils.rename(fieldName, paramsRename)).append("=:").append(fieldParam).append(COMMA);
            paramMap.put(fieldParam, value);
        }
        return TupleTwo.creat(sb.toString(), paramMap);
    }

    public static enum Sort {
        ASC("ASC"),
        DESC("DESC");
        private String sort;

        private Sort(String sort) {
            this.sort = sort;
        }

        @Override
        public String toString() {
            return sort;
        }
    }

    /**
     * Select SQL构建器
     */
    public static class SelectBuilder {
        /**
         * 数据库类型(跟分页有关系)
         */
        private DbType dbType = DbType.MYSQL;
        /**
         * 数据偏移量(1 <= pageSize <= 1000)
         */
        private Integer size;
        /**
         * 第一条数据的起始位置(offset >= 0)
         */
        private Integer offset;
        /**
         * sql参数
         */
        private final Map<String, Object> paramMap = new HashMap<>();
        /**
         * 查询的的列(字段)
         */
        private final StringBuilder select = new StringBuilder();
        /**
         * 查询的表
         */
        private final StringBuilder from = new StringBuilder();
        /**
         * 查询条件
         */
        private final StringBuilder where = new StringBuilder();
        /**
         * 查询分组
         */
        private final StringBuilder groupBy = new StringBuilder();
        /**
         * 过滤分组条件
         */
        private final StringBuilder having = new StringBuilder();
        /**
         * 排序字段(列)
         */
        private final StringBuilder orderBy = new StringBuilder();

        private SelectBuilder() {
        }

        public DbType getDbType() {
            return dbType;
        }

        /**
         * 获取 sql
         */
        public String buildSql() {
            // SELECT <select> FROM <from> WHERE <where> GROUP BY <groupBy> HAVING <having> ORDER BY <orderBy>
            StringBuilder sql = new StringBuilder();
            delLast(this.select, COMMA);
            delLast(this.from, COMMA);
            delLast(this.where, WHERE_AND);
            delLast(this.groupBy, COMMA);
            delLast(this.having, WHERE_AND);
            delLast(this.orderBy, COMMA);
            final String select = StringUtils.trimToEmpty(this.select.toString());
            final String from = StringUtils.trimToEmpty(this.from.toString());
            final String where = StringUtils.trimToEmpty(this.where.toString());
            final String groupBy = StringUtils.trimToEmpty(this.groupBy.toString());
            final String having = StringUtils.trimToEmpty(this.having.toString());
            final String orderBy = StringUtils.trimToEmpty(this.orderBy.toString());
            if (StringUtils.isNotBlank(select)) {
                sql.append("SELECT ").append(select).append(SPACE);
            }
            if (StringUtils.isNotBlank(from)) {
                sql.append("FROM ").append(from).append(SPACE);
            }
            if (StringUtils.isNotBlank(where)) {
                sql.append("WHERE ").append(where).append(SPACE);
            }
            if (StringUtils.isNotBlank(groupBy)) {
                sql.append("GROUP BY ").append(groupBy).append(SPACE);
            }
            if (StringUtils.isNotBlank(having)) {
                sql.append("HAVING ").append(having).append(SPACE);
            }
            if (StringUtils.isNotBlank(orderBy)) {
                sql.append("ORDER BY ").append(orderBy).append(SPACE);
            }
            String sqlStr = StringUtils.trimToEmpty(sql.toString());
            if (offset != null && size != null) {
                if (dbType == null) {
                    dbType = DbType.MYSQL;
                }
                if (offset < 0) {
                    offset = 0;
                }
                if (size < 1) {
                    size = 1;
                }
                if (size > QueryByPage.PAGE_SIZE_MAX) {
                    size = QueryByPage.PAGE_SIZE_MAX;
                }
                IDialect dialect = DialectFactory.getDialect(dbType, null);
                sqlStr = dialect.buildPaginationSql(sqlStr, offset, size, paramMap);
            }
            return sqlStr;
        }

        /**
         * 获取 count sql
         */
        public String buildCountSql() {
            // 备份设置
            final Integer offset = this.offset;
            final Integer size = this.size;
            this.offset = null;
            this.size = null;
            try {
                String sql = buildSql();
                return SqlUtils.getCountSql(sql);
            } finally {
                // 还原设置
                this.offset = offset;
                this.size = size;
            }
        }

        public SelectBuilder setDbType(DbType dbType) {
            this.dbType = dbType;
            return this;
        }

        /**
         * 获取参数
         */
        public Map<String, Object> getParams() {
            return paramMap;
        }

        /**
         * 增加参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public SelectBuilder addParam(String name, Object value, boolean bool) {
            if (bool) {
                paramMap.put(name, value);
            }
            return this;
        }

        /**
         * 增加参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public SelectBuilder addParam(String name, Object value) {
            return addParam(name, value, true);
        }

        /**
         * 增加参数
         */
        public SelectBuilder addParams(Map<String, Object> params, boolean bool) {
            if (bool && params != null && !params.isEmpty()) {
                paramMap.putAll(params);
            }
            return this;
        }

        /**
         * 增加参数
         */
        public SelectBuilder addParams(Map<String, Object> params) {
            return addParams(params, true);
        }

        /**
         * 重新设置查询列
         */
        public SelectBuilder setSelect(String columns, boolean bool) {
            if (bool) {
                select.delete(0, select.length());
                select.append(columns);
                delLast(select, COMMA);
                select.append(COMMA);
            }
            return this;
        }

        /**
         * 重新设置查询列
         */
        public SelectBuilder setSelect(String columns) {
            return setSelect(columns, true);
        }

        /**
         * 新增查询列
         */
        public SelectBuilder addColumn(String column, boolean bool) {
            if (bool) {
                select.append(SPACE).append(column);
                delLast(select, COMMA);
                select.append(COMMA);
            }
            return this;
        }

        /**
         * 新增查询列
         */
        public SelectBuilder addColumn(String column) {
            return addColumn(column, true);
        }

        /**
         * 重新设置查询的表(包含关联语句: join...on)
         */
        public SelectBuilder setTable(String table, boolean bool) {
            if (bool) {
                from.delete(0, from.length());
                from.append(table);
                delLast(from, COMMA);
                from.append(COMMA);
            }
            return this;
        }

        /**
         * 重新设置查询的表(包含关联语句: join...on)
         */
        public SelectBuilder setTable(String table) {
            return setTable(table, true);
        }

        /**
         * 新增查询的表
         */
        public SelectBuilder addTable(String table, boolean bool) {
            if (bool) {
                from.append(SPACE).append(table);
                delLast(from, COMMA);
                from.append(COMMA);
            }
            return this;
        }

        /**
         * 新增查询的表
         */
        public SelectBuilder addTable(String table) {
            return addTable(table, true);
        }

        /**
         * 内关联查询
         *
         * @param joinTable 关联表
         * @param condition 关联条件: on (...)
         */
        public SelectBuilder innerJoin(String joinTable, String condition, boolean bool) {
            if (bool) {
                delLast(from, COMMA);
                from.append(SPACE).append("INNER JOIN").append(SPACE).append(joinTable).append(SPACE).append(condition);
                delLast(from, COMMA);
                from.append(COMMA);
            }
            return this;
        }

        /**
         * 内关联查询
         *
         * @param joinTable 关联表
         * @param condition 关联条件: on (...)
         */
        public SelectBuilder innerJoin(String joinTable, String condition) {
            return innerJoin(joinTable, condition, true);
        }

        /**
         * 左关联查询
         *
         * @param joinTable 关联表
         * @param condition 关联条件: on (...)
         */
        public SelectBuilder leftJoin(String joinTable, String condition, boolean bool) {
            if (bool) {
                delLast(from, COMMA);
                from.append(SPACE).append("LEFT JOIN").append(SPACE).append(joinTable).append(SPACE).append(condition);
                delLast(from, COMMA);
                from.append(COMMA);
            }
            return this;
        }

        /**
         * 左关联查询
         *
         * @param joinTable 关联表
         * @param condition 关联条件: on (...)
         */
        public SelectBuilder leftJoin(String joinTable, String condition) {
            return leftJoin(joinTable, condition, true);
        }

        /**
         * 右关联查询
         *
         * @param joinTable 关联表
         * @param condition 关联条件: on (...)
         */
        public SelectBuilder rightJoin(String joinTable, String condition, boolean bool) {
            if (bool) {
                delLast(from, COMMA);
                from.append(SPACE).append("RIGHT JOIN").append(SPACE).append(joinTable).append(SPACE).append(condition);
                delLast(from, COMMA);
                from.append(COMMA);
            }
            return this;
        }

        /**
         * 右关联查询
         *
         * @param joinTable 关联表
         * @param condition 关联条件: on (...)
         */
        public SelectBuilder rightJoin(String joinTable, String condition) {
            return rightJoin(joinTable, condition, true);
        }

        /**
         * 重新设置查询条件
         *
         * @param condition 查询条件
         * @param params    查询参数
         */
        public SelectBuilder setWhere(String condition, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                where.delete(0, where.length());
                where.append(condition);
                delLast(where, WHERE_AND);
                where.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 重新设置查询条件
         *
         * @param condition  查询条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder setWhere(String condition, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setWhere(condition, params, bool);
        }

        /**
         * 重新设置查询条件
         *
         * @param condition 查询条件
         * @param params    查询参数
         */
        public SelectBuilder setWhere(String condition, Map<String, Object> params) {
            return setWhere(condition, params, true);
        }

        /**
         * 重新设置查询条件
         *
         * @param condition  查询条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder setWhere(String condition, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setWhere(condition, params);
        }

        /**
         * 重新设置查询条件
         *
         * @param condition 查询条件
         */
        public SelectBuilder setWhere(String condition) {
            return setWhere(condition, null, true);
        }

        /**
         * 增加查询条件
         *
         * @param condition 查询条件
         * @param params    查询参数
         */
        public SelectBuilder addWhere(String condition, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                where.append(SPACE).append(condition);
                delLast(where, WHERE_AND);
                where.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 增加查询条件
         *
         * @param condition  查询条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder addWhere(String condition, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addWhere(condition, params, bool);
        }

        /**
         * 增加查询条件
         *
         * @param condition 查询条件
         * @param params    查询参数
         */
        public SelectBuilder addWhere(String condition, Map<String, Object> params) {
            return addWhere(condition, params, true);
        }

        /**
         * 增加查询条件
         *
         * @param condition  查询条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder addWhere(String condition, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addWhere(condition, params);
        }

        /**
         * 增加查询条件
         *
         * @param condition 查询条件
         */
        public SelectBuilder addWhere(String condition) {
            return addWhere(condition, null, true);
        }

        /**
         * 重新设置查询分组
         */
        public SelectBuilder setGroupBy(String groupBy, boolean bool) {
            if (bool) {
                this.groupBy.delete(0, this.groupBy.length());
                this.groupBy.append(groupBy);
                delLast(this.groupBy, COMMA);
                this.groupBy.append(COMMA);
            }
            return this;
        }

        /**
         * 重新设置查询分组
         */
        public SelectBuilder setGroupBy(String groupBy) {
            return setGroupBy(groupBy, true);
        }

        /**
         * 新增查询分组列
         */
        public SelectBuilder addGroupBy(String column, boolean bool) {
            if (bool) {
                this.groupBy.append(SPACE).append(column);
                delLast(this.groupBy, COMMA);
                this.groupBy.append(COMMA);
            }
            return this;
        }

        /**
         * 新增查询分组列
         */
        public SelectBuilder addGroupBy(String column) {
            return addGroupBy(column, true);
        }

        /**
         * 重新设置having条件
         *
         * @param having having条件
         * @param params 查询参数
         */
        public SelectBuilder setHaving(String having, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                this.having.delete(0, this.having.length());
                this.having.append(having);
                delLast(this.having, WHERE_AND);
                this.having.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 重新设置having条件
         *
         * @param having     having条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder setHaving(String having, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setHaving(having, params, bool);
        }

        /**
         * 重新设置having条件
         *
         * @param having having条件
         * @param params 查询参数
         */
        public SelectBuilder setHaving(String having, Map<String, Object> params) {
            return setHaving(having, params, true);
        }

        /**
         * 重新设置having条件
         *
         * @param having     having条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder setHaving(String having, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setHaving(having, params);
        }

        /**
         * 重新设置having条件
         *
         * @param having having条件
         */
        public SelectBuilder setHaving(String having) {
            return setHaving(having, null, true);
        }

        /**
         * 新增having条件
         *
         * @param having having条件
         * @param params 参数
         */
        public SelectBuilder addHaving(String having, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                this.having.append(SPACE).append(having);
                delLast(this.having, WHERE_AND);
                this.having.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 新增having条件
         *
         * @param having     having条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder addHaving(String having, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addHaving(having, params, bool);
        }

        /**
         * 新增having条件
         *
         * @param having having条件
         * @param params 参数
         */
        public SelectBuilder addHaving(String having, Map<String, Object> params) {
            return addHaving(having, params, true);
        }

        /**
         * 新增having条件
         *
         * @param having     having条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public SelectBuilder addHaving(String having, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addHaving(having, params);
        }

        /**
         * 增加过滤分组条件
         *
         * @param having 过滤分组条件
         */
        public SelectBuilder addHaving(String having) {
            return addHaving(having, null, true);
        }

        /**
         * 重新设置排序字段
         */
        public SelectBuilder setOrderBy(String orderBy, boolean bool) {
            if (bool) {
                this.orderBy.delete(0, this.orderBy.length());
                this.orderBy.append(orderBy);
                delLast(this.orderBy, COMMA);
                this.orderBy.append(COMMA);
            }
            return this;
        }

        /**
         * 重新设置排序字段
         */
        public SelectBuilder setOrderBy(String orderBy) {
            return setOrderBy(orderBy, true);
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         * @param sort    排序规则 ASC DESC
         */
        public SelectBuilder addOrderBy(String orderBy, Sort sort, boolean bool) {
            if (bool) {
                this.orderBy.append(SPACE).append(orderBy).append(SPACE).append(sort);
                delLast(this.orderBy, COMMA);
                this.orderBy.append(COMMA);
            }
            return this;
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         * @param sort    排序规则 ASC DESC
         */
        public SelectBuilder addOrderBy(String orderBy, Sort sort) {
            return addOrderBy(orderBy, sort, true);
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         */
        public SelectBuilder addOrderBy(String orderBy) {
            return addOrderBy(orderBy, Sort.ASC, true);
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         */
        public SelectBuilder addOrderByAsc(String orderBy, boolean bool) {
            return addOrderBy(orderBy, Sort.ASC, bool);
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         */
        public SelectBuilder addOrderByAsc(String orderBy) {
            return addOrderBy(orderBy, Sort.ASC, true);
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         */
        public SelectBuilder addOrderByDesc(String orderBy, boolean bool) {
            return addOrderBy(orderBy, Sort.DESC, bool);
        }

        /**
         * 增加排序字段
         *
         * @param orderBy 排序字段
         */
        public SelectBuilder addOrderByDesc(String orderBy) {
            return addOrderBy(orderBy, Sort.DESC, true);
        }

        /**
         * 设置查询的数据位置
         *
         * @param offset 数据偏移量
         * @param size   数据数量
         */
        public SelectBuilder setLimit(int offset, int size, boolean bool) {
            if (bool) {
                this.offset = offset;
                this.size = size;
            }
            return this;
        }

        /**
         * 设置查询的数据位置
         *
         * @param offset 数据偏移量
         * @param size   数据数量
         */
        public SelectBuilder setLimit(int offset, int size) {
            return setLimit(offset, size, true);
        }

        /**
         * 清空查询的数据位置
         */
        public SelectBuilder clearLimit(boolean bool) {
            if (bool) {
                this.offset = null;
                this.size = null;
            }
            return this;
        }

        /**
         * 清空查询的数据位置
         */
        public SelectBuilder clearLimit() {
            return clearLimit(true);
        }

        /**
         * 设置查询分页位置
         *
         * @param pageNo   数据页(>=1)
         * @param pageSize 页大小
         */
        public SelectBuilder setPagination(int pageNo, int pageSize, boolean bool) {
            if (bool) {
                if (pageNo < 1) {
                    pageNo = 1;
                }
                this.offset = (pageNo - 1) * pageSize;
                this.size = pageSize;
            }
            return this;
        }

        /**
         * 设置查询分页位置
         *
         * @param pageNo   数据页(>=1)
         * @param pageSize 页大小
         */
        public SelectBuilder setPagination(int pageNo, int pageSize) {
            return setPagination(pageNo, pageSize, true);
        }

        /**
         * 清空查询分页位置
         */
        public SelectBuilder clearPagination(boolean bool) {
            return clearLimit(bool);
        }

        /**
         * 清空查询分页位置
         */
        public SelectBuilder clearPagination() {
            return clearLimit(true);
        }

        /**
         * 设置查询的数据量
         *
         * @param top 查询前n条数据(>=1)
         */
        public SelectBuilder setTop(int top, boolean bool) {
            if (bool) {
                this.offset = 0;
                this.size = top;
            }
            return this;
        }

        /**
         * 设置查询的数据量
         *
         * @param top 查询前n条数据(>=1)
         */
        public SelectBuilder setTop(int top) {
            return setTop(top, true);
        }

        /**
         * 清空查询的数据量
         */
        public SelectBuilder clearTop(boolean bool) {
            return clearLimit(bool);
        }

        /**
         * 清空查询的数据量
         */
        public SelectBuilder clearTop() {
            return clearLimit(true);
        }
    }

    /**
     * Update SQL构建器
     */
    public static class UpdateBuilder {
        /**
         * sql参数
         */
        private final Map<String, Object> paramMap = new HashMap<>();
        /**
         * 更新表
         */
        private String table = "";
        /**
         * 更新字段
         */
        private final StringBuilder fields = new StringBuilder();
        /**
         * 更新条件
         */
        private final StringBuilder where = new StringBuilder();

        private UpdateBuilder() {
        }

        /**
         * 获取 sql
         */
        public String buildSql() {
            // UPDATE <table> SET <fields> WHERE <where>
            StringBuilder sql = new StringBuilder();
            delLast(this.fields, COMMA);
            delLast(this.where, WHERE_AND);
            final String fields = StringUtils.trimToEmpty(this.fields.toString());
            final String where = StringUtils.trimToEmpty(this.where.toString());
            sql.append("UPDATE ").append(this.table).append(SPACE);
            if (StringUtils.isNotBlank(fields)) {
                sql.append("SET ").append(fields).append(SPACE);
            }
            if (StringUtils.isNotBlank(where)) {
                sql.append("WHERE ").append(where).append(SPACE);
            }
            return StringUtils.trimToEmpty(sql.toString());
        }

        /**
         * 获取参数
         */
        public Map<String, Object> getParams() {
            return paramMap;
        }

        /**
         * 增加参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public UpdateBuilder addParam(String name, Object value, boolean bool) {
            if (bool) {
                paramMap.put(name, value);
            }
            return this;
        }

        /**
         * 增加参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public UpdateBuilder addParam(String name, Object value) {
            return addParam(name, value, true);
        }

        /**
         * 增加参数
         */
        public UpdateBuilder addParams(Map<String, Object> params, boolean bool) {
            if (bool && params != null && !params.isEmpty()) {
                paramMap.putAll(params);
            }
            return this;
        }

        /**
         * 增加参数
         */
        public UpdateBuilder addParams(Map<String, Object> params) {
            return addParams(params, true);
        }

        /**
         * 设置更新的表
         */
        public UpdateBuilder setTable(String table, boolean bool) {
            if (bool) {
                this.table = table;
            }
            return this;
        }

        /**
         * 设置更新的表
         */
        public UpdateBuilder setTable(String table) {
            return setTable(table, true);
        }

        /**
         * 重新设置更新的字段
         *
         * @param fields 需要更新的字段值 field=:fieldValue
         */
        public UpdateBuilder setFields(String fields, boolean bool) {
            if (bool) {
                this.fields.delete(0, this.fields.length());
                this.fields.append(fields);
                delLast(this.fields, COMMA);
                this.fields.append(COMMA);
            }
            return this;
        }

        /**
         * 重新设置更新的字段
         *
         * @param fields 需要更新的字段值 field=:fieldValue
         */
        public UpdateBuilder setFields(String fields) {
            return setFields(fields, true);
        }

        /**
         * 重新设置更新的字段以及字段值
         *
         * @param fields       需要更新的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public UpdateBuilder setFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename, boolean bool) {
            if (bool && fields != null && !fields.isEmpty()) {
                TupleTwo<String, Map<String, Object>> TupleTwo = getUpdateSql(fields, paramsRename);
                this.fields.delete(0, this.fields.length());
                this.fields.append(TupleTwo.getValue1());
                delLast(this.fields, COMMA);
                this.fields.append(COMMA);
                this.paramMap.putAll(TupleTwo.getValue2());
            }
            return this;
        }

        /**
         * 重新设置更新的字段以及字段值
         *
         * @param fields       需要更新的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public UpdateBuilder setFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename) {
            return setFieldsAndValues(fields, paramsRename, true);
        }

        /**
         * 重新设置更新的字段以及字段值
         *
         * @param fields 需要更新的字段值
         */
        public UpdateBuilder setFieldsAndValues(Map<String, Object> fields) {
            return setFieldsAndValues(fields, DEFAULT_PARAMS_RENAME, true);
        }

        /**
         * 新增更新的字段
         *
         * @param field  字段 field=:fieldValue
         * @param params 参数
         */
        public UpdateBuilder addField(String field, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                this.fields.append(SPACE).append(field);
                delLast(this.fields, COMMA);
                this.fields.append(COMMA);
            }
            return this;
        }

        /**
         * 新增更新的字段
         *
         * @param field      字段 field=:fieldValue
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public UpdateBuilder addField(String field, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addField(field, params);
        }

        /**
         * 新增更新的字段
         *
         * @param field  字段 field=:fieldValue
         * @param params 参数
         */
        public UpdateBuilder addField(String field, Map<String, Object> params) {
            return addField(field, params, true);
        }

        /**
         * 新增更新的字段
         *
         * @param field      字段 field=:fieldValue
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public UpdateBuilder addField(String field, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addField(field);
        }

        /**
         * 新增更新的字段
         *
         * @param field 字段 field=:fieldValue
         */
        public UpdateBuilder addField(String field) {
            return addField(field, null, true);
        }

        /**
         * 新增更新的字段以及字段值
         *
         * @param fields       需要更新的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public UpdateBuilder addFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename, boolean bool) {
            if (bool && fields != null && !fields.isEmpty()) {
                TupleTwo<String, Map<String, Object>> TupleTwo = getUpdateSql(fields, paramsRename);
                this.fields.append(SPACE).append(TupleTwo.getValue1());
                delLast(this.fields, COMMA);
                this.fields.append(COMMA);
                this.paramMap.putAll(TupleTwo.getValue2());
            }
            return this;
        }

        /**
         * 新增更新的字段以及字段值
         *
         * @param fields       需要更新的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public UpdateBuilder addFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename) {
            return addFieldsAndValues(fields, paramsRename, true);
        }

        /**
         * 新增更新的字段以及字段值
         *
         * @param fields 需要更新的字段值
         */
        public UpdateBuilder addFieldsAndValues(Map<String, Object> fields) {
            return addFieldsAndValues(fields, DEFAULT_PARAMS_RENAME, true);
        }

        /**
         * 重新设置更新条件
         *
         * @param condition 更新条件
         * @param params    更新参数
         */
        public UpdateBuilder setWhere(String condition, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                where.delete(0, where.length());
                where.append(condition);
                delLast(where, WHERE_AND);
                where.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 重新设置更新条件
         *
         * @param condition  更新条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public UpdateBuilder setWhere(String condition, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setWhere(condition, params, bool);
        }

        /**
         * 重新设置更新条件
         *
         * @param condition 更新条件
         * @param params    更新参数
         */
        public UpdateBuilder setWhere(String condition, Map<String, Object> params) {
            return setWhere(condition, params, true);
        }

        /**
         * 重新设置更新条件
         *
         * @param condition  更新条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public UpdateBuilder setWhere(String condition, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setWhere(condition, params);
        }

        /**
         * 重新设置更新条件
         *
         * @param condition 更新条件
         */
        public UpdateBuilder setWhere(String condition) {
            return setWhere(condition, null, true);
        }

        /**
         * 增加更新条件
         *
         * @param condition 更新条件
         * @param params    更新参数
         */
        public UpdateBuilder addWhere(String condition, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                where.append(SPACE).append(condition);
                delLast(where, WHERE_AND);
                where.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 增加更新条件
         *
         * @param condition  更新条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public UpdateBuilder addWhere(String condition, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addWhere(condition, params, bool);
        }

        /**
         * 增加更新条件
         *
         * @param condition 更新条件
         * @param params    更新参数
         */
        public UpdateBuilder addWhere(String condition, Map<String, Object> params) {
            return addWhere(condition, params, true);
        }

        /**
         * 增加更新条件
         *
         * @param condition  更新条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public UpdateBuilder addWhere(String condition, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addWhere(condition, params);
        }

        /**
         * 增加更新条件
         *
         * @param condition 更新条件
         */
        public UpdateBuilder addWhere(String condition) {
            return addWhere(condition, null, true);
        }
    }

    /**
     * Insert SQL构建器
     */
    public static class InsertBuilder {
        /**
         * 插入语句
         */
        private String insertSql = "INSERT INTO";
        /**
         * 更新表
         */
        private String table = "";
        /**
         * 新增字段以及参数
         */
        private final Map<String, Object> fields = new LinkedHashMap<>();

        private InsertBuilder() {
        }

        /**
         * 获取 sql
         */
        public String buildSql() {
            // INSERT INTO <table> (<fields.keys>) VALUES (<fields.values>)
            StringBuilder sql = new StringBuilder();
            final String insertSql = StringUtils.trimToEmpty(this.insertSql);
            if (StringUtils.isNotBlank(insertSql)) {
                sql.append(insertSql).append(SPACE);
            }
            final String table = StringUtils.trimToEmpty(this.table);
            if (StringUtils.isNotBlank(table)) {
                sql.append(table).append(SPACE);
            }
            if (!fields.isEmpty()) {
                sql.append("(");
                int index = 0;
                for (Map.Entry<String, ?> field : fields.entrySet()) {
                    String fieldName = field.getKey();
                    if (index != 0) {
                        sql.append(COMMA).append(SPACE);
                    }
                    sql.append(fieldName);
                    index++;
                }
                sql.append(") VALUES (");
                index = 0;
                for (Map.Entry<String, ?> field : fields.entrySet()) {
                    String fieldName = field.getKey();
                    if (index != 0) {
                        sql.append(COMMA).append(SPACE);
                    }
                    sql.append(":").append(fieldName);
                    index++;
                }
                sql.append(")");
            }
            return StringUtils.trimToEmpty(sql.toString());
        }

        /**
         * 获取参数
         */
        public Map<String, Object> getParams() {
            return fields;
        }

        /**
         * 设置插入语句，默认是"INSERT INTO"，可改为"REPLACE INTO"
         */
        public InsertBuilder setInsertSql(String insertSql, boolean bool) {
            if (bool) {
                this.insertSql = insertSql;
            }
            return this;
        }

        /**
         * 设置插入语句，默认是"INSERT INTO"，可改为"REPLACE INTO"
         */
        public InsertBuilder setInsertSql(String insertSql) {
            return setInsertSql(insertSql, true);
        }

        /**
         * 设置插入的表
         */
        public InsertBuilder setTable(String table, boolean bool) {
            if (bool) {
                this.table = table;
            }
            return this;
        }

        /**
         * 设置插入的表
         */
        public InsertBuilder setTable(String table) {
            return setTable(table, true);
        }

        /**
         * 重新设置插入的字段以及字段值
         *
         * @param fields       需要插入的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public InsertBuilder setFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename, boolean bool) {
            if (bool && fields != null && !fields.isEmpty()) {
                this.fields.clear();
                fields.forEach((key, value) -> this.fields.put(NamingUtils.rename(key, paramsRename), value));
            }
            return this;
        }

        /**
         * 重新设置插入的字段以及字段值
         *
         * @param fields       需要插入的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public InsertBuilder setFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename) {
            return setFieldsAndValues(fields, paramsRename, true);
        }

        /**
         * 重新设置插入的字段以及字段值
         *
         * @param fields 需要插入的字段值
         */
        public InsertBuilder setFieldsAndValues(Map<String, Object> fields) {
            return setFieldsAndValues(fields, DEFAULT_PARAMS_RENAME, true);
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param fields       需要插入的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public InsertBuilder addFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename, boolean bool) {
            if (bool && fields != null && !fields.isEmpty()) {
                fields.forEach((key, value) -> this.fields.put(NamingUtils.rename(key, paramsRename), value));
            }
            return this;
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param fields       需要插入的字段值
         * @param paramsRename fields字段名重命名策略
         */
        public InsertBuilder addFieldsAndValues(Map<String, Object> fields, RenameStrategy paramsRename) {
            return addFieldsAndValues(fields, paramsRename, true);
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param fields 需要插入的字段值
         */
        public InsertBuilder addFieldsAndValues(Map<String, Object> fields) {
            return addFieldsAndValues(fields, DEFAULT_PARAMS_RENAME, true);
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param name  字段名
         * @param value 字段值
         */
        public InsertBuilder addFieldAndValue(String name, Object value, RenameStrategy paramsRename, boolean bool) {
            if (bool) {
                this.fields.put(NamingUtils.rename(name, paramsRename), value);
            }
            return this;
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param name  字段名
         * @param value 字段值
         */
        public InsertBuilder addFieldAndValue(String name, Object value, RenameStrategy paramsRename) {
            return addFieldAndValue(name, value, paramsRename, true);
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param name  字段名
         * @param value 字段值
         */
        public InsertBuilder addFieldAndValue(String name, Object value, boolean bool) {
            return addFieldAndValue(name, value, DEFAULT_PARAMS_RENAME, bool);
        }

        /**
         * 增加插入的字段以及字段值
         *
         * @param name  字段名
         * @param value 字段值
         */
        public InsertBuilder addFieldAndValue(String name, Object value) {
            return addFieldAndValue(name, value, DEFAULT_PARAMS_RENAME, true);
        }
    }

    /**
     * Delete SQL构建器
     */
    public static class DeleteBuilder {
        /**
         * sql参数
         */
        private final Map<String, Object> paramMap = new HashMap<>();
        /**
         * 删除表
         */
        private String table = "";
        /**
         * 更新条件
         */
        private final StringBuilder where = new StringBuilder();

        private DeleteBuilder() {
        }

        /**
         * 获取 sql
         */
        public String buildSql() {
            // DELETE FROM <table> WHERE <where>
            StringBuilder sql = new StringBuilder();
            delLast(this.where, WHERE_AND);
            final String table = StringUtils.trimToEmpty(this.table);
            final String where = StringUtils.trimToEmpty(this.where.toString());
            if (StringUtils.isNotBlank(table)) {
                sql.append("DELETE FROM ").append(table).append(SPACE);
            }
            if (StringUtils.isNotBlank(where)) {
                sql.append("WHERE ").append(where).append(SPACE);
            }
            return StringUtils.trimToEmpty(sql.toString());
        }

        /**
         * 获取参数
         */
        public Map<String, Object> getParams() {
            return paramMap;
        }

        /**
         * 增加参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public DeleteBuilder addParam(String name, Object value, boolean bool) {
            if (bool) {
                paramMap.put(name, value);
            }
            return this;
        }

        /**
         * 增加参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public DeleteBuilder addParam(String name, Object value) {
            return addParam(name, value, true);
        }

        /**
         * 增加参数
         */
        public DeleteBuilder addParams(Map<String, Object> params, boolean bool) {
            if (bool && params != null && !params.isEmpty()) {
                paramMap.putAll(params);
            }
            return this;
        }

        /**
         * 增加参数
         */
        public DeleteBuilder addParams(Map<String, Object> params) {
            return addParams(params, true);
        }

        /**
         * 设置删除的表
         */
        public DeleteBuilder setTable(String table, boolean bool) {
            if (bool) {
                this.table = table;
            }
            return this;
        }

        /**
         * 设置删除的表
         */
        public DeleteBuilder setTable(String table) {
            return setTable(table, true);
        }

        /**
         * 重新设置删除条件
         *
         * @param condition 删除条件
         * @param params    删除参数
         */
        public DeleteBuilder setWhere(String condition, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                where.delete(0, where.length());
                where.append(condition);
                delLast(where, WHERE_AND);
                where.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 重新设置删除条件
         *
         * @param condition  删除条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public DeleteBuilder setWhere(String condition, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setWhere(condition, params, bool);
        }

        /**
         * 重新设置删除条件
         *
         * @param condition 删除条件
         * @param params    删除参数
         */
        public DeleteBuilder setWhere(String condition, Map<String, Object> params) {
            return setWhere(condition, params, true);
        }

        /**
         * 重新设置删除条件
         *
         * @param condition  删除条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public DeleteBuilder setWhere(String condition, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return setWhere(condition, params);
        }

        /**
         * 重新设置删除条件
         *
         * @param condition 删除条件
         */
        public DeleteBuilder setWhere(String condition) {
            return setWhere(condition, null, true);
        }

        /**
         * 增加删除条件
         *
         * @param condition 删除条件
         * @param params    删除参数
         */
        public DeleteBuilder addWhere(String condition, Map<String, Object> params, boolean bool) {
            if (bool) {
                if (params != null && !params.isEmpty()) {
                    paramMap.putAll(params);
                }
                where.append(SPACE).append(condition);
                delLast(where, WHERE_AND);
                where.append(SPACE).append(WHERE_AND);
            }
            return this;
        }

        /**
         * 增加删除条件
         *
         * @param condition  删除条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public DeleteBuilder addWhere(String condition, String paramName, Object paramValue, boolean bool) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addWhere(condition, params, bool);
        }

        /**
         * 增加删除条件
         *
         * @param condition 删除条件
         * @param params    删除参数
         */
        public DeleteBuilder addWhere(String condition, Map<String, Object> params) {
            return addWhere(condition, params, true);
        }

        /**
         * 增加删除条件
         *
         * @param condition  删除条件
         * @param paramName  参数名
         * @param paramValue 参数值
         */
        public DeleteBuilder addWhere(String condition, String paramName, Object paramValue) {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, paramValue);
            return addWhere(condition, params);
        }

        /**
         * 增加删除条件
         *
         * @param condition 删除条件
         */
        public DeleteBuilder addWhere(String condition) {
            return addWhere(condition, null, true);
        }
    }
}
