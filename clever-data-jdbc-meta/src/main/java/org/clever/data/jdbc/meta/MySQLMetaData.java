package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取数据库元数据 MySQL 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 22:58 <br/>
 */
public class MySQLMetaData extends AbstractMetaData {
    private final Jdbc jdbc;

    public MySQLMetaData(Jdbc jdbc) {
        this.jdbc = jdbc;
        addIgnoreSchema("information_schema");
        addIgnoreSchema("mysql");
        addIgnoreSchema("performance_schema");
        addIgnoreSchema("sys");
    }

    @Override
    public String currentSchema() {
        return StringUtils.lowerCase(jdbc.queryString("select database() from dual"));
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
        // 所有的 Schema | Map<schemaName, Schema>
        final Map<String, Schema> mapSchema = new HashMap<>();
        // 查询字段信息
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder sql = new StringBuilder();
        sql.append("select");
        sql.append("    table_schema             as `schemaName`, ");
        sql.append("    table_name               as `tableName`, ");
        sql.append("    column_name              as `columnName`, ");
        sql.append("    column_comment           as `columnComment`, ");
        sql.append("    column_key               as `partOfPrimaryKey`, ");
        // partOfIndex
        // partOfUniqueIndex
        // autoIncremented
        sql.append("    is_nullable              as `notNull`, ");
        sql.append("    data_type                as `dataType`, ");
        sql.append("    numeric_precision        as `size`, ");
        sql.append("    numeric_scale            as `decimalDigits`, ");
        sql.append("    character_maximum_length as `width`, ");
        sql.append("    column_default           as `defaultValue`, ");
        sql.append("    ordinal_position         as `ordinalPosition`, ");
        sql.append("    column_type              as `width`, ");
        sql.append("    extra                    as `extra` ");
        sql.append("from ");
        sql.append("    information_schema.columns ");
        sql.append("where 1=1 ");
        addWhere(sql, params, schemasName, tablesName, ignoreSchemas, ignoreTables);
        sql.append("order by table_schema, table_name, ordinal_position");
        List<Map<String, Object>> mapColumns = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapColumns) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            if (ignoreTablesPrefix.stream().anyMatch(schemaName::startsWith)) {
                continue;
            }
            if (ignoreTablesSuffix.stream().anyMatch(schemaName::endsWith)) {
                continue;
            }
            Schema schema = mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.MYSQL));
            Table table = schema.getTable(tableName);
            if (table == null) {
                table = new Table(schema);
                table.setName(tableName);
                schema.addTable(table);
            }
            Column column = new Column(table);
            fillColumn(column, map);
            table.addColumn(column);
        }
        // 查询表信息
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    table_schema        as `schemaName`, ");
        sql.append("    table_name          as `tableName`, ");
        sql.append("    table_comment       as `comment` ");
        sql.append("from ");
        sql.append("    information_schema.tables ");
        sql.append("where 1=1  ");
        addWhere(sql, params, schemasName, tablesName, ignoreSchemas, ignoreTables);
        List<Map<String, Object>> mapTables = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapTables) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            String comment = Conv.asString(map.get("comment"), null);
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            Table table = schema.getTable(tableName);
            if (table == null) {
                continue;
            }
            table.setComment(comment);
        }
        // 查询主键&索引
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    table_schema as `schemaName`, ");
        sql.append("    table_name   as `tableName`, ");
        sql.append("    index_name   as `name`, ");
        sql.append("    seq_in_index as `order`, ");
        sql.append("    column_name  as `columnName`, ");
        sql.append("    non_unique   as `nonUnique`, ");
        sql.append("    comment      as `comment`, ");
        sql.append("    index_schema as `index_schema`, ");
        sql.append("    collation    as `collation`, ");
        sql.append("    cardinality  as `cardinality`, ");
        sql.append("    sub_part     as `sub_part`, ");
        sql.append("    nullable     as `nullable`, ");
        sql.append("    index_type   as `index_type` ");
        sql.append("from information_schema.statistics ");
        sql.append("where 1=1 ");
        addWhere(sql, params, schemasName, tablesName, ignoreSchemas, ignoreTables);
        sql.append("order by table_schema, table_name, index_name, seq_in_index, column_name");
        List<Map<String, Object>> mapStatistics = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapStatistics) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            String columnName = Conv.asString(map.get("columnName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            Table table = schema.getTable(tableName);
            if (table == null) {
                continue;
            }
            Column column = table.getColumn(columnName);
            if (column == null) {
                continue;
            }
            // 主键的索引名称是 “PRIMARY”
            if ("PRIMARY".equalsIgnoreCase(name)) {
                PrimaryKey primaryKey = table.getPrimaryKey();
                if (primaryKey == null) {
                    primaryKey = new PrimaryKey(table);
                    primaryKey.setName(name);
                    primaryKey.getAttributes().putAll(map);
                    table.setPrimaryKey(primaryKey);
                }
                primaryKey.addColumn(column);
            } else {
                Index index = table.getIndex(name);
                if (index == null) {
                    boolean unique = !Conv.asBoolean(map.get("nonUnique"));
                    index = new Index(table);
                    index.setName(name);
                    index.setUnique(unique);
                    index.getAttributes().putAll(map);
                    table.addIndex(index);
                }
                index.addColumn(column);
            }
        }
        // 返回数据
        List<Schema> result = new ArrayList<>(mapSchema.values());
        sort(result);
        return result;
    }

    private void addWhere(StringBuilder sql,
                          Map<String, Object> params,
                          Collection<String> schemasName,
                          Collection<String> tablesName,
                          Collection<String> ignoreSchemas,
                          Collection<String> ignoreTables) {
        if (!schemasName.isEmpty()) {
            sql.append("and lower(table_schema) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(table_schema) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
    }

    private void fillColumn(Column column, Map<String, Object> map) {
        column.setName(Conv.asString(map.get("columnName"), null));
        column.setComment(Conv.asString(map.get("columnComment"), null));
        // COLUMN_KEY 可以有以下取值：
        //   PRI: 表示该列是主键
        //   UNI: 表示该列具有唯一性约束
        //   MUL: 表示该列是非唯一性索引的一部分
        //   ''（空字符串）：表示该列不具有任何约束
        switch (Conv.asString(map.get("partOfPrimaryKey")).toUpperCase()) {
            case "PRI":
                column.setPartOfPrimaryKey(true);
                column.setPartOfIndex(true);
                column.setPartOfUniqueIndex(true);
                break;
            case "UNI":
                column.setPartOfIndex(true);
                column.setPartOfUniqueIndex(true);
                break;
            case "MUL":
                column.setPartOfIndex(true);
                break;
        }
        // EXTRA 可以有以下取值：
        //   auto_increment: 表示该列是自动递增的
        //   DEFAULT_GENERATED: 表示该列使用了生成的默认值（如序列或自增字段）
        //   VIRTUAL GENERATED: 表示该列是虚拟生成的
        //   STORED GENERATED: 表示该列是存储生成的
        //   ''（空字符串）：表示该列没有任何特殊属性
        if (Conv.asString(map.get("extra")).toUpperCase().contains("AUTO_INCREMENT")) {
            column.setAutoIncremented(true);
        }
        column.setNotNull(Conv.asBoolean(map.get("notNull")));
        column.setDataType(Conv.asString(map.get("dataType"), null));
        column.setSize(Conv.asInteger(map.get("size")));
        column.setDecimalDigits(Conv.asInteger(map.get("decimalDigits")));
        column.setWidth(Conv.asInteger(map.get("width")));
        column.setDefaultValue(Conv.asString(map.get("defaultValue"), null));
        column.setOrdinalPosition(Conv.asInteger(map.get("ordinalPosition")));
        column.setAttribute("extra", map.get("extra"));
    }
}
