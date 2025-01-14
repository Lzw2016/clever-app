package org.clever.data.jdbc.meta;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.RenameStrategy;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.inner.ColumnTypeMapping;
import org.clever.data.jdbc.meta.inner.DefaultValueMapping;
import org.clever.data.jdbc.meta.model.*;
import org.clever.data.jdbc.support.DbColumnMetaData;
import org.clever.data.jdbc.support.sqlparser.GlobalSqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 10:15 <br/>
 */
@SuppressWarnings("DuplicatedCode")
public abstract class AbstractMetaData implements DataBaseMetaData {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
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
        final Set<String> ignoreSchemas = new HashSet<>(getIgnoreSchemas());
        final Set<String> ignoreTables = new HashSet<>(getIgnoreTables());
        ignoreSchemas.removeAll(schemasName);
        ignoreTables.removeAll(tablesName);
        // 过滤 ignoreTablesPrefix ignoreTablesSuffix
        final Set<String> ignoreTablesPrefix = getIgnoreTablesPrefix();
        final Set<String> ignoreTablesSuffix = getIgnoreTablesSuffix();
        return doGetSchemas(schemasName, tablesName, ignoreSchemas, ignoreTables, ignoreTablesPrefix, ignoreTablesSuffix);
    }

    protected abstract List<Schema> doGetSchemas(Collection<String> schemasName, Collection<String> tablesName, Set<String> ignoreSchemas, Set<String> ignoreTables, Set<String> ignoreTablesPrefix, Set<String> ignoreTablesSuffix);

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
    public Schema getSchema(String schemaName, Collection<String> tablesName) {
        List<Schema> schemas = getSchemas(Collections.singletonList(schemaName), tablesName);
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
     * 表和字段的备注转义处理
     */
    protected String toComment(String comment) {
        comment = StringUtils.trimToEmpty(comment);
        return StringUtils.replace(comment, "'", "''");
    }

    protected abstract String toLiteral(String objName);

    protected String columnType(Column column) {
        String dataType = StringUtils.lowerCase(column.getDataType());
        StringBuilder type = new StringBuilder();
        if (dataType.contains("time") || dataType.contains("date")) {
            type.append(dataType);
        } else if (dataType.contains("char") || column.getSize() <= 0) {
            type.append(dataType);
            if (column.getWidth() > 0) {
                type.append("(").append(column.getWidth()).append(")");
            } else if (column.getSize() > 0) {
                type.append("(").append(column.getSize()).append(")");
            }
        } else {
            type.append(dataType);
            if (column.getSize() > 0) {
                type.append("(").append(column.getSize());
                if (column.getDecimalDigits() > 0) {
                    type.append(", ").append(column.getDecimalDigits());
                }
                type.append(")");
            }
        }
        return type.toString();
    }

    /**
     * 生产修改表的部分sql: 字段变化、主键变化、索引变化
     */
    protected String doAlterTable(Table newTable, Table oldTable) {
        final StringBuilder ddl = new StringBuilder();
        // 字段变化
        final List<Column> newColumns = getNewColumns(newTable, oldTable);
        final List<Column> delColumns = getDelColumns(newTable, oldTable);
        final List<TupleTwo<Column, Column>> diffColumns = getDiffColumns(newTable, oldTable);
        for (Column column : newColumns) {
            ddl.append(createColumn(column));
        }
        for (Column column : delColumns) {
            ddl.append(dropColumn(column));
        }
        for (TupleTwo<Column, Column> tuple : diffColumns) {
            Column newColumn = tuple.getValue1();
            Column oldColumn = tuple.getValue2();
            ddl.append(alterColumn(newColumn, oldColumn));
        }
        // 主键变化
        if (isPrimaryKeyChange(newTable.getPrimaryKey(), oldTable.getPrimaryKey())) {
            ddl.append(alterPrimaryKey(newTable.getPrimaryKey(), oldTable.getPrimaryKey()));
        }
        // 索引变化
        final List<Index> newIndices = getNewIndices(newTable, oldTable);
        final List<Index> delIndices = getDelIndices(newTable, oldTable);
        final List<TupleTwo<Index, Index>> diffIndices = getDiffIndices(newTable, oldTable);
        for (Index index : newIndices) {
            ddl.append(createIndex(index));
        }
        for (Index index : delIndices) {
            ddl.append(dropIndex(index));
        }
        for (TupleTwo<Index, Index> tuple : diffIndices) {
            Index newIndex = tuple.getValue1();
            Index oldIndex = tuple.getValue2();
            ddl.append(alterIndex(newIndex, oldIndex));
        }
        return ddl.toString();
    }

    /**
     * 获取需要新增的字段
     */
    protected List<Column> getNewColumns(Table newTable, Table oldTable) {
        final List<Column> newColumns = new ArrayList<>();
        for (Column newColumn : newTable.getColumns()) {
            if (oldTable.getColumn(newColumn.getName()) == null) {
                newColumns.add(newColumn);
            }
        }
        return newColumns;
    }

    /**
     * 获取需要删除的字段
     */
    protected List<Column> getDelColumns(Table newTable, Table oldTable) {
        final List<Column> delColumns = new ArrayList<>();
        for (Column oldColumn : oldTable.getColumns()) {
            if (newTable.getColumn(oldColumn.getName()) == null) {
                delColumns.add(oldColumn);
            }
        }
        return delColumns;
    }

    /**
     * 获取需要变化的字段
     *
     * @return {@code List<TupleTwo<newColumn, oldColumn>>}
     */
    protected List<TupleTwo<Column, Column>> getDiffColumns(Table newTable, Table oldTable) {
        final List<TupleTwo<Column, Column>> diffColumns = new ArrayList<>();
        for (Column newColumn : newTable.getColumns()) {
            Column oldColumn = oldTable.getColumn(newColumn.getName());
            if (oldColumn == null) {
                continue;
            }
            // 对比两个字段有无变化
            boolean change = !StringUtils.equalsIgnoreCase(newColumn.getName(), oldColumn.getName())
                || !Objects.equals(newColumn.getComment(), oldColumn.getComment())
                || !Objects.equals(newColumn.isAutoIncremented(), oldColumn.isAutoIncremented())
                || !Objects.equals(newColumn.isNotNull(), oldColumn.isNotNull());
            if (!change) {
                TupleTwo<Boolean, Boolean> typeAndDelValue = equalsColumnTypeAndDelValue(newColumn, oldColumn);
                change = typeAndDelValue.getValue1() || typeAndDelValue.getValue2();
            }
            if (change) {
                diffColumns.add(TupleTwo.creat(newColumn, oldColumn));
            }
        }
        return diffColumns;
    }

    protected TupleTwo<Boolean, Boolean> equalsColumnTypeAndDelValue(Column newColumn, Column oldColumn) {
        boolean typeChange;
        boolean delValueChange = false;
        DbType dbType = (jdbc != null) ? jdbc.getDbType() : null;
        if (dbType == null && oldColumn.getTable() != null && oldColumn.getTable().getSchema() != null) {
            dbType = oldColumn.getTable().getSchema().getDbType();
        }
        if (dbType == null && newColumn.getTable() != null && newColumn.getTable().getSchema() != null) {
            dbType = newColumn.getTable().getSchema().getDbType();
        }
        Column newColumnCopy = columnTypeMapping(newColumn, dbType);
        Column oldColumnCopy = columnTypeMapping(oldColumn, dbType);
        typeChange = !Objects.equals(newColumnCopy.getDataType(), oldColumnCopy.getDataType())
            || !Objects.equals(newColumnCopy.getSize(), oldColumnCopy.getSize())
            || !Objects.equals(newColumnCopy.getDecimalDigits(), oldColumnCopy.getDecimalDigits())
            || !Objects.equals(newColumnCopy.getWidth(), oldColumnCopy.getWidth());
        if (!Objects.equals(newColumn.getDefaultValue(), oldColumn.getDefaultValue())) {
            delValueChange = !Objects.equals(defaultValueMapping(newColumnCopy, dbType), defaultValueMapping(oldColumnCopy, dbType));
        }
        return TupleTwo.creat(typeChange, delValueChange);
    }

    /**
     * 主键是否变化
     */
    protected boolean isPrimaryKeyChange(PrimaryKey newPrimaryKey, PrimaryKey oldPrimaryKey) {
        if (oldPrimaryKey == null && newPrimaryKey == null) {
            return false;
        }
        if (oldPrimaryKey == null || newPrimaryKey == null) {
            return true;
        }
        if (!Objects.equals(newPrimaryKey.getName(), oldPrimaryKey.getName())) {
            return true;
        }
        List<String> newNames = newPrimaryKey.getColumns().stream().map(column -> StringUtils.lowerCase(StringUtils.trim(column.getName()))).collect(Collectors.toList());
        List<String> oldNames = oldPrimaryKey.getColumns().stream().map(column -> StringUtils.lowerCase(StringUtils.trim(column.getName()))).collect(Collectors.toList());
        return !Objects.equals(StringUtils.join(newNames), StringUtils.join(oldNames));
    }

    /**
     * 获取需要新增的索引
     */
    protected List<Index> getNewIndices(Table newTable, Table oldTable) {
        final List<Index> newIndices = new ArrayList<>();
        for (Index newIndex : newTable.getIndices()) {
            if (oldTable.getIndex(newIndex.getName()) == null) {
                newIndices.add(newIndex);
            }
        }
        return newIndices;
    }

    /**
     * 获取需要删除的索引
     */
    protected List<Index> getDelIndices(Table newTable, Table oldTable) {
        final List<Index> delIndices = new ArrayList<>();
        for (Index delIndex : oldTable.getIndices()) {
            if (newTable.getIndex(delIndex.getName()) == null) {
                delIndices.add(delIndex);
            }
        }
        return delIndices;
    }

    /**
     * 获取需要变化的字段
     *
     * @return {@code List<TupleTwo<newIndex, oldIndex>>}
     */
    protected List<TupleTwo<Index, Index>> getDiffIndices(Table newTable, Table oldTable) {
        final List<TupleTwo<Index, Index>> diffIndices = new ArrayList<>();
        for (Index newIndex : newTable.getIndices()) {
            Index oldIndex = oldTable.getIndex(newIndex.getName());
            if (oldIndex == null) {
                continue;
            }
            // 对比两个索引有无变化
            boolean change = !Objects.equals(newIndex.getName(), oldIndex.getName()) || !Objects.equals(newIndex.isUnique(), oldIndex.isUnique());
            if (!change) {
                List<String> newNames = newIndex.getColumns().stream().map(column -> StringUtils.lowerCase(StringUtils.trim(column.getName()))).collect(Collectors.toList());
                List<String> oldNames = oldIndex.getColumns().stream().map(column -> StringUtils.lowerCase(StringUtils.trim(column.getName()))).collect(Collectors.toList());
                change = !Objects.equals(StringUtils.join(newNames), StringUtils.join(oldNames));
            }
            if (change) {
                diffIndices.add(TupleTwo.creat(newIndex, oldIndex));
            }
        }
        return diffIndices;
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
        newColumn.getAttributes().putAll(column.getAttributes());
        // 不同数据库需要做类型映射 dataType size width decimalDigits
        switch (targetDb) {
            case MYSQL:
                ColumnTypeMapping.mysql(newColumn);
                break;
            case ORACLE:
                ColumnTypeMapping.oracle(newColumn);
                break;
            case POSTGRE_SQL:
                ColumnTypeMapping.postgresql(newColumn);
                break;
        }
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
        // 不同数据库需要做映射 defaultValue
        return switch (targetDb) {
            case MYSQL -> DefaultValueMapping.mysql(column);
            case ORACLE -> DefaultValueMapping.oracle(column);
            case POSTGRE_SQL -> DefaultValueMapping.postgresql(column);
            default -> StringUtils.trim(column.getDefaultValue());
        };
    }

    public String updateColumnPosition(Table table) {
        Assert.notNull(table, "参数 newTable 不能为空");
        final Table tmpTable = new Table(table.getSchema());
        BeanUtils.copyProperties(table, tmpTable);
        tmpTable.getAttributes().putAll(table.getAttributes());
        table.getColumns().forEach(tmpTable::addColumn);
        for (Index index : table.getIndices()) {
            Index tmpIndex = new Index(tmpTable);
            BeanUtils.copyProperties(index, tmpIndex);
            index.getColumns().forEach(tmpIndex::addColumn);
            tmpTable.addIndex(tmpIndex);
        }
        tmpTable.setName(table.getName() + "__tmp");
        final StringBuilder sql = new StringBuilder();
        // 创建临时表 | create table sys_lock__tmp
        sql.append(createTable(tmpTable)).append(LINE);
        // 把数据转移到临时表 | insert into sys_lock__tmp(lock_id, lock_name) select lock_id, lock_name from sys_lock;
        sql.append("insert into ").append(toLiteral(tmpTable.getName())).append("(");
        final List<Column> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(toLiteral(column.getName()));
        }
        sql.append(") select ");
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(toLiteral(column.getName()));
        }
        sql.append(" from ").append(toLiteral(table.getName())).append(";").append(LINE).append(LINE);
        // 删除目标表 | drop table sys_lock;
        sql.append(dropTable(table));
        // 修改零时表为目标表 alter table sys_lock__tmp rename to sys_lock;
        sql.append(alterTable(table, tmpTable));
        return sql.toString();
    }

    @Override
    public QueryMetaData queryMetaData(String sql, Map<String, Object> paramMap, RenameStrategy resultRename) {
        // TODO 需要与低代码配合
        List<DbColumnMetaData> list = jdbc.queryMetaData(sql, paramMap, resultRename);
        // sql解析
        try {
            Statement statement = GlobalSqlParser.parse(sql);
            PlainSelect plainSelect;
            if (statement instanceof PlainSelect) {
                plainSelect = (PlainSelect) statement;
            } else if (statement instanceof SetOperationList setOperationList) {
                if (!setOperationList.getSelects().isEmpty()) {
                    Select select = setOperationList.getSelect(0);

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to parse sql:%s", sql), e);
        }
        return null;
    }
}
