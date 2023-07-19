package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.beans.BeanUtils;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Column;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 10:15 <br/>
 */
public abstract class AbstractMetaData implements DataBaseMetaData {
    protected final Jdbc jdbc;

    /**
     * 忽略Schema名(忽略大小写过滤)
     */
    private final Set<String> ignoreSchemas = new HashSet<>();
    /**
     * 忽略表名(忽略大小写过滤)
     */
    private final Set<String> ignoreTables = new HashSet<>();
    /**
     * 忽略表前缀(忽略大小写过滤)
     */
    private final Set<String> ignoreTablesPrefix = new HashSet<>();
    /**
     * 忽略表后缀(忽略大小写过滤)
     */
    private final Set<String> ignoreTablesSuffix = new HashSet<>();

    protected AbstractMetaData(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 忽略Schema名(忽略大小写过滤)
     */
    public void addIgnoreSchema(String schemaName) {
        if (StringUtils.isBlank(schemaName)) {
            return;
        }
        ignoreSchemas.add(schemaName);
    }

    /**
     * 忽略表名(忽略大小写过滤)
     */
    public void addIgnoreTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        ignoreTables.add(tableName);
    }

    /**
     * 忽略表前缀(忽略大小写过滤)
     */
    public void addIgnoreTablePrefix(String tablePrefix) {
        if (StringUtils.isBlank(tablePrefix)) {
            return;
        }
        ignoreTablesPrefix.add(tablePrefix);
    }

    /**
     * 忽略表后缀(忽略大小写过滤)
     */
    public void addIgnoreTableSuffix(String tableSuffix) {
        if (StringUtils.isBlank(tableSuffix)) {
            return;
        }
        ignoreTablesSuffix.add(tableSuffix);
    }

    /**
     * 忽略Schema名(忽略大小写过滤)
     */
    public Set<String> getIgnoreSchemas() {
        return ignoreSchemas.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表名(忽略大小写过滤)
     */
    public Set<String> getIgnoreTables() {
        return ignoreTables.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表前缀(忽略大小写过滤)
     */
    public Set<String> getIgnoreTablesPrefix() {
        return ignoreTablesPrefix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表后缀(忽略大小写过滤)
     */
    public Set<String> getIgnoreTablesSuffix() {
        return ignoreTablesSuffix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    @Override
    public Jdbc getJdbc() {
        return jdbc;
    }

    @Override
    public List<Schema> getSchemas(Collection<String> schemasName, Collection<String> tablesName) {
        if (schemasName == null) {
            schemasName = new HashSet<>();
        }
        if (tablesName == null) {
            tablesName = new HashSet<>();
        }
        schemasName = schemasName.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
        tablesName = tablesName.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
        // 过滤 ignoreSchemas ignoreTables
        final Set<String> ignoreSchemas = getIgnoreSchemas();
        final Set<String> ignoreTables = getIgnoreTables();
        // 过滤 ignoreTablesPrefix ignoreTablesSuffix
        final Set<String> ignoreTablesPrefix = getIgnoreTablesPrefix();
        final Set<String> ignoreTablesSuffix = getIgnoreTablesSuffix();
        return doGetSchemas(schemasName, tablesName, ignoreSchemas, ignoreTables, ignoreTablesPrefix, ignoreTablesSuffix);
    }

    protected abstract List<Schema> doGetSchemas(Collection<String> schemasName,
                                                 Collection<String> tablesName,
                                                 Set<String> ignoreSchemas,
                                                 Set<String> ignoreTables,
                                                 Set<String> ignoreTablesPrefix,
                                                 Set<String> ignoreTablesSuffix);

    @Override
    public List<Schema> getSchemas() {
        return getSchemas(null, null);
    }

    @Override
    public Schema getSchema(String schemaName) {
        List<Schema> schemas = getSchemas(Collections.singletonList(schemaName), null);
        return schemas.isEmpty() ? null : schemas.get(0);
    }

    @Override
    public Schema getSchema() {
        String currentSchema = currentSchema();
        List<Schema> schemas = getSchemas(Collections.singletonList(currentSchema), null);
        return schemas.isEmpty() ? null : schemas.get(0);
    }

    @Override
    public Table getTable(String schemaName, String tableName) {
        List<Schema> schemas = getSchemas(Collections.singletonList(schemaName), Collections.singletonList(tableName));
        if (schemas.isEmpty()) {
            return null;
        }
        Schema schema = schemas.get(0);
        return schema.getTables().isEmpty() ? null : schema.getTables().get(0);
    }

    /**
     * 生成 where in 条件 sql “f in ( whereIn )” 中的 whereIn 片段
     */
    protected String createWhereIn(Map<String, Object> params, Collection<?> values) {
        StringBuilder whereIn = new StringBuilder();
        int idx = 0;
        for (Object value : values) {
            if (idx > 0) {
                whereIn.append(", ");
            }
            idx++;
            String paramName = "param_" + params.size();
            whereIn.append(":").append(paramName);
            params.put(paramName, value);
        }
        return whereIn.toString();
    }

    /**
     * 对 Schema 集合进行深度排序
     */
    protected void sort(List<Schema> schemas) {
        schemas.sort(Comparator.comparing(Schema::getName));
        for (Schema schema : schemas) {
            List<Table> tables = schema.getTables();
            tables.sort(Comparator.comparing(Table::getName));
            for (Table table : tables) {
                List<Column> columns = table.getColumns();
                columns.sort(Comparator.comparing(Column::getOrdinalPosition));
            }
        }
    }

    /**
     * 数据库字段类型映射
     *
     * @param column   源数据库字段
     * @param targetDb 目标数据库类型
     */
    protected Column columnTypeMapping(Column column, DbType targetDb) {
        DbType dbType = column.getTable().getSchema().getDbType();
        if (dbType == null || targetDb == null || Objects.equals(dbType, targetDb)) {
            return column;
        }
        Column newColumn = new Column(column.getTable());
        BeanUtils.copyProperties(column, newColumn);
        // TODO 不同数据库需要做类型映射 dataType size width decimalDigits
        return newColumn;
    }

    /**
     * 数据库字段默认值映射
     *
     * @param column   源数据库字段
     * @param targetDb 目标数据库类型
     */
    protected String defaultValueMapping(Column column, DbType targetDb) {
        DbType dbType = column.getTable().getSchema().getDbType();
        if (dbType == null || targetDb == null || Objects.equals(dbType, targetDb)) {
            return StringUtils.trim(column.getDefaultValue());
        }
        // TODO 不同数据库需要做映射 defaultValue
        return StringUtils.trim(column.getDefaultValue());
    }
}
