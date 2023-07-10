package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;

import java.util.*;

/**
 * 获取数据库元数据 PostgreSQL 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 15:51 <br/>
 */
@SuppressWarnings("DuplicatedCode")
public class PostgreSQLMetaData extends AbstractMetaData {
    public PostgreSQLMetaData(Jdbc jdbc) {
        super(jdbc);
        addIgnoreSchema("information_schema");
        addIgnoreSchema("pg_catalog");
        addIgnoreSchema("pg_toast");
        addIgnoreSchema("pg_global");
        addIgnoreTable("pg_stat_activity");
        addIgnoreTable("pg_tables");
        addIgnoreTable("pg_views");
        addIgnoreTable("pg_indexes");
        addIgnoreTable("pg_attribute");
        addIgnoreTable("pg_type");
        addIgnoreTable("pg_roles");
        addIgnoreTable("pg_namespace");
        addIgnoreTable("pg_constraint");
        addIgnoreTable("pg_database");
        addIgnoreTable("pg_stat_replication");
        addIgnoreTable("pg_locks");
    }

    @Override
    public String currentSchema() {
        return StringUtils.lowerCase(jdbc.queryString("select current_schema()"));
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
        sql.append("select ");
        sql.append("    a.table_schema                                            as schemaName, ");
        sql.append("    a.table_name                                              as tableName, ");
        sql.append("    a.column_name                                             as columnName, ");
        // columnComment
        // partOfPrimaryKey
        // partOfIndex
        // partOfUniqueIndex
        // autoIncremented
        sql.append("    a.is_nullable                                             as notNull, ");
        sql.append("    a.udt_name                                                as dataType, ");
        sql.append("    a.numeric_precision                                       as size, ");
        sql.append("    a.numeric_scale                                           as decimalDigits, ");
        sql.append("    a.character_maximum_length                                as width, ");
        sql.append("    a.column_default                                          as defaultValue, ");
        sql.append("    a.ordinal_position                                        as ordinalPosition, ");
        sql.append("    a.is_identity, ");
        sql.append("    a.table_catalog, ");
        sql.append("    a.character_octet_length, ");
        sql.append("    a.datetime_precision, ");
        sql.append("    a.udt_schema, ");
        sql.append("    a.data_type ");
        sql.append("from information_schema.columns a left join information_schema.views b on (a.table_catalog=b.table_catalog and a.table_schema=b.table_schema and a.table_name=b.table_name) ");
        sql.append("where b.table_schema is null ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(a.table_schema) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(a.table_schema) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("order by a.table_schema, a.table_name, a.ordinal_position ");
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
            Schema schema = mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.POSTGRE_SQL, name));
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
        // 查询字段注释
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    n.nspname as schemaName, ");
        sql.append("    c.relname as tableName, ");
        sql.append("    a.attname as columnName, ");
        sql.append("    col_description(a.attrelid, a.attnum) as columnComment ");
        sql.append("from pg_class c ");
        sql.append("    join pg_attribute a on (c.oid = a.attrelid) ");
        sql.append("    join pg_namespace n on (n.oid = c.relnamespace) ");
        sql.append("where a.attnum > 0 ");
        sql.append("  and not a.attisdropped ");
        // 所有可能的 relkind 值：
        //   r：普通表
        //   i：索引
        //   S：序列对象
        //   v：视图
        //   c：复合类型
        //   t：TOAST 表
        //   f：外部表
        //   p：分区表
        //   I：对主键或唯一键创建的索引
        //   C：对检查约束创建的索引
        //   T：自动以数据类型的 TOAST 表
        //   F：外键约束
        sql.append("  and c.relkind in ('r', 'p') ");
        sql.append("  and col_description(a.attrelid, a.attnum) is not null ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(n.nspname) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(c.relname) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(n.nspname) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(c.relname) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("order by n.nspname, c.relname, a.attnum ");
        mapColumns = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapColumns) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            String columnName = Conv.asString(map.get("columnName")).toLowerCase();
            String columnComment = Conv.asString(map.get("columnComment"), null);
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
            column.setComment(columnComment);
        }
        // 查询表信息
        sql.setLength(0);
        params.clear();
        sql.append("SELECT ");
        sql.append("    nps.nspname as schemaName, ");
        sql.append("    cls.relname as tableName, ");
        sql.append("    description.description as comment ");
        sql.append("FROM ");
        sql.append("    pg_class cls ");
        sql.append("    LEFT JOIN pg_namespace nps on nps.oid = cls.relnamespace ");
        sql.append("    LEFT JOIN pg_description description ON cls.oid = description.objoid and description.objsubid = 0 ");
        sql.append("WHERE cls.relkind in ('r', 'p') ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(nps.nspname) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(cls.relname) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(nps.nspname) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(cls.relname) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
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
        // 查询主键&索引 ->
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    tc.table_schema     as schemaName, ");
        sql.append("    tc.table_name       as tableName, ");
        sql.append("    c.conname           as name, ");
        sql.append("    kc.column_name      as columnName, ");
        sql.append("    true                as unique, ");
        sql.append("    tc.constraint_type  as type, ");
        sql.append("    ordinal_position    as position ");
        sql.append("from ");
        sql.append("    information_schema.table_constraints tc ");
        sql.append("    join information_schema.key_column_usage kc on kc.constraint_name = tc.constraint_name ");
        sql.append("    join pg_constraint c on c.conname = tc.constraint_name ");
        sql.append("where tc.constraint_type in ('PRIMARY KEY') ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(tc.table_schema) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(tc.table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(tc.table_schema) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(tc.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("union ");
        sql.append("select ");
        sql.append("    n.nspname                           as schemaName, ");
        sql.append("    t.relname                           as tableName, ");
        sql.append("    i.relname                           as name, ");
        sql.append("    a.attname                           as columnName, ");
        sql.append("    x.indisunique                       as unique, ");
        sql.append("    'INDEX'                             as type, ");
        sql.append("    array_position(x.indkey, a.attnum)  as position ");
        sql.append("from ");
        sql.append("    pg_index as x ");
        sql.append("    join pg_class as i on i.oid = x.indexrelid ");
        sql.append("    join pg_class as t on t.oid = x.indrelid ");
        sql.append("    join pg_attribute as a on (a.attrelid = t.oid and a.attnum = any(x.indkey)) ");
        sql.append("    join pg_namespace as n on n.oid = i.relnamespace ");
        sql.append("where t.relkind in ('r', 'p') ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(n.nspname) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(t.relname) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(n.nspname) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(t.relname) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("order by schemaName, tableName, name, position");
        List<Map<String, Object>> mapStatistics = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapStatistics) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            String columnName = Conv.asString(map.get("columnName")).toLowerCase();
            boolean unique = Conv.asBoolean(map.get("unique"));
            String type = Conv.asString(map.get("type")).toLowerCase();
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
            column.setPartOfIndex(true);
            if (unique) {
                column.setPartOfUniqueIndex(true);
            }
            if ("PRIMARY KEY".equalsIgnoreCase(type)) {
                column.setPartOfPrimaryKey(true);
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
                    index = new Index(table);
                    index.setName(name);
                    index.setUnique(unique);
                    index.getAttributes().putAll(map);
                    table.addIndex(index);
                }
                index.addColumn(column);
            }
        }
        // 查询存储过程&函数
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    routine_schema              as schemaName, ");
        sql.append("    routine_name                as name, ");
        sql.append("    routine_type                as type, ");
        sql.append("    routine_definition          as definition, ");
        sql.append("    routine_body , ");
        sql.append("    specific_catalog, ");
        sql.append("    specific_schema , ");
        sql.append("    specific_name , ");
        sql.append("    routine_catalog, ");
        sql.append("    data_type, ");
        sql.append("    character_maximum_length, ");
        sql.append("    character_octet_length, ");
        sql.append("    numeric_precision, ");
        sql.append("    numeric_scale, ");
        sql.append("    datetime_precision, ");
        sql.append("    interval_precision, ");
        sql.append("    udt_name, ");
        sql.append("    external_language, ");
        sql.append("    external_name, ");
        sql.append("    parameter_style ");
        sql.append("from information_schema.routines ");
        sql.append("where 1=1 ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(routine_schema) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(routine_schema) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by routine_schema, routine_type, routine_name ");
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

    private void fillColumn(Column column, Map<String, Object> map) {
        column.setName(Conv.asString(map.get("columnName"), null));
        column.setNotNull(Conv.asBoolean(map.get("notNull")));
        column.setDataType(Conv.asString(map.get("dataType"), null));
        column.setSize(Conv.asInteger(map.get("size")));
        if (column.getSize() == 0) {
            column.setSize(Conv.asInteger(map.get("datetime_precision")));
        }
        column.setDecimalDigits(Conv.asInteger(map.get("decimalDigits")));
        column.setWidth(Conv.asInteger(map.get("width")));
        column.setDefaultValue(Conv.asString(map.get("defaultValue"), null));
        column.setOrdinalPosition(Conv.asInteger(map.get("ordinalPosition")));
        // is_identity | column_default LIKE 'nextval(%'
        column.setAutoIncremented(Conv.asBoolean(map.get("is_identity")) || Conv.asString(map.get("defaultValue")).toLowerCase().startsWith("nextval("));
    }
}
