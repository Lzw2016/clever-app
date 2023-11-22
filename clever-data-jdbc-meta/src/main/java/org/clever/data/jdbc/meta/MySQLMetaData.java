package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;
import org.clever.util.Assert;

import java.nio.charset.StandardCharsets;
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
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder sql = new StringBuilder();
        // 查询 schemas
        sql.append("select  ");
        sql.append("    schema_name as `schemaName` ");
        sql.append("from information_schema.schemata ");
        if (!schemasName.isEmpty()) {
            sql.append("where lower(schema_name) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        sql.append("order by schema_name ");
        List<Map<String, Object>> schemas = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : schemas) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.MYSQL, name));
        }
        // 查询表信息
        sql.setLength(0);
        params.clear();
        // TABLE_TYPE列可能的一些值：
        //   "BASE TABLE"：表示普通的表。
        //   "VIEW"：表示视图。
        //   "SYSTEM VIEW"：表示系统视图，这些视图提供了关于数据库内部元数据的信息。
        //   "TEMPORARY"：表示临时表，它们在会话结束后自动被删除。
        //   "SEQUENCE"：表示序列，这是MySQL 8.0版本后引入的新特性，用于生成序列号。
        sql.append("select ");
        sql.append("    table_schema        as `schemaName`, ");
        sql.append("    table_name          as `tableName`, ");
        sql.append("    table_comment       as `comment` ");
        sql.append("from ");
        sql.append("    information_schema.tables ");
        sql.append("where lower(table_type) in ('base table') ");
        addWhere(sql, params, schemasName, tablesName, ignoreSchemas, ignoreTables);
        sql.append("order by table_schema, table_name");
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
            Schema schema = mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.MYSQL, name));
            Table table = new Table(schema);
            table.setName(tableName);
            table.setComment(comment);
            schema.addTable(table);
        }
        // 查询字段信息
        sql.setLength(0);
        params.clear();
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
        List<Map<String, Object>> statistics = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : statistics) {
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
        sql.append("    db          as `schemaName`, ");
        sql.append("    name        as `name`, ");
        sql.append("    type        as `type`, ");
        sql.append("    param_list, ");
        sql.append("    returns, ");
        sql.append("    body_utf8, ");
        sql.append("    language, ");
        sql.append("    specific_name, ");
        sql.append("    comment ");
        sql.append("from mysql.proc ");
        sql.append("where lower(type) in ('procedure','function') ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(db) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(db) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by db, type, name, specific_name ");
        List<Map<String, Object>> routines = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : routines) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            String type = Conv.asString(map.get("type")).toLowerCase();
            String paramList = StringUtils.toEncodedString((byte[]) map.get("param_list"), StandardCharsets.UTF_8);
            String returns = StringUtils.toEncodedString((byte[]) map.get("returns"), StandardCharsets.UTF_8);
            String body = StringUtils.toEncodedString((byte[]) map.get("body_utf8"), StandardCharsets.UTF_8);
            Schema schema = mapSchema.computeIfAbsent(schemaName, sName -> new Schema(DbType.MYSQL, sName));
            Procedure procedure = new Procedure(schema);
            procedure.setName(name);
            // ROUTINE_TYPE 可能的值包括：
            //   PROCEDURE 表示存储过程
            //   FUNCTION 表示函数
            procedure.setFunction("FUNCTION".equalsIgnoreCase(type));
            StringBuilder definition = new StringBuilder();
            String stripChars = "\r\n";
            if (procedure.isFunction()) {
                // CREATE FUNCTION function_name(parameter_list)
                // RETURNS return_datatype
                // [characteristic ...]
                // routine_body
                definition.append("create function ").append(toLiteral(name)).append("(").append(LINE)
                    .append(StringUtils.strip(paramList, stripChars)).append(LINE)
                    .append(")").append(LINE)
                    .append("returns ").append(returns).append(LINE)
                    .append(StringUtils.strip(body, stripChars));
            } else {
                // CREATE PROCEDURE procedure_name(parameter_list)
                // [characteristics]
                // routine_body
                definition.append("create procedure ").append(toLiteral(name)).append("(").append(LINE)
                    .append(StringUtils.strip(paramList, stripChars)).append(LINE)
                    .append(")").append(LINE)
                    .append(StringUtils.strip(body, stripChars));
            }
            String def = StringUtils.trim(definition.toString());
            if (!StringUtils.endsWith(def, ";")) {
                def = def + ";";
            }
            procedure.setDefinition(def);
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
        // extra 可以有以下取值：
        //   auto_increment: 表示该列是自动递增的
        //   default_generated: 表示该列使用了生成的默认值（如序列或自增字段）
        //   virtual generated: 表示该列是虚拟生成的
        //   stored generated: 表示该列是存储生成的
        //   ''（空字符串）：表示该列没有任何特殊属性
        if (Conv.asString(map.get("extra")).toLowerCase().contains("auto_increment")) {
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

    // --------------------------------------------------------------------------------------------
    //  表结构变更 DDL 语句
    // --------------------------------------------------------------------------------------------

    @Override
    public String alterTable(Table newTable, Table oldTable) {
        Assert.notNull(newTable, "参数 newTable 不能为空");
        Assert.notNull(oldTable, "参数 oldTable 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // 表名变化
        if (!StringUtils.equalsIgnoreCase(newTable.getName(), oldTable.getName())) {
            // rename table auto_increment_id to auto_increment_id2;
            ddl.append(String.format(
                "rename table %s to %s;",
                toLiteral(oldTable.getName()), toLiteral(newTable.getName())
            )).append(LINE);
        }
        // 修改表备注
        if (!Objects.equals(newTable.getComment(), oldTable.getComment())) {
            // alter table biz_code comment '业务编码表A';
            ddl.append(String.format(
                "alter table %s comment '%s';",
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
        // (
        //     id            bigint auto_increment comment '主键id',
        //     sequence_name varchar(127) collate utf8mb4_bin         not null comment '序列名称',
        //     current_value bigint      default -1                   not null comment '当前值',
        //     description   varchar(511)                             null comment '说明',
        //     create_at     datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
        //     update_at     datetime(3)                              null on update CURRENT_TIMESTAMP(3) comment '更新时间',
        //     constraint auto_increment_id_pk primary key (id, current_value)
        // ) comment '自增长id表';
        // create index idx_sys_login_failed_count_user_id on sys_login_failed_count (user_id);
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
            if (column.isAutoIncremented() && column.isPartOfPrimaryKey()) {
                ddl.append(" auto_increment");
            }
            if (StringUtils.isNotBlank(column.getComment())) {
                ddl.append(String.format(" comment '%s'", toComment(column.getComment())));
            }
            if ((i + 1) < columns.size()) {
                ddl.append(",").append(LINE);
            } else if (newTable.getPrimaryKey() != null) {
                // 最后一个字段 & 存在主键
                ddl.append(",").append(LINE);
                ddl.append(TAB);
                if (StringUtils.isNotBlank(primaryKey.getName())
                    && !StringUtils.trimToEmpty(primaryKey.getName()).equalsIgnoreCase("primary")) {
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
        ddl.append(")");
        // 表备注
        if (StringUtils.isNotBlank(newTable.getComment())) {
            ddl.append(String.format(" comment '%s'", toComment(newTable.getComment())));
        }
        ddl.append(";").append(LINE);
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
        // alter table auto_increment_id2 change sequence_name sequence_name2 varchar(128) collate utf8mb4_bin not null comment '序列名称';
        // alter table auto_increment_id2 change description description2 varchar(511) not null comment '说明2';
        if (StringUtils.equalsIgnoreCase(toLiteral(oldColumn.getName()), toLiteral(newColumn.getName()))) {
            ddl.append(String.format(
                "alter table %s modify %s %s",
                toLiteral(tableName), toLiteral(newColumn.getName()), columnType(newColumn)
            ));
        } else {
            ddl.append(String.format(
                "alter table %s change %s %s %s",
                toLiteral(tableName), toLiteral(oldColumn.getName()), toLiteral(newColumn.getName()), columnType(newColumn)
            ));
        }
        if (newColumn.isNotNull()) {
            ddl.append(" not null");
        }
        if (newColumn.isAutoIncremented() && newColumn.isPartOfPrimaryKey()) {
            ddl.append(" auto_increment");
        }
        if (StringUtils.isNotBlank(newColumn.getComment())) {
            ddl.append(String.format(" comment '%s'", toComment(newColumn.getComment())));
        }
        ddl.append(";").append(LINE);
        return ddl.toString();
    }

    @Override
    public String dropColumn(Column oldColumn) {
        Assert.notNull(oldColumn, "参数 oldColumn 不能为空");
        return String.format("alter table %s drop column %s;", toLiteral(oldColumn.getTableName()), toLiteral(oldColumn.getName())) + LINE;
    }

    @Override
    public String createColumn(Column newColumn) {
        Assert.notNull(newColumn, "参数 newColumn 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // alter table auto_increment_id2 add col varchar(100) default 'abc' not null comment '测试';
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
        if (newColumn.isAutoIncremented() && newColumn.isPartOfPrimaryKey()) {
            ddl.append(" auto_increment");
        }
        if (StringUtils.isNotBlank(newColumn.getComment())) {
            ddl.append(String.format(" comment '%s'", toComment(newColumn.getComment())));
        }
        ddl.append(";").append(LINE);
        return ddl.toString();
    }

    @Override
    public String alterPrimaryKey(PrimaryKey newPrimaryKey, PrimaryKey oldPrimaryKey) {
        if (oldPrimaryKey == null && newPrimaryKey == null) {
            return StringUtils.EMPTY;
        }
        final StringBuilder ddl = new StringBuilder();
        final String tableName = newPrimaryKey != null ? newPrimaryKey.getTableName() : oldPrimaryKey.getTableName();
        // 先删除主键
        if (oldPrimaryKey != null) {
            for (Column column : oldPrimaryKey.getColumns()) {
                // alter table auto_increment_id2 modify id bigint not null comment '主键';
                if (column.isAutoIncremented()) {
                    ddl.append(String.format(
                        "alter table %s modify %s %s",
                        toLiteral(tableName), toLiteral(column.getName()), columnType(column)
                    ));
                    String defaultValue = defaultValue(column);
                    if (StringUtils.isNotBlank(defaultValue)) {
                        ddl.append(" default ").append(defaultValue);
                    }
                    if (column.isNotNull()) {
                        ddl.append(" not null");
                    }
                    if (StringUtils.isNotBlank(column.getComment())) {
                        ddl.append(String.format(" comment '%s'", toComment(column.getComment())));
                    }
                    ddl.append(";").append(LINE);
                }
            }
            // alter table auto_increment_id2 drop primary key;
            ddl.append(String.format("alter table %s drop primary key;", toLiteral(tableName))).append(LINE);
        }
        // 在新增主键
        if (newPrimaryKey != null) {
            // alter table auto_increment_id2 add constraint auto_increment_id2_pk primary key (id, sequence_name);
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
            // alter table auto_increment_id2 modify id bigint default 1 not null auto_increment comment '主键id';
            boolean hasAutoInc = false;
            for (Column column : newPrimaryKey.getColumns()) {
                if (column.isAutoIncremented()) {
                    hasAutoInc = true;
                    ddl.append(String.format(
                        "alter table %s modify %s %s",
                        toLiteral(tableName), toLiteral(column.getName()), columnType(column)
                    ));
                    String defaultValue = defaultValue(column);
                    if (StringUtils.isNotBlank(defaultValue)) {
                        ddl.append(" default ").append(defaultValue);
                    }
                    if (column.isNotNull()) {
                        ddl.append(" not");
                    }
                    ddl.append(" null auto_increment");
                    if (StringUtils.isNotBlank(column.getComment())) {
                        ddl.append(String.format(" comment '%s'", toComment(column.getComment())));
                    }
                    ddl.append(";").append(LINE);
                }
            }
            if (hasAutoInc) {
                // alter table auto_increment_id2 auto_increment = 1;
                ddl.append(String.format("alter table %s auto_increment = 1;", toLiteral(tableName))).append(LINE);
            }
        }
        return ddl.toString();
    }

    @Override
    public String dropPrimaryKey(PrimaryKey oldPrimaryKey) {
        Assert.notNull(oldPrimaryKey, "参数 oldPrimaryKey 不能为空");
        final StringBuilder ddl = new StringBuilder();
        final String tableName = oldPrimaryKey.getTableName();
        // 确保当前表没有 auto_increment 列
        for (Column column : oldPrimaryKey.getColumns()) {
            // alter table auto_increment_id2 modify id bigint not null comment '主键';
            if (column.isAutoIncremented()) {
                ddl.append(String.format(
                    "alter table %s modify %s %s",
                    toLiteral(tableName), toLiteral(column.getName()), columnType(column)
                ));
                String defaultValue = defaultValue(column);
                if (StringUtils.isNotBlank(defaultValue)) {
                    ddl.append(" default ").append(defaultValue);
                }
                if (column.isNotNull()) {
                    ddl.append(" not null");
                }
                if (StringUtils.isNotBlank(column.getComment())) {
                    ddl.append(String.format(" comment '%s'", toComment(column.getComment())));
                }
                ddl.append(";").append(LINE);
            }
        }
        // alter table auto_increment_id2 drop primary key;
        ddl.append(String.format("alter table %s drop primary key;", toLiteral(tableName))).append(LINE);
        return ddl.toString();
    }

    @Override
    public String createPrimaryKey(PrimaryKey newPrimaryKey) {
        Assert.notNull(newPrimaryKey, "参数 newPrimaryKey 不能为空");
        final StringBuilder ddl = new StringBuilder();
        final String tableName = newPrimaryKey.getTableName();
        // alter table auto_increment_id2 add constraint auto_increment_id2_pk primary key (id, sequence_name);
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
        // alter table auto_increment_id2 modify id bigint default 1 not null auto_increment comment '主键id';
        boolean hasAutoInc = false;
        for (Column column : newPrimaryKey.getColumns()) {
            if (column.isAutoIncremented()) {
                hasAutoInc = true;
                ddl.append(String.format(
                    "alter table %s modify %s %s",
                    toLiteral(tableName), toLiteral(column.getName()), columnType(column)
                ));
                String defaultValue = defaultValue(column);
                if (StringUtils.isNotBlank(defaultValue)) {
                    ddl.append(" default ").append(defaultValue);
                }
                if (column.isNotNull()) {
                    ddl.append(" not");
                }
                ddl.append(" null auto_increment");
                if (StringUtils.isNotBlank(column.getComment())) {
                    ddl.append(String.format(" comment '%s'", toComment(column.getComment())));
                }
                ddl.append(";").append(LINE);
            }
        }
        if (hasAutoInc) {
            // alter table auto_increment_id2 auto_increment = 1;
            ddl.append(String.format("alter table %s auto_increment = 1;", toLiteral(tableName))).append(LINE);
        }
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
            // alter table auto_increment_id2 drop key auto_increment_uidx1;
            if (oldIndex.isUnique()) {
                ddl.append(String.format(
                    "alter table %s drop key %s;",
                    toLiteral(tableName), toLiteral(oldIndex.getName())
                )).append(LINE);
            } else {
                // drop index auto_increment_idx1 on auto_increment_id2;
                ddl.append(String.format(
                    "drop index %s on %s;",
                    toLiteral(oldIndex.getName()), toLiteral(tableName)
                )).append(LINE);
            }
        }
        if (newIndex != null) {
            // create unique index auto_increment_uidx1 on auto_increment_id2 (id, current_value);
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
        // alter table auto_increment_id2 drop key auto_increment_uidx1;
        if (oldIndex.isUnique()) {
            return String.format(
                "alter table %s drop key %s;",
                toLiteral(oldIndex.getTableName()), toLiteral(oldIndex.getName())
            ) + LINE;
        }
        // drop index auto_increment_idx1 on auto_increment_id2;
        return String.format(
            "drop index %s on %s;",
            toLiteral(oldIndex.getName()), toLiteral(oldIndex.getTableName())
        ) + LINE;
    }

    @Override
    public String createIndex(Index newIndex) {
        Assert.notNull(newIndex, "参数 newIndex 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // create unique index auto_increment_uidx1 on auto_increment_id2 (id, current_value);
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
            return String.format("`%s`", objName);
        }
        return objName;
    }

    protected String columnType(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        String columnType = column.getAttribute("column_type");
        if (StringUtils.isNotBlank(columnType)) {
            return StringUtils.lowerCase(StringUtils.trimToEmpty(columnType));
        }
        column = columnTypeMapping(column, DbType.MYSQL);
        String dataType = StringUtils.lowerCase(column.getDataType());
        if ("datetime".equalsIgnoreCase(dataType)) {
            return "datetime(3)";
        }
        if ("timestamp".equalsIgnoreCase(dataType)) {
            return "timestamp(3)";
        }
        if ("time".equalsIgnoreCase(dataType)) {
            return "time(3)";
        }
        final String[] noParamType = new String[]{
            "tinyint", "smallint", "mediumint", "int", "bigint", "float", "double",
            "tinytext", "text", "mediumtext", "longtext",
            "year", "date",
            "tinyblob", "mediumblob", "longblob",
        };
        if (StringUtils.equalsAnyIgnoreCase(dataType, noParamType)) {
            return dataType;
        }
        // datetime(3) | varchar(511) | decimal(20, 4)
        return super.columnType(column);
    }

    protected String defaultValue(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        return defaultValueMapping(column, DbType.MYSQL);
    }

    // --------------------------------------------------------------------------------------------
    //  其它(序列、存储过程、函数)元数据 DDL 语句
    // --------------------------------------------------------------------------------------------

    @Override
    public String alterSequence(Sequence newSequence, Sequence oldSequence) {
        // throw new UnsupportedOperationException("MySQL不支持“序列”功能");
        return StringUtils.EMPTY;
    }

    @Override
    public String dropSequence(Sequence oldSequence) {
        // throw new UnsupportedOperationException("MySQL不支持“序列”功能");
        return StringUtils.EMPTY;
    }

    @Override
    public String createSequence(Sequence newSequence) {
        // throw new UnsupportedOperationException("MySQL不支持“序列”功能");
        return StringUtils.EMPTY;
    }

    @Override
    public String dropProcedure(Procedure oldProcedure) {
        Assert.notNull(oldProcedure, "参数 oldProcedure 不能为空");
        if (!Objects.equals(oldProcedure.getDbType(), jdbc.getDbType())) {
            return StringUtils.EMPTY;
        }
        // drop function hello_world;
        String typeName = oldProcedure.isFunction() ? "function" : "procedure";
        return String.format(
            "drop %s %s;%s",
            typeName,
            toLiteral(oldProcedure.getName()),
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
