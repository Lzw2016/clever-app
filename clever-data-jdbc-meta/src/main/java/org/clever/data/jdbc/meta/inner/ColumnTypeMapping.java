package org.clever.data.jdbc.meta.inner;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.meta.model.Column;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 对于数据库的表字段类型(dataType size width decimalDigits)不同数据库需要做映射
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/20 12:01 <br/>
 */
@SuppressWarnings("CodeBlock2Expr")
public class ColumnTypeMapping {
    @FunctionalInterface
    public interface SupportConvert {
        /**
         * @param dataType      字段类型
         * @param size          字段大小(char、date、numeric、decimal)
         * @param width         小数位数
         * @param decimalDigits string类型的长度
         * @param column        字段信息
         */
        boolean support(String dataType, int size, int width, int decimalDigits, Column column);
    }

    @FunctionalInterface
    public interface TypeConvert {
        void convert(String dataType, int size, int width, int decimalDigits, Column column);
    }

    public static final SupportConvert ALWAYS_TRUE = (dataType, size, width, decimalDigits, column) -> true;
    public static final TypeConvert NONE_CONVERT = (dataType, size, width, decimalDigits, column) -> {
    };

    // Map<srcDbType, Map<targetDbType, Map<dataType, List<Tuple<support, convert>>>>>
    public static final ConcurrentMap<DbType, ConcurrentMap<DbType, ConcurrentMap<String, LinkedList<TupleTwo<SupportConvert, TypeConvert>>>>> CONVERT_MAP = new ConcurrentHashMap<>();

    /**
     * 注册数据库类型转换逻辑
     *
     * @param dbType       源库类型
     * @param dataType     源字段类型
     * @param targetDbType 目标库类型
     * @param support      是否支持转换
     * @param convert      转换逻辑
     */
    public static void register(DbType dbType, String dataType, DbType targetDbType, SupportConvert support, TypeConvert convert) {
        ConcurrentMap<DbType, ConcurrentMap<String, LinkedList<TupleTwo<SupportConvert, TypeConvert>>>> level_1 = CONVERT_MAP.computeIfAbsent(dbType, type -> new ConcurrentHashMap<>());
        ConcurrentMap<String, LinkedList<TupleTwo<SupportConvert, TypeConvert>>> level_2 = level_1.computeIfAbsent(targetDbType, type -> new ConcurrentHashMap<>());
        LinkedList<TupleTwo<SupportConvert, TypeConvert>> level_3 = level_2.computeIfAbsent(StringUtils.lowerCase(dataType), type -> new LinkedList<>());
        level_3.add(TupleTwo.creat(support, convert));
    }

    /**
     * 注册数据库类型转换逻辑
     *
     * @param dbType       源库类型
     * @param dataTypes    源字段类型数组
     * @param targetDbType 目标库类型
     * @param support      是否支持转换
     * @param convert      转换逻辑
     */
    public static void register(DbType dbType, String[] dataTypes, DbType targetDbType, SupportConvert support, TypeConvert convert) {
        for (String dataType : dataTypes) {
            register(dbType, dataType, targetDbType, support, convert);
        }
    }

    /**
     * 数字类型
     */
    protected static void setColumnType(String dataType, int size, int decimalDigits, Column column) {
        column.setDataType(dataType);
        column.setSize(size);
        column.setDecimalDigits(decimalDigits);
        column.setWidth(0);
    }

    /**
     * 字符类型
     */
    protected static void setColumnType(String dataType, int width, Column column) {
        column.setDataType(dataType);
        column.setWidth(width);
        column.setSize(0);
        column.setDecimalDigits(0);
    }

    /**
     * 时间、boolean、二进制等其它类型
     */
    protected static void setColumnType(String dataType, Column column) {
        column.setDataType(dataType);
        column.setWidth(0);
    }

    protected static void mysql2postgresql() {
        final DbType targetDbType = DbType.POSTGRE_SQL;
        // 数值型
        register(DbType.MYSQL, new String[]{"tinyint", "smallint"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("int2", 16, 0, column);
        });
        register(DbType.MYSQL, new String[]{"mediumint", "int", "integer"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("int4", 32, 0, column);
        });
        register(DbType.MYSQL, "bigint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("int8", 64, 0, column);
        });
        register(DbType.MYSQL, "float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("float4", 24, 0, column);
        });
        register(DbType.MYSQL, "double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("float8", 53, 0, column);
        });
        register(DbType.MYSQL, "decimal", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("numeric", column.getSize(), column.getDecimalDigits(), column);
        });
        // 字符串型
        register(DbType.MYSQL, "char", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bpchar", column.getWidth(), column);
        });
        register(DbType.MYSQL, "varchar", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar", column.getWidth(), column);
        });
        register(DbType.MYSQL, new String[]{"tinytext", "text", "mediumtext", "longtext"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("text", 0, column);
        });
        // 日期和时间型
        register(DbType.MYSQL, new String[]{"year", "date"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.MYSQL, "time", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("time", column);
        });
        register(DbType.MYSQL, new String[]{"datetime", "timestamp"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp", column);
        });
        // 布尔型
        register(DbType.MYSQL, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bool", column);
        });
        // 二进制类型
        register(DbType.MYSQL, new String[]{"bit", "binary", "varbinary", "tinyblob", "blob", "mediumblob", "longblob"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bytea", column);
        });
    }

    protected static void mysql2oracle() {
        final DbType targetDbType = DbType.ORACLE;
        // 数值型
        register(DbType.MYSQL, new String[]{"tinyint", "smallint", "mediumint", "int", "integer"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", 11, 0, column);
        });
        register(DbType.MYSQL, "bigint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", 19, 0, column);
        });
        register(DbType.MYSQL, "float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("binary_float", 4, column);
        });
        register(DbType.MYSQL, "double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("binary_double", 8, column);
        });
        register(DbType.MYSQL, "decimal", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", column.getSize(), column.getDecimalDigits(), column);
        });
        // 字符串型
        register(DbType.MYSQL, "char", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("char", column.getWidth(), column);
        });
        register(DbType.MYSQL, "varchar", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar2", column.getWidth(), column);
        });
        register(DbType.MYSQL, "tinytext", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar2", 255, column);
        });
        register(DbType.MYSQL, new String[]{"text", "mediumtext", "longtext"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("clob", 4000, column);
        });
        // 日期和时间型
        register(DbType.MYSQL, new String[]{"year", "time", "date"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.MYSQL, new String[]{"datetime", "timestamp"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp(6)", column);
        });
        // 布尔型
        register(DbType.MYSQL, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", 1, 0, column);
        });
        // 二进制类型
        register(DbType.MYSQL, new String[]{"bit", "binary", "varbinary", "tinyblob", "blob", "mediumblob", "longblob"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("blob", column);
        });
    }

    protected static void oracle2mysql() {
        final DbType targetDbType = DbType.MYSQL;
        // 数值型
        register(DbType.ORACLE, "number", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("decimal", column.getSize(), column.getDecimalDigits(), column);
        });
        register(DbType.ORACLE, "integer", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("int", 10, 0, column);
        });
        register(DbType.ORACLE, new String[]{"float", "binary_float", "real"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("float", 12, 0, column);
        });
        register(DbType.ORACLE, new String[]{"double", "binary_double"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("double", 22, 0, column);
        });
        // 字符串型
        register(DbType.ORACLE, new String[]{"char", "nchar"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("char", column.getWidth(), column);
        });
        register(DbType.ORACLE, new String[]{"varchar2", "nvarchar2"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar", column.getWidth(), column);
        });
        register(DbType.ORACLE, new String[]{"clob", "nclob"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("longtext", column);
        });
        // 日期和时间型
        register(DbType.ORACLE, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.ORACLE, new String[]{"timestamp(6)", "timestamp", "timestamp(6) with local time zone", "timestamp with local time zone", "timestamp(6) with time zone", "timestamp with time zone"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("datetime", column);
        });
        // 布尔型
        register(DbType.ORACLE, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("tinyint", 3, 0, column);
        });
        // 二进制型
        register(DbType.ORACLE, new String[]{"blob", "raw", "long raw"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("longblob", column);
        });
    }

    protected static void oracle2postgresql() {
        final DbType targetDbType = DbType.POSTGRE_SQL;
        // 数值型
        register(DbType.ORACLE, "number", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("numeric", column);
        });
        register(DbType.ORACLE, "integer", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("int4", column);
        });
        register(DbType.ORACLE, new String[]{"float", "binary_float", "real"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("float4", column);
        });
        register(DbType.ORACLE, new String[]{"double", "binary_double"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("float8", column);
        });
        // 字符串型
        register(DbType.ORACLE, new String[]{"char", "nchar"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bpchar", column.getWidth(), column);
        });
        register(DbType.ORACLE, new String[]{"varchar2", "nvarchar2"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar", column.getWidth(), column);
        });
        register(DbType.ORACLE, new String[]{"clob", "nclob"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("text", column);
        });
        // 日期和时间型
        register(DbType.ORACLE, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.ORACLE, new String[]{"timestamp(6)", "timestamp"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp", column);
        });
        register(DbType.ORACLE, new String[]{"timestamp(6) with local time zone", "timestamp(6) with time zone", "timestamp with local time zone", "timestamp with time zone"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamptz", column);
        });
        // 布尔型
        register(DbType.ORACLE, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bool", column);
        });
        // 二进制型
        register(DbType.ORACLE, new String[]{"blob", "raw", "long raw"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bytea", column);
        });
    }

    protected static void postgresql2mysql() {
        final DbType targetDbType = DbType.MYSQL;
        // 数值型
        register(DbType.POSTGRE_SQL, new String[]{"int2", "smallint", "smallserial"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("smallint", 5, 0, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"int4", "integer", "serial"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("int", 10, 0, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"int8", "bigint", "bigserial"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("bigint", 19, 0, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"decimal", "numeric"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("decimal", column.getSize(), column.getDecimalDigits(), column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"float4", "real"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("float", 12, 0, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"double", "float8", "double precision"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("double", 22, 0, column);
        });
        // 字符串型
        register(DbType.POSTGRE_SQL, new String[]{"varchar", "character varying"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar", column.getWidth(), column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"bpchar", "character", "char"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("char", column.getWidth(), column);
        });
        register(DbType.POSTGRE_SQL, "text", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("longtext", column);
        });
        // 日期和时间型
        register(DbType.POSTGRE_SQL, new String[]{"timestamp", "timestamptz", "timestamp without time zone", "timestamp with time zone"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp", column);
        });
        register(DbType.POSTGRE_SQL, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"time", "timetz", "time without time zone", "time with time zone"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("time", column);
        });
        // 布尔型
        register(DbType.POSTGRE_SQL, new String[]{"bool", "boolean"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("tinyint", 1, 0, column);
        });
        // 二进制型
        register(DbType.POSTGRE_SQL, "bytea", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("longblob", column);
        });
    }

    protected static void postgresql2oracle() {
        final DbType targetDbType = DbType.ORACLE;
        // 数值型
        register(DbType.POSTGRE_SQL, new String[]{"int2", "smallint", "smallserial", "int4", "integer", "serial"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", 11, 0, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"int8", "bigint", "bigserial"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", 19, 0, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"decimal", "numeric"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", column.getSize(), column.getDecimalDigits(), column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"float4", "real"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("binary_float", 4, column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"double", "float8", "double precision"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("binary_double", 8, column);
        });
        // 字符串型
        register(DbType.POSTGRE_SQL, new String[]{"varchar", "character varying"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("varchar2", column.getWidth(), column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"bpchar", "character", "char"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("char", column.getWidth(), column);
        });
        register(DbType.POSTGRE_SQL, "text", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("clob", column);
        });
        // 日期和时间型
        register(DbType.POSTGRE_SQL, new String[]{"timestamp", "timestamp without time zone", "time", "time without time zone"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp(6)", column);
        });
        register(DbType.POSTGRE_SQL, new String[]{"timestamptz", "timestamp with time zone", "timetz", "time with time zone"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp(6) with time zone", column);
        });
        register(DbType.POSTGRE_SQL, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        // 布尔型
        register(DbType.POSTGRE_SQL, new String[]{"bool", "boolean"}, targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("number", 1, 0, column);
        });
        // 二进制型
        register(DbType.POSTGRE_SQL, "bytea", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("blob", column);
        });
    }

    static {
        // mysql -> postgresql
        mysql2postgresql();
        // mysql -> oracle
        mysql2oracle();

        // oracle -> mysql
        oracle2mysql();
        // oracle -> postgresql
        oracle2postgresql();

        // postgresql -> mysql
        postgresql2mysql();
        // postgresql -> oracle
        postgresql2oracle();
    }

    protected static TypeConvert getConvert(Column column, DbType targetDbType) {
        String dataType = StringUtils.lowerCase(column.getDataType());
        int size = column.getSize();
        int width = column.getWidth();
        int decimalDigits = column.getDecimalDigits();
        ConcurrentMap<DbType, ConcurrentMap<String, LinkedList<TupleTwo<SupportConvert, TypeConvert>>>> level_1 = CONVERT_MAP.get(column.getTable().getSchema().getDbType());
        if (level_1 == null) {
            return NONE_CONVERT;
        }
        ConcurrentMap<String, LinkedList<TupleTwo<SupportConvert, TypeConvert>>> level_2 = level_1.get(targetDbType);
        if (level_2 == null) {
            return NONE_CONVERT;
        }
        LinkedList<TupleTwo<SupportConvert, TypeConvert>> level_3 = level_2.get(dataType);
        if (level_3 == null || level_3.isEmpty()) {
            return NONE_CONVERT;
        }
        for (TupleTwo<SupportConvert, TypeConvert> two : level_3) {
            SupportConvert support = two.getValue1();
            TypeConvert convert = two.getValue2();
            if (convert != null && support != null && support.support(dataType, size, width, decimalDigits, column)) {
                return convert;
            }
        }
        return NONE_CONVERT;
    }

    // --------------------------------------------------------------------------------------------
    // 提供外部使用的函数
    // --------------------------------------------------------------------------------------------

    public static void mysql(Column column) {
        TypeConvert convert = getConvert(column, DbType.MYSQL);
        convert.convert(column.getDataType(), column.getSize(), column.getWidth(), column.getDecimalDigits(), column);
    }

    public static void oracle(Column column) {
        TypeConvert convert = getConvert(column, DbType.ORACLE);
        convert.convert(column.getDataType(), column.getSize(), column.getWidth(), column.getDecimalDigits(), column);
    }

    public static void postgresql(Column column) {
        TypeConvert convert = getConvert(column, DbType.POSTGRE_SQL);
        convert.convert(column.getDataType(), column.getSize(), column.getWidth(), column.getDecimalDigits(), column);
    }
}
