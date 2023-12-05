package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;
import org.clever.util.Assert;

import java.util.*;

/**
 * 获取数据库元数据 Oracle 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 15:53 <br/>
 */
@SuppressWarnings("DuplicatedCode")
public class OracleMetaData extends AbstractMetaData {
    public OracleMetaData(Jdbc jdbc) {
        super(jdbc);
        addIgnoreSchema("system");
        addIgnoreSchema("sys");
        addIgnoreSchema("outln");
        addIgnoreSchema("dip");
        addIgnoreSchema("oracle_ocm");
        addIgnoreSchema("dbsnmp");
        addIgnoreSchema("appqossys");
        addIgnoreSchema("wmsys");
        addIgnoreSchema("exfsys");
        addIgnoreSchema("ctxsys");
        addIgnoreSchema("anonymous");
        addIgnoreSchema("xdb");
        addIgnoreSchema("xs$null");
        addIgnoreSchema("mdsys");
        addIgnoreSchema("si_informtn_schema");
        addIgnoreSchema("ordplugins");
        addIgnoreSchema("orddata");
        addIgnoreSchema("ordsys");
        addIgnoreSchema("olapsys");
        addIgnoreSchema("mddata");
        addIgnoreSchema("spatial_wfs_admin_usr");
        addIgnoreSchema("spatial_csw_admin_usr");
        addIgnoreSchema("sysman");
        addIgnoreSchema("mgmt_view");
        addIgnoreSchema("apex_030200");
        addIgnoreSchema("apex_public_user");
        addIgnoreSchema("flows_files");
        addIgnoreSchema("owbsys");
        addIgnoreSchema("owbsys_audit");
        addIgnoreSchema("scott");
    }

    @Override
    public String currentSchema() {
        return StringUtils.lowerCase(jdbc.queryString("select sys_context('userenv', 'current_schema') from dual"));
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
        sql.append("select ");
        sql.append("    username as \"schemaName\"");
        sql.append("from sys.all_users ");
        sql.append("where 1=1 ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(username) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(username) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by username ");
        List<Map<String, Object>> schemas = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : schemas) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.ORACLE, name));
        }
        // 查询表信息
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    a.owner                                 as \"schemaName\", ");
        sql.append("    a.table_name                            as \"tableName\", ");
        sql.append("    b.comments                              as \"comment\" ");
        sql.append("from sys.all_tables a left join sys.all_tab_comments b on (a.owner = b.owner and a.table_name = b.table_name) ");
        sql.append("where a.iot_type is null ");
        if (!schemasName.isEmpty()) {
            sql.append("  and lower(a.owner) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("  and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("  and lower(a.owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("  and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("order by a.owner, a.table_name ");
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
            Schema schema = mapSchema.computeIfAbsent(schemaName, name -> new Schema(DbType.ORACLE, name));
            Table table = new Table(schema);
            table.setName(tableName);
            table.setComment(comment);
            schema.addTable(table);
        }
        // 查询字段信息
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    a.owner                                 as \"schemaName\", ");
        sql.append("    a.table_name                            as \"tableName\", ");
        sql.append("    a.column_name                           as \"columnName\", ");
        sql.append("    b.comments                              as \"columnComment\", ");
        sql.append("    a.nullable                              as \"notNull\", ");
        sql.append("    a.data_type                             as \"dataType\", ");
        sql.append("    a.data_precision                        as \"size\", ");
        sql.append("    a.DATA_SCALE                            as \"decimalDigits\", ");
        sql.append("    a.data_length                           as \"width\", ");
        sql.append("    a.data_default                          as \"defaultValue\", ");
        sql.append("    a.column_id                             as \"ordinalPosition\", ");
        sql.append("    a.char_length, ");
        sql.append("    a.character_set_name ");
        sql.append("from sys.all_tab_columns a ");
        sql.append("    left join sys.all_col_comments b on (a.owner=b.owner and a.table_name = b.table_name and a.column_name=b.column_name) ");
        sql.append("where 1=1 ");
        if (!schemasName.isEmpty()) {
            sql.append("  and lower(a.owner) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("  and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("  and lower(a.owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("  and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("order by a.owner, a.table_name, a.column_id ");
        List<Map<String, Object>> mapColumns = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : mapColumns) {
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
        // 查询主键&索引 ->
        sql.setLength(0);
        params.clear();
        // constraint_type字段可能的取值：
        //   "C"：表示检查约束（Check Constraint）
        //   "P"：表示主键约束（Primary Key Constraint）
        //   "U"：表示唯一约束（Unique Constraint）
        //   "R"：表示引用约束（Foreign Key Constraint）
        //   "V"：表示视图约束（View Constraint）
        //   "O"：表示对象约束（Object Constraint）
        //   "H"：表示哈希分区扩展约束（Hash Partition Extension Constraint）
        //   "F"：表示全文本索引约束（Text Index Constraint）
        //   "S"：表示空间索引约束（Spatial Index Constraint）
        //   "N"：表示无操作约束（No Action Constraint）
        sql.append("select");
        sql.append("    a.owner                                         as \"schemaName\",");
        sql.append("    a.table_name                                    as \"tableName\",");
        sql.append("    a.constraint_name                               as \"name\",");
        sql.append("    b.column_name                                   as \"columnName\",");
        sql.append("    'true'                                          as \"unique\",");
        sql.append("    a.constraint_type                               as \"type\",");
        sql.append("    b.position                                      as \"position\"");
        sql.append("from sys.all_constraints a");
        sql.append("    left join sys.all_cons_columns b on (a.owner=b.owner and a.table_name=b.table_name and a.constraint_name = b.constraint_name)");
        sql.append("where a.constraint_type in ('P')");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(a.owner) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(a.owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("union ");
        // index_type字段可能的取值：
        //   "NORMAL"：表示常规索引（Normal Index）
        //   "UNIQUE"：表示唯一索引（Unique Index）
        //   "BITMAP"：表示位图索引（Bitmap Index）
        //   "FUNCTION-BASED NORMAL"：表示基于函数的常规索引（Function-Based Normal Index）
        //   "FUNCTION-BASED BITMAP"：表示基于函数的位图索引（Function-Based Bitmap Index）
        //   "DOMAIN"：表示域索引（Domain Index）
        //   "LOB"：表示 CLOB 或 BLOB 数据类型的 LOB 索引（LOB Index）
        //   "XMLTYPE"：表示 XML 类型的索引（XMLType Index）
        //   "SPATIAL"：表示空间索引（Spatial Index）
        //   "PREFETCH"：表示预取索引（Prefetch Index）
        //   "DOMAIN INDEX PARTITION"：表示分区域索引（Domain Index Partition）
        sql.append("select ");
        sql.append("    a.owner                                         as \"schemaName\", ");
        sql.append("    a.table_name                                    as \"tableName\", ");
        sql.append("    a.index_name                                    as \"name\", ");
        sql.append("    b.column_name                                   as \"columnName\", ");
        sql.append("    decode(a.uniqueness, 'UNIQUE', 'true', 'false') as \"unique\", ");
        sql.append("    a.index_type                                    as \"type\", ");
        sql.append("    b.column_position                               as \"position\" ");
        sql.append("from sys.all_indexes a ");
        sql.append("    left join sys.all_ind_columns b on (a.table_owner =b.table_owner and a.index_name = b.index_name and a.table_name = b.table_name) ");
        sql.append("where 1=1 ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(a.owner) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!tablesName.isEmpty()) {
            sql.append("and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(a.owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(") ");
        }
        sql.append("order by \"schemaName\", \"tableName\", \"name\", \"position\"  ");
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
            if ("P".equalsIgnoreCase(type)) {
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
        sql.append("    owner               as \"schemaName\", ");
        sql.append("    name                as \"name\", ");
        sql.append("    type                as \"type\", ");
        sql.append("    text                as \"definition\", ");
        sql.append("    line                as \"line\" ");
        sql.append("from sys.all_source ");
        sql.append("where type in ('FUNCTION', 'PROCEDURE') ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(owner) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by owner, type, name, line ");
        List<Map<String, Object>> routines = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        Map<String, TupleTwo<StringBuilder, Procedure>> routineMap = new HashMap<>();
        for (Map<String, Object> map : routines) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            String type = Conv.asString(map.get("type")).toLowerCase();
            String definition = Conv.asString(map.get("definition"));
            Schema schema = mapSchema.computeIfAbsent(schemaName, sName -> new Schema(DbType.ORACLE, sName));
            String key = String.format("schemaName=%s|type=%s|name=%s", schemaName, type, name);
            TupleTwo<StringBuilder, Procedure> tuple = routineMap.get(key);
            if (tuple == null) {
                tuple = TupleTwo.creat(new StringBuilder("create or replace "), new Procedure(schema));
                routineMap.put(key, tuple);
                schema.addProcedure(tuple.getValue2());
            }
            if (definition != null) {
                tuple.getValue1().append(definition);
            }
            tuple.getValue2().setName(name);
            // type 可能的值包括：
            //   PROCEDURE 表示存储过程
            //   FUNCTION 表示函数
            tuple.getValue2().setFunction("FUNCTION".equalsIgnoreCase(type));
            tuple.getValue2().getAttributes().putAll(map);
        }
        for (TupleTwo<StringBuilder, Procedure> tuple : routineMap.values()) {
            String def = StringUtils.trim(tuple.getValue1().toString());
            if (!StringUtils.endsWith(def, ";")) {
                def = def + ";";
            }
            tuple.getValue2().setDefinition(def);
        }
        // 查询序列
        sql.setLength(0);
        params.clear();
        sql.append("select ");
        sql.append("    sequence_owner          as \"schemaName\", ");
        sql.append("    sequence_name           as \"name\", ");
        sql.append("    min_value               as \"minValue\", ");
        sql.append("    max_value               as \"maxValue\", ");
        sql.append("    increment_by            as \"increment\", ");
        sql.append("    cycle_flag              as \"cycle\", ");
        sql.append("    order_flag, ");
        sql.append("    cache_size, ");
        sql.append("    last_number ");
        sql.append("from sys.all_sequences ");
        sql.append("where 1=1 ");
        if (!schemasName.isEmpty()) {
            sql.append("and lower(sequence_owner) in (").append(createWhereIn(params, schemasName)).append(") ");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("and lower(sequence_owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(") ");
        }
        sql.append("order by sequence_owner, sequence_name ");
        List<Map<String, Object>> sequences = jdbc.queryMany(sql.toString(), params, RenameStrategy.None);
        for (Map<String, Object> map : sequences) {
            String schemaName = Conv.asString(map.get("schemaName")).toLowerCase();
            String name = Conv.asString(map.get("name")).toLowerCase();
            Long minValue = Conv.asLong(map.get("minValue"), null);
            Long maxValue = Conv.asLong(map.get("maxValue"), null);
            Long increment = Conv.asLong(map.get("increment"), null);
            boolean cycle = Conv.asBoolean(map.get("cycle"));
            Schema schema = mapSchema.computeIfAbsent(schemaName, sName -> new Schema(DbType.ORACLE, sName));
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
        column.setComment(Conv.asString(map.get("columnComment"), null));
        column.setNotNull(!Conv.asBoolean(map.get("notNull")));
        column.setDataType(Conv.asString(map.get("dataType"), null));
        column.setSize(Conv.asInteger(map.get("size")));
        column.setDecimalDigits(Conv.asInteger(map.get("decimalDigits")));
        column.setWidth(Conv.asInteger(map.get("width")));
        column.setDefaultValue(Conv.asString(map.get("defaultValue"), null));
        column.setOrdinalPosition(Conv.asInteger(map.get("ordinalPosition")));
        column.setAutoIncremented(false);
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
            // rename sys_user to "sys_user1"
            ddl.append(String.format(
                "rename %s to %s;",
                toLiteral(oldTable.getName()), toLiteral(newTable.getName())
            )).append(LINE);
        }
        // 修改表备注
        if (!Objects.equals(newTable.getComment(), oldTable.getComment())) {
            // comment on table "sys_user1" is '用户表''u'''
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
        // create table "table_name"
        // (
        //     user_id    number(19)                                        not null,
        //     update_at  timestamp(6) with local time zone default sysdate not null,
        //     primary key (user_id, user_code)
        //     constraint "sys_user2_pk" primary key (user_id, user_code)
        // );
        // comment on table table_name is 'AAA';
        // comment on column table_name.user_id is '用户id';
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
        if (!StringUtils.equalsIgnoreCase(newColumn.getName(), oldColumn.getName())) {
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
        // alter table sys_user modify is_enable_1 number(16) default 16 not null
        TupleTwo<Boolean, Boolean> typeAndDelValue = equalsColumnTypeAndDelValue(newColumn, oldColumn);
        if (typeAndDelValue.getValue1() || typeAndDelValue.getValue2() || !Objects.equals(newColumn.isNotNull(), oldColumn.isNotNull())) {
            ddl.append(String.format(
                "alter table %s modify %s %s",
                toLiteral(tableName), toLiteral(newColumn.getName()), columnType(newColumn)
            ));
            if (StringUtils.isNotBlank(newColumn.getDefaultValue())) {
                ddl.append(" default ").append(defaultValue(newColumn));
            }
            if (newColumn.isNotNull()) {
                ddl.append(" not null");
            }
            ddl.append(";").append(LINE);
        }
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
        // alter table sys_user add column_name varchar2(100) default 'aaa' not null
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
        // comment on column sys_user.column_name is '测试'
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
            // alter table sys_user2 drop primary key
            ddl.append(String.format("alter table %s drop primary key;", toLiteral(tableName))).append(LINE);
        }
        if (newPrimaryKey != null) {
            // alter table sys_user2 add constraint "sys_user2_pk"  primary key (user_id, user_code)
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
        // alter table sys_user2 drop primary key
        return String.format("alter table %s drop primary key;", toLiteral(oldPrimaryKey.getTableName())) + LINE;
    }

    @Override
    public String createPrimaryKey(PrimaryKey newPrimaryKey) {
        Assert.notNull(newPrimaryKey, "参数 newPrimaryKey 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // alter table sys_user2 add constraint "sys_user2_pk"  primary key (user_id, user_code)
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
            // drop index "user_id_user_code_idx"
            ddl.append(String.format("drop index %s;", toLiteral(oldIndex.getName()))).append(LINE);
        }
        if (newIndex != null) {
            // create unique index "user_id_user_code_idx" on sys_user2 (user_id, user_code)
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
        // drop index "user_id_user_code_idx"
        return String.format("drop index %s;", toLiteral(oldIndex.getName())) + LINE;
    }

    @Override
    public String createIndex(Index newIndex) {
        Assert.notNull(newIndex, "参数 newIndex 不能为空");
        final StringBuilder ddl = new StringBuilder();
        // create unique index "user_id_user_code_idx" on sys_user2 (user_id, user_code)
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
        String[] keywords = new String[]{
            "primary","unique",
        };
        if (StringUtils.equalsAnyIgnoreCase(objName, keywords)) {
            return "\"" + objName + "\"";
        }
        return objName;
    }

    protected String columnType(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        column = columnTypeMapping(column, DbType.ORACLE);
        String dataType = StringUtils.lowerCase(column.getDataType());
        final String[] noParamType = new String[]{
            "float", "binary_float", "binary_double",
            "clob", "nclob",
            "date",
            "blob", "long raw",
        };
        if (StringUtils.equalsAnyIgnoreCase(dataType, noParamType)) {
            return dataType;
        }
        // timestamp(6) with local time zone | varchar2(64) | number(10, 4)
        return super.columnType(column);
    }

    protected String defaultValue(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        return defaultValueMapping(column, DbType.ORACLE);
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
        // rename seq_app_version to seq_app_version2;
        if (!Objects.equals(oldSequence.getName(), newSequence.getName())) {
            ddl.append(String.format(
                "rename %s to %s;", toLiteral(oldSequence.getName()), newName
            )).append(LINE);
        }
        // alter sequence SEQ_APP_VERSION2 cache 30;
        // alter sequence SEQ_APP_VERSION2 cycle; nocycle
        // alter sequence SEQ_APP_VERSION2 order; noorder
        // alter sequence SEQ_APP_VERSION2 increment by 2 minvalue 0 maxvalue 10;
        if (!Objects.equals(oldSequence.isCycle(), newSequence.isCycle())) {
            ddl.append(String.format(
                "alter sequence %s %scycle;", newName, (newSequence.isCycle() ? " no" : "")
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
        // drop sequence seq_app_version;
        return String.format("drop sequence %s;%s", toLiteral(oldSequence.getName()), LINE);
    }

    @Override
    public String createSequence(Sequence newSequence) {
        Assert.notNull(newSequence, "参数 newSequence 不能为空");
        // create sequence aaa increment by 2 minvalue 0 maxvalue 10 order cycle cache 5;
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
        // drop procedure p_log;
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
