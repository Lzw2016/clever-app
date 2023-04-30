package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;

import java.util.*;

/**
 * 获取数据库元数据 MySQL 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 22:58 <br/>
 */
@SuppressWarnings("DuplicatedCode")
public class MySQLMetaData extends AbstractMetaData {
    public MySQLMetaData(Jdbc jdbc) {
        super(jdbc);
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
    protected List<Schema> doGetSchemas(Collection<String> schemasName,
                                        Collection<String> tablesName,
                                        Set<String> ignoreSchemas,
                                        Set<String> ignoreTables,
                                        Set<String> ignoreTablesPrefix,
                                        Set<String> ignoreTablesSuffix) {
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
        sql.append("    column_type              as `column_type`, ");
        sql.append("    datetime_precision       as `datetime_precision`, ");
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
            if (ignoreTablesPrefix.stream().anyMatch(tableName::startsWith)) {
                continue;
            }
            if (ignoreTablesSuffix.stream().anyMatch(tableName::endsWith)) {
                continue;
            }
            Schema schema = mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.MYSQL, name));
            Table table = schema.getTable(tableName);
            if (table == null) {
                table = new Table(schema);
                table.setName(tableName);
                schema.addTable(table);
            }
            Column column = new Column(table);
            fillColumn(column, map);
            column.getAttributes().putAll(map);
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
            }
        }
        // 查询存储过程&函数 -> 用户必须要有“数据库服务器select权限(navicat授权)”才能查询到 routine_definition 字段
        // grant select on *.* to `admin`@`%`;
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    routine_schema      as `schemaName`, ");
        sql.append("    routine_name        as `name`, ");
        sql.append("    routine_type        as `type`, ");
        sql.append("    routine_definition  as `definition`, ");
        sql.append("    routine_body        as `body`, ");
        sql.append("    specific_name       as `specific_name`, ");
        sql.append("    external_name       as `external_name`, ");
        sql.append("    external_language   as `external_language`, ");
        sql.append("    parameter_style     as `parameter_style`, ");
        sql.append("    routine_comment     as `routine_comment` ");
        sql.append("from information_schema.routines ");
        sql.append("where lower(routine_type) in ('procedure','function') ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(routine_schema) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(routine_schema) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by routine_schema, type, routine_name");
        List<Map<String, Object>> mapRoutines = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapRoutines) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            String type = Conv.asString(map.get("type")).toLowerCase();
            String definition = Conv.asString(map.get("definition"));
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            Procedure procedure = new Procedure(schema);
            procedure.setName(name);
            // ROUTINE_TYPE 可能的值包括：
            //   PROCEDURE 表示存储过程
            //   FUNCTION 表示函数
            procedure.setFunction("FUNCTION".equalsIgnoreCase(type));
            procedure.setDefinition(definition);
            procedure.getAttributes().putAll(map);
            schema.addProcedure(procedure);
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
        column.setNotNull(!Conv.asBoolean(map.get("notNull"), true));
        column.setDataType(Conv.asString(map.get("dataType"), null));
        column.setSize(Conv.asInteger(map.get("size")));
        if (column.getSize() == 0) {
            column.setSize(Conv.asInteger(map.get("datetime_precision")));
        }
        column.setDecimalDigits(Conv.asInteger(map.get("decimalDigits")));
        column.setWidth(Conv.asInteger(map.get("width")));
        column.setDefaultValue(Conv.asString(map.get("defaultValue"), null));
        column.setOrdinalPosition(Conv.asInteger(map.get("ordinalPosition")));
    }
}
