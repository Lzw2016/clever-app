package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;
import org.clever.util.Assert;

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

    // --------------------------------------------------------------------------------------------
    //  表结构元数据
    // --------------------------------------------------------------------------------------------

    @Override
    protected List<Schema> doGetSchemas(Collection<String> schemasName,
                                        Collection<String> tablesName,
                                        Set<String> ignoreSchemas,
                                        Set<String> ignoreTables,
                                        Set<String> ignoreTablesPrefix,
                                        Set<String> ignoreTablesSuffix) {
        // 所有的 Schema | Map<schemaName, Schema>
        final Map<String, Schema> mapSchema = new HashMap<>();
        // 查询表信息
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("    nps.nspname             as schemaName, ");
        sql.append("    cls.relname             as tableName, ");
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
        sql.append("order by nps.nspname, cls.relname");
        List<Map<String, Object>> tables = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : tables) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            String comment = Conv.asString(map.get("comment"), null);
            if (ignoreTablesPrefix.stream().anyMatch(tableName::startsWith)) {
                continue;
            }
            if (ignoreTablesSuffix.stream().anyMatch(tableName::endsWith)) {
                continue;
            }
            Schema schema = mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.POSTGRE_SQL, name));
            Table table = new Table(schema);
            table.setName(tableName);
            table.setComment(comment);
            schema.addTable(table);
        }
        // 查询字段信息
        sql.setLength(0);
        params.clear();
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
        sql.append("from information_schema.columns a ");
        sql.append("where 1=1 ");
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
        List<Map<String, Object>> columns = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : columns) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String tableName = Conv.asString(map.get("tableName")).toLowerCase();
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            Table table = schema.getTable(tableName);
            if (table == null) {
                continue;
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
        columns = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : columns) {
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
        List<Map<String, Object>> statistics = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : statistics) {
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
        sql.append("    b.nspname                                               as schemaName, ");
        sql.append("    a.proname                                               as name, ");
        sql.append("    a.prokind                                               as type, ");
        sql.append("    pg_catalog.pg_get_functiondef(a.oid)                    as definition, ");
        sql.append("    pg_catalog.pg_get_function_result(a.oid)                as returnType, ");
        sql.append("    pg_catalog.pg_get_function_arguments(a.oid)             as arguments, ");
        sql.append("    a.prosrc                                                as proSrc, ");
        sql.append("    c.lanname                                               as lanName, ");
        sql.append("    pg_catalog.pg_get_function_identity_arguments(a.oid)    as identityArguments ");
        sql.append("from pg_catalog.pg_proc a ");
        sql.append("    left join pg_catalog.pg_namespace b on (b.oid = a.pronamespace) ");
        sql.append("    left join pg_catalog.pg_language c on (c.oid = a.prolang) ");
        sql.append("where 1 = 1 ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(b.nspname) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(b.nspname) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by b.nspname, a.prokind, a.proname");
        List<Map<String, Object>> routines = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : routines) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            String type = Conv.asString(map.get("type")).toLowerCase();
            // String definition = Conv.asString(map.get("definition"));
            String returnType = Conv.asString(map.get("returnType"));
            String arguments = Conv.asString(map.get("arguments"));
            String proSrc = Conv.asString(map.get("proSrc"));
            String lanName = Conv.asString(map.get("lanName"));
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            Procedure procedure = new Procedure(schema);
            procedure.setName(name);
            // a.prokind 可能的值包括：
            //   p,procedure    表示存储过程
            //   f,function     表示函数
            procedure.setFunction(StringUtils.equalsAnyIgnoreCase(type, "function", "f"));
            String stripChars = "\r\n";
            String typeName = procedure.isFunction() ? "function" : "procedure";
            String customDefinition = "create or replace " + typeName + " " + toLiteral(name) + "(" + LINE +
                StringUtils.strip(arguments, stripChars) + LINE +
                ")" + LINE +
                "returns " + StringUtils.trim(returnType) + LINE +
                "language " + lanName + LINE +
                "as" + LINE +
                "$" + typeName + "$" + LINE +
                StringUtils.strip(proSrc, stripChars) + LINE +
                "$" + typeName + "$;";
            procedure.setDefinition(customDefinition);
            procedure.getAttributes().putAll(map);
            schema.addProcedure(procedure);
        }
        // 查询序列
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    sequence_schema              as schemaName, ");
        sql.append("    sequence_name                as name, ");
        sql.append("    minimum_value                as minValue, ");
        sql.append("    maximum_value                as maxValue, ");
        sql.append("    increment                    as increment, ");
        sql.append("    cycle_option                 as cycle, ");
        sql.append("    sequence_catalog, ");
        sql.append("    data_type, ");
        sql.append("    numeric_precision, ");
        sql.append("    numeric_precision_radix, ");
        sql.append("    numeric_scale, ");
        sql.append("    start_value ");
        sql.append("from information_schema.sequences ");
        sql.append("where 1=1 ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(sequence_schema) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(sequence_schema) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by sequence_catalog, sequence_schema, sequence_name ");
        List<Map<String, Object>> sequences = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : sequences) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            Long minValue = Conv.asLong(map.get("minValue"), null);
            Long maxValue = Conv.asLong(map.get("maxValue"), null);
            Long increment = Conv.asLong(map.get("increment"), null);
            boolean cycle = Conv.asBoolean(map.get("cycle"));
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            Sequence sequence = new Sequence(schema);
            sequence.setName(name);
            sequence.setMinValue(minValue);
            sequence.setMaxValue(maxValue);
            sequence.setIncrement(increment);
            sequence.setCycle(cycle);
            schema.addSequence(sequence);
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

    // --------------------------------------------------------------------------------------------
    //  表结构变更 DDL 语句
    // --------------------------------------------------------------------------------------------

    @Override
    public String alterTable(Table newTable, Table oldTable) {
        Assert.notNull(newTable, "参数 newTable 不能为空");
        Assert.notNull(oldTable, "参数 oldTable 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // 表名变化
        if (!Objects.equals(newTable.getName(), oldTable.getName())) {
            // alter table auto_increment_id rename to auto_increment_id2;
            ddl.append(String.format(
                "alter table %s rename to %s;",
                toLiteral(oldTable.getName()), toLiteral(newTable.getName())
            )).append(LINE);
        }
        // 修改表备注
        if (!Objects.equals(newTable.getComment(), oldTable.getComment())) {
            // comment on table auto_increment_id2 is '自增长ID数据表2';
            ddl.append(String.format(
                "comment on table %s is '%s';",
                toLiteral(newTable.getName()), toComment(newTable.getComment())
            )).append(LINE);
        }
        // 字段变化、主键变化、索引变化
        ddl.append(doAlterTable(newTable, oldTable));
        return ddl.toString();
    }

    @Override
    public String dropTable(Table oldTable) {
        Assert.notNull(oldTable, "参数 oldTable 不能为空");
        return String.format("drop table %s;", toLiteral(oldTable.getName())) + LINE;
    }

    @Override
    public String createTable(Table newTable) {
        Assert.notNull(newTable, "参数 newTable 不能为空");
        // create table auto_increment_id2
        //(
        //    c1  smallint,
        //    c2  integer,
        //    c3  bigint,
        //    constraint aii2_pk primary key (c1, c2)
        //);
        //comment on table auto_increment_id2 is '自增长ID数据表';
        //comment on column auto_increment_id2.c1 is 'smallint';
        //comment on column auto_increment_id2.c2 is 'integer';
        //comment on column auto_increment_id2.c3 is 'bigint';
        final StringBuilder ddl = new StringBuilder();
        final PrimaryKey primaryKey = newTable.getPrimaryKey();
        final List<Column> columns = newTable.getColumns();
        final List<Index> indices = newTable.getIndices();
        ddl.append("create table ").append(toLiteral(newTable.getName())).append(LINE);
        ddl.append("(").append(LINE);
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            ddl.append(TAB).append(String.format("%s %s", toLiteral(column.getName()), columnType(column)));
            String defaultValue = defaultValue(column);
            if (StringUtils.isNotBlank(defaultValue)) {
                ddl.append(" default ").append(defaultValue);
            }
            if (column.isNotNull()) {
                ddl.append(" not null");
            }
            if ((i + 1) < columns.size()) {
                ddl.append(",").append(LINE);
            } else if (newTable.getPrimaryKey() != null) {
                // 最后一个字段 & 存在主键
                ddl.append(",").append(LINE);
                ddl.append(TAB);
                if (StringUtils.isNotBlank(primaryKey.getName())) {
                    ddl.append(String.format("constraint %s ", toLiteral(primaryKey.getName())));
                }
                ddl.append("primary key (");
                for (int j = 0; j < primaryKey.getColumns().size(); j++) {
                    Column pCol = primaryKey.getColumns().get(j);
                    if (j > 0) {
                        ddl.append(", ");
                    }
                    ddl.append(toLiteral(pCol.getName()));
                }
                ddl.append(")").append(LINE);
            } else {
                ddl.append(LINE);
            }
        }
        ddl.append(");").append(LINE);
        // 表备注
        if (StringUtils.isNotBlank(newTable.getComment())) {
            ddl.append(String.format(
                "comment on table %s is '%s';",
                toLiteral(newTable.getName()), toComment(newTable.getComment()))
            ).append(LINE);
        }
        // 字段备注
        for (Column column : columns) {
            if (StringUtils.isBlank(column.getComment())) {
                continue;
            }
            ddl.append(String.format(
                "comment on column %s.%s is '%s';",
                toLiteral(newTable.getName()), toLiteral(column.getName()), toComment(column.getComment()))
            ).append(LINE);
        }
        // 索引
        for (Index index : indices) {
            if (primaryKey != null && Objects.equals(primaryKey.getName(), index.getName())) {
                continue;
            }
            ddl.append(createIndex(index));
        }
        return ddl.toString();
    }

    @Override
    public String alterColumn(Column newColumn, Column oldColumn) {
        Assert.notNull(newColumn, "参数 newColumn 不能为空");
        Assert.notNull(oldColumn, "参数 oldColumn 不能为空");
        final StringBuilder ddl = new StringBuilder();
        final String tableName = newColumn.getTableName();
        // alter table sys_user2 rename column is_enable2 to is_enable3
        if (!Objects.equals(newColumn.getName(), oldColumn.getName())) {
            ddl.append(String.format(
                "alter table %s rename column %s to %s;",
                toLiteral(tableName), toLiteral(oldColumn.getName()), toLiteral(newColumn.getName())
            )).append(LINE);
        }
        // comment on column sys_user.is_enable_1 is '是否启用: 0:禁用，1:启用_1'
        if (!Objects.equals(newColumn.getComment(), oldColumn.getComment())) {
            ddl.append(String.format(
                "comment on column %s.%s is '%s';",
                toLiteral(tableName), toLiteral(newColumn.getName()), toComment(newColumn.getComment())
            )).append(LINE);
        }
        // alter table auto_increment_id2 alter column c11 set default 'abc';
        if (!Objects.equals(newColumn.getDefaultValue(), oldColumn.getDefaultValue())) {
            ddl.append(String.format(
                "alter table %s alter column %s set default %s;",
                toLiteral(tableName), toLiteral(newColumn.getName()), defaultValue(newColumn)
            )).append(LINE);
        }
        // alter table auto_increment_id2 alter column c11 type varchar(12) using c11::varchar(12);
        if (!Objects.equals(newColumn.getDataType(), oldColumn.getDataType())
            || !Objects.equals(newColumn.getSize(), oldColumn.getSize())
            || !Objects.equals(newColumn.getDecimalDigits(), oldColumn.getDecimalDigits())
            || !Objects.equals(newColumn.getWidth(), oldColumn.getWidth())) {
            ddl.append(String.format(
                "alter table %s alter column %s type %s using %s::%s;",
                toLiteral(tableName), toLiteral(newColumn.getName()), columnType(newColumn), toLiteral(newColumn.getName()), columnType(newColumn)
            )).append(LINE);
        }
        // alter table auto_increment_id2 alter column c11 set not null;
        if (!Objects.equals(newColumn.isNotNull(), oldColumn.isNotNull())) {
            ddl.append(String.format(
                "alter table %s alter column %s set",
                toLiteral(tableName), toLiteral(newColumn.getName())
            ));
            if (newColumn.isNotNull()) {
                ddl.append(" not");
            }
            ddl.append(" null;").append(LINE);
        }
        return ddl.toString();
    }

    @Override
    public String dropColumn(Column oldColumn) {
        Assert.notNull(oldColumn, "参数 oldColumn 不能为空");
        // alter table auto_increment_id2 drop column c23;
        return String.format("alter table %s drop column %s;", toLiteral(oldColumn.getTableName()), toLiteral(oldColumn.getName())) + LINE;
    }

    @Override
    public String createColumn(Column newColumn) {
        Assert.notNull(newColumn, "参数 newColumn 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // alter table auto_increment_id2 add c25 varchar(10) default 'abc' not null;
        ddl.append(String.format(
            "alter table %s add %s %s",
            toLiteral(newColumn.getTableName()), toLiteral(newColumn.getName()), columnType(newColumn)
        ));
        if (StringUtils.isNotBlank(newColumn.getDefaultValue())) {
            ddl.append(" default ").append(defaultValue(newColumn));
        }
        if (newColumn.isNotNull()) {
            ddl.append(" not null");
        }
        ddl.append(";").append(LINE);
        // comment on column auto_increment_id2.c25 is '测试';
        if (StringUtils.isNotBlank(newColumn.getComment())) {
            ddl.append(String.format(
                "comment on column %s.%s is '%s';",
                toLiteral(newColumn.getTableName()), toLiteral(newColumn.getName()), toComment(newColumn.getComment())
            )).append(LINE);
        }
        return ddl.toString();
    }

    @Override
    public String alterPrimaryKey(PrimaryKey newPrimaryKey, PrimaryKey oldPrimaryKey) {
        if (oldPrimaryKey == null && newPrimaryKey == null) {
            return StringUtils.EMPTY;
        }
        final StringBuilder ddl = new StringBuilder();
        final String tableName = newPrimaryKey != null ? newPrimaryKey.getTableName() : oldPrimaryKey.getTableName();
        if (oldPrimaryKey != null) {
            // alter table auto_increment_id2 drop constraint auto_increment_id2_pk;
            ddl.append(String.format("alter table %s drop constraint %s;", toLiteral(tableName), toLiteral(oldPrimaryKey.getName()))).append(LINE);
        }
        if (newPrimaryKey != null) {
            // alter table auto_increment_id2 add constraint auto_increment_id2_pk primary key (c1);
            ddl.append(String.format(
                "alter table %s add constraint %s primary key (",
                toLiteral(tableName), toLiteral(newPrimaryKey.getName())
            ));
            for (int i = 0; i < newPrimaryKey.getColumns().size(); i++) {
                Column column = newPrimaryKey.getColumns().get(i);
                if (i > 0) {
                    ddl.append(", ");
                }
                ddl.append(toLiteral(column.getName()));
            }
            ddl.append(");").append(LINE);
        }
        return ddl.toString();
    }

    @Override
    public String dropPrimaryKey(PrimaryKey oldPrimaryKey) {
        Assert.notNull(oldPrimaryKey, "参数 oldPrimaryKey 不能为空");
        // alter table auto_increment_id2 drop constraint auto_increment_id2_pk;
        return String.format(
            "alter table %s drop constraint %s;",
            toLiteral(oldPrimaryKey.getTableName()), toLiteral(oldPrimaryKey.getName())
        ) + LINE;
    }

    @Override
    public String createPrimaryKey(PrimaryKey newPrimaryKey) {
        Assert.notNull(newPrimaryKey, "参数 newPrimaryKey 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // alter table auto_increment_id2 add constraint auto_increment_id2_pk primary key (c1, c2);
        ddl.append(String.format(
            "alter table %s add constraint %s primary key (",
            toLiteral(newPrimaryKey.getTableName()), toLiteral(newPrimaryKey.getName())
        ));
        for (int i = 0; i < newPrimaryKey.getColumns().size(); i++) {
            Column column = newPrimaryKey.getColumns().get(i);
            if (i > 0) {
                ddl.append(", ");
            }
            ddl.append(toLiteral(column.getName()));
        }
        ddl.append(");").append(LINE);
        return ddl.toString();
    }

    @Override
    public String alterIndex(Index newIndex, Index oldIndex) {
        if (newIndex == null && oldIndex == null) {
            return StringUtils.EMPTY;
        }
        final StringBuilder ddl = new StringBuilder();
        final String tableName = newIndex != null ? newIndex.getTableName() : oldIndex.getTableName();
        if (oldIndex != null) {
            // drop index auto_increment_id2_c1_idx;
            ddl.append(String.format("drop index %s;", toLiteral(oldIndex.getName()))).append(LINE);
        }
        if (newIndex != null) {
            // create unique index auto_increment_id2_c1_idx on auto_increment_id2 (c1);
            ddl.append("create ");
            if (newIndex.isUnique()) {
                ddl.append("unique ");
            }
            ddl.append(String.format("index %s on %s (", toLiteral(newIndex.getName()), toLiteral(tableName)));
            for (int i = 0; i < newIndex.getColumns().size(); i++) {
                Column column = newIndex.getColumns().get(i);
                if (i > 0) {
                    ddl.append(", ");
                }
                ddl.append(toLiteral(column.getName()));
            }
            ddl.append(");").append(LINE);
        }
        return ddl.toString();
    }

    @Override
    public String dropIndex(Index oldIndex) {
        Assert.notNull(oldIndex, "参数 oldPrimaryKey 不能为空");
        // drop index auto_increment_id2_c1_idx;
        return String.format("drop index %s;", toLiteral(oldIndex.getName())) + LINE;
    }

    @Override
    public String createIndex(Index newIndex) {
        Assert.notNull(newIndex, "参数 newIndex 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // create unique index auto_increment_id2_c1_idx on auto_increment_id2 (c1);
        ddl.append("create ");
        if (newIndex.isUnique()) {
            ddl.append("unique ");
        }
        ddl.append(String.format("index %s on %s (", toLiteral(newIndex.getName()), toLiteral(newIndex.getTableName())));
        for (int i = 0; i < newIndex.getColumns().size(); i++) {
            Column column = newIndex.getColumns().get(i);
            if (i > 0) {
                ddl.append(", ");
            }
            ddl.append(toLiteral(column.getName()));
        }
        ddl.append(");").append(LINE);
        return ddl.toString();
    }

    @Override
    protected String toLiteral(String objName) {
        Assert.isNotBlank(objName, "参数 objName 不能为空");
        final String special = "@#$%^&*+-/|<>=?![]{};,`. ";
        objName = StringUtils.trim(objName).toLowerCase();
        if (StringUtils.containsAny(objName, special)) {
            return "\"" + objName + "\"";
        }
        return objName;
    }

    protected String columnType(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        String dataType = column.getAttribute("data_type");
        final String[] dataTypes = new String[]{
            "smallint", "integer", "bigint", "real", "double precision",
            "boolean", "bytea",
        };
        if (StringUtils.equalsAnyIgnoreCase(dataType, dataTypes)) {
            return dataType;
        }
        column = columnTypeMapping(column, DbType.POSTGRE_SQL);
        dataType = StringUtils.lowerCase(column.getDataType());
        final String[] noParamType = new String[]{
            "int2", "int4", "int8", "float4", "float8",
            "timestamp", "timestamptz", "date", "time", "timetz",
            "bool", "bytea",
        };
        if (StringUtils.equalsAnyIgnoreCase(dataType, noParamType)) {
            return dataType;
        }
        // timestamp(6) with local time zone | varchar2(64) | number(10, 4)
        return super.columnType(column);
    }

    protected String defaultValue(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        return defaultValueMapping(column, DbType.POSTGRE_SQL);
    }

    // --------------------------------------------------------------------------------------------
    //  其它(序列、存储过程、函数)元数据 DDL 语句
    // --------------------------------------------------------------------------------------------

    @Override
    public String alterSequence(Sequence newSequence, Sequence oldSequence) {
        Assert.notNull(newSequence, "参数 newSequence 不能为空");
        Assert.notNull(oldSequence, "参数 oldSequence 不能为空");
        StringBuilder ddl = new StringBuilder();
        final String newName = toLiteral(newSequence.getName());
        // alter sequence q_name rename to q_name2;
        if (!Objects.equals(oldSequence.getName(), newSequence.getName())) {
            ddl.append(String.format(
                "alter sequence %s rename to %s;", toLiteral(oldSequence.getName()), newName
            )).append(LINE);
        }
        // alter sequence q_name2 cache 10;
        // alter sequence q_name2 no cycle;
        // alter sequence q_name2 start with 1 increment by 1 minvalue 0 maxvalue 999;
        if (!Objects.equals(oldSequence.isCycle(), newSequence.isCycle())) {
            ddl.append(String.format(
                "alter sequence %s %s cycle;", newName, (newSequence.isCycle() ? " no" : "")
            )).append(LINE);
        }
        final boolean incrementChange = !Objects.equals(oldSequence.getIncrement(), newSequence.getIncrement());
        final boolean minValueChange = !Objects.equals(oldSequence.getMinValue(), newSequence.getMinValue());
        final boolean maxValueChange = !Objects.equals(oldSequence.getMaxValue(), newSequence.getMaxValue());
        if (incrementChange || minValueChange || maxValueChange) {
            ddl.append("alter sequence ").append(newName);
            if (incrementChange) {
                ddl.append(" increment by ").append(newSequence.getIncrement());
            }
            if (minValueChange) {
                ddl.append(" minvalue ").append(newSequence.getMinValue());
            }
            if (maxValueChange) {
                ddl.append(" maxvalue ").append(newSequence.getMaxValue());
            }
            ddl.append(";").append(LINE);
        }
        return ddl.toString();
    }

    @Override
    public String dropSequence(Sequence oldSequence) {
        Assert.notNull(oldSequence, "参数 oldSequence 不能为空");
        // drop sequence q_name;
        return String.format("drop sequence %s;%s", toLiteral(oldSequence.getName()), LINE);
    }

    @Override
    public String createSequence(Sequence newSequence) {
        Assert.notNull(newSequence, "参数 newSequence 不能为空");
        // create sequence q_name start with 3 increment by 2 minvalue 2 maxvalue 100000 cache 8 cycle;
        // comment on sequence q_name is '备注';
        StringBuilder ddl = new StringBuilder();
        ddl.append("create sequence ").append(toLiteral(newSequence.getName()));
        if (newSequence.getIncrement() != null) {
            ddl.append(" increment by ").append(newSequence.getIncrement());
        }
        if (newSequence.getMinValue() != null) {
            ddl.append(" minvalue ").append(newSequence.getMinValue());
        }
        if (newSequence.getMaxValue() != null) {
            ddl.append(" maxvalue ").append(newSequence.getMaxValue());
        }
        if (newSequence.isCycle()) {
            ddl.append(" cycle");
        }
        ddl.append(";").append(LINE);
        return ddl.toString();
    }

    @Override
    public String dropProcedure(Procedure oldProcedure) {
        Assert.notNull(oldProcedure, "参数 oldProcedure 不能为空");
        if (!Objects.equals(oldProcedure.getDbType(), jdbc.getDbType())) {
            return StringUtils.EMPTY;
        }
        // drop function nvl(item boolean, d boolean);
        String typeName = oldProcedure.isFunction() ? "function" : "procedure";
        return String.format(
            "drop %s %s(%s);%s",
            typeName,
            toLiteral(oldProcedure.getName()),
            Conv.asString(oldProcedure.getAttribute("identityArguments")),
            LINE
        );
    }

    @Override
    public String createProcedure(Procedure newProcedure) {
        Assert.notNull(newProcedure, "参数 newProcedure 不能为空");
        if (!Objects.equals(newProcedure.getDbType(), jdbc.getDbType())) {
            return StringUtils.EMPTY;
        }
        return newProcedure.getDefinition() + LINE;
    }
}
