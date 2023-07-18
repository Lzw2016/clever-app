package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;

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
        // 查询表信息
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    a.owner                                 as \"schemaName\", ");
        sql.append("    a.table_name                            as \"tableName\", ");
        sql.append("    b.comments                              as \"comment\" ");
        sql.append("from sys.all_tables a left join sys.all_tab_comments b on (a.owner = b.owner and a.table_name = b.table_name) ");
        sql.append("where a.iot_type is null ");
        if (!schemasName.isEmpty()) {
            sql.append("  and lower(a.owner) in (").append(createWhereIn(params, schemasName)).append(")");
        }
        if (!tablesName.isEmpty()) {
            sql.append("  and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(")");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("  and lower(a.owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(")");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("  and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(")");
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
            sql.append("  and lower(a.owner) in (").append(createWhereIn(params, schemasName)).append(")");
        }
        if (!tablesName.isEmpty()) {
            sql.append("  and lower(a.table_name) in (").append(createWhereIn(params, tablesName)).append(")");
        }
        if (!ignoreSchemas.isEmpty()) {
            sql.append("  and lower(a.owner) not in (").append(createWhereIn(params, ignoreSchemas)).append(")");
        }
        if (!ignoreTables.isEmpty()) {
            sql.append("  and lower(a.table_name) not in (").append(createWhereIn(params, ignoreTables)).append(")");
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
            Schema schema = mapSchema.get(schemaName);
            if (schema == null) {
                continue;
            }
            String key = String.format("schemaName=%s|type=%s|name=%s", schemaName, type, name);
            TupleTwo<StringBuilder, Procedure> tuple = routineMap.get(key);
            if (tuple == null) {
                tuple = TupleTwo.creat(new StringBuilder(), new Procedure(schema));
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
            tuple.getValue2().setDefinition(tuple.getValue1().toString());
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
    public String diffTable(Table newTable, Table oldTable) {
        return null;
    }

    @Override
    public String delTable(Table oldTable) {
        return null;
    }

    @Override
    public String addTable(Table newTable) {
        return null;
    }

    @Override
    public String diffColumn(Column newColumn, Column oldColumn) {
        return null;
    }

    @Override
    public String delColumn(Column oldColumn) {
        return null;
    }

    @Override
    public String addColumn(Column oldColumn) {
        return null;
    }

    @Override
    public String diffPrimaryKey(PrimaryKey newPrimaryKey, PrimaryKey oldPrimaryKey) {
        return null;
    }

    @Override
    public String delPrimaryKey(PrimaryKey oldPrimaryKey) {
        return null;
    }

    @Override
    public String addPrimaryKey(PrimaryKey newPrimaryKey) {
        return null;
    }

    @Override
    public String diffIndex(Index newIndex, Index oldIndex) {
        return null;
    }

    @Override
    public String delIndex(Index oldIndex) {
        return null;
    }

    @Override
    public String addIndex(Index newIndex) {
        return null;
    }
}
