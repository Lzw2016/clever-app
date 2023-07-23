package org.clever.data.jdbc.meta.inner;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.meta.model.Column;

/**
 * 不同数据库需要做类型映射 dataType size width decimalDigits
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

//    // 源库 | 目标库  | 源类型    | 转换条件 | 转换逻辑
//    // 源库 | DbType | dataType | 转换条件 | 转换逻辑
//    public static final Map<DbType, List<>>

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
            setColumnType("bigint", 64, 0, column);
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
        register(DbType.MYSQL, "year", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.MYSQL, "time", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("time", column);
        });
        register(DbType.MYSQL, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("date", column);
        });
        register(DbType.MYSQL, "datetime", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
            setColumnType("timestamp", column);
        });
        register(DbType.MYSQL, "timestamp", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
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
        register(DbType.MYSQL, "tinyint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "smallint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "mediumint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "int", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "integer", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "bigint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "decimal", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 字符串型
        register(DbType.MYSQL, "char", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "varchar", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "tinytext", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "text", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "mediumtext", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "longtext", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 日期和时间型
        register(DbType.MYSQL, "year", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "time", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "datetime", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "timestamp", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 布尔型
        register(DbType.MYSQL, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 二进制类型
        register(DbType.MYSQL, "bit", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "binary", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "varbinary", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "tinyblob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "blob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "mediumblob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.MYSQL, "longblob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
    }

    protected static void oracle2mysql() {
        final DbType targetDbType = DbType.MYSQL;
        // 数值型
        register(DbType.ORACLE, "number", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "integer", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "binary_float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "binary_double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "real", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 字符串型
        register(DbType.ORACLE, "char", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "varchar2", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "nchar", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "nvarchar2", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "clob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "nclob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 日期和时间型
        register(DbType.ORACLE, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "timestamp", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "timestamp with local time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "timestamp with time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "interval year to moth", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "interval day to second", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 布尔型
        register(DbType.ORACLE, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 二进制型
        register(DbType.ORACLE, "blob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "raw", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "long raw", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
    }

    protected static void oracle2postgresql() {
        final DbType targetDbType = DbType.POSTGRE_SQL;
        // 数值型
        register(DbType.ORACLE, "number", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "integer", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "binary_float", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "binary_double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "real", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 字符串型
        register(DbType.ORACLE, "char", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "varchar2", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "nchar", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "nvarchar2", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "clob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "nclob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 日期和时间型
        register(DbType.ORACLE, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "timestamp", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "timestamp with local time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "timestamp with time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "interval year to moth", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "interval day to second", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 布尔型
        register(DbType.ORACLE, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 二进制型
        register(DbType.ORACLE, "blob", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "raw", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.ORACLE, "long raw", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
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
        register(DbType.POSTGRE_SQL, "smallint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "integer", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "bigint", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "decimal", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "numeric", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "real", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "double", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "smallserial", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "serial", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "bigserial", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 字符串型
        register(DbType.POSTGRE_SQL, "character varying", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "varchar", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "character", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "char", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "text", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 日期和时间型
        register(DbType.POSTGRE_SQL, "timestamp", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "timestamp without time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "timestamp with time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "date", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "time", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "time without time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "time with time zone", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "interval", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        register(DbType.POSTGRE_SQL, "timestamptz", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 布尔型
        register(DbType.POSTGRE_SQL, "boolean", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
        });
        // 二进制型
        register(DbType.POSTGRE_SQL, "bytea", targetDbType, ALWAYS_TRUE, (dataType, size, width, decimalDigits, column) -> {
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

        // 默认映射
    }

// pg, mysql, oracle
// 整数,int4,int,number(10)
// 字符串,varchar,varchar,varchar2
// 长整型,int8,bigint,number(19)
// 大文本,varchar,text,clob
// 日期时间,timestamptz,datetime,timestamp(6) with local time zone
// ID,int8,bigint,number(19)
// 数量,numeric,decimal,number
// 长宽高体积重量,numeric,decimal,number
// 序号(行号),int4,int,number(10)
// 字典值,varchar,varchar,varchar2(50)
// 是否(启用),varchar,varchar,varchar2(50)
// 人名,varchar,varchar,varchar2
// 批属性,varchar,varchar,varchar2
// 金额,numeric,decimal,number
// 编号,varchar,varchar,varchar2
// 排层列巷道深度,varchar,varchar,varchar2
// 二进制文件,bytea,blob,blob
// 日期字符串,varchar,varchar,varchar2
// 是否,varchar,varchar,varchar2(50)
//数值型（Numeric Types）：
//MySQL: INT -> PostgreSQL: INTEGER -> Oracle: NUMBER
//MySQL: DECIMAL(p, s) -> PostgreSQL: DECIMAL(p, s) -> Oracle: NUMBER(p, s)
//MySQL: FLOAT -> PostgreSQL: REAL -> Oracle: FLOAT
//MySQL: DOUBLE -> PostgreSQL: DOUBLE PRECISION -> Oracle: DOUBLE PRECISION
//字符串型（Character Types）：
//MySQL: CHAR(size) -> PostgreSQL: CHAR(size) -> Oracle: CHAR(size)
//MySQL: VARCHAR(size) -> PostgreSQL: VARCHAR(size) -> Oracle: VARCHAR2(size)
//MySQL: TEXT -> PostgreSQL: TEXT -> Oracle: CLOB
//日期和时间型（Date and Time Types）：
//MySQL: DATE -> PostgreSQL: DATE -> Oracle: DATE
//MySQL: DATETIME -> PostgreSQL: TIMESTAMP -> Oracle: TIMESTAMP
//MySQL: TIMESTAMP -> PostgreSQL: TIMESTAMP -> Oracle: TIMESTAMP
//MySQL: TIME -> PostgreSQL: TIME -> Oracle: DATE
//布尔型（Boolean Type）：
//MySQL: BOOLEAN -> PostgreSQL: BOOLEAN -> Oracle: NUMBER(1)
//二进制型（Binary Types）：
//MySQL: BLOB -> PostgreSQL: BYTEA -> Oracle: BLOB
//JSON 类型：
//MySQL: JSON -> PostgreSQL: JSON -> Oracle: JSON

    public static void mysql(Column column) {
        String dataType = StringUtils.lowerCase(column.getDataType());
        int size = column.getSize();
        int width = column.getWidth();
        int decimalDigits = column.getDecimalDigits();
//数值型
//tinyint	1个字节
//smallint	2个字节
//mediumint	3个字节
//int(integer)	4个字节
//bigint	8个字节
//float	单精度浮点数	4个字节
//double	霜精度浮点数	8个字节
//decimal(m,d)	压缩的“严格”定点数	m+2个字节

//字符串型
//char(m)	固定长度非二进制字符串	m字节，1<=m<= 255
//varchar(m)	变长非二进制字符串	l+1字节，l<=m和1<=m<=255
//tinytext	非常小的非二进制字符串	l+1字节，在此l<2^8
//text	小的非二进制字符串	l+2字节，在此l<2^16
//mediumtext	中等大小的非二进制字符串	l+3字节，在此l<2^32
//longtext	大的非二进制字符串	l+4字节，在此l<2^32
//enum	枚举类型，只能存一个枚举字符串值	1或2个字节，取决于枚举值的数目（最大值65535）
//set	一个设置，字符串对象可以有零个或多个 set 成员	1、2、3、4或8个字节，取决于集合成员的数量（最多64个成员）

//日期和时间型
//year	yyyy	1901~2155	1个字节
//time	hh:mm:ss	-838:59:59~838:59:59	3个字节
//date	yyyy-mm-dd	1000-01-01~9999-12-3	3个字节
//datetime	yyyy-mm-dd hh:mm:ss	1000-01-01 00:00:00~9999-12-31 23:59:59	8个字节
//timestamp	yyyy-mm-dd hh:mm:ss	1970-01-01 00:00:01 utc ~2038-01-19 03:14:07 utc	4个字节

//布尔型
//boolean

//二进制类型
//bit(m)	位字段类型	大约(m+7)/8个字节
//binary(m)	固定长度二进制字符串	m个字节
//varbinary(m)	可变长度二进制字符串	m+1个字节
//tinyblob	非常小的blob	l+1个字节，l<2^8
//blob(m)	小的blob	l+2个字节，l<2^16
//mediumblob	中等大小的blob	l+3个字节，l<2^24
//longblob(m)	非常大的的blob	l+4个字节，l<2^32
    }

    public static void oracle(Column column) {
//数值型
//number(p, s)
//integer
//float(p) BINARY_FLOAT
//double precision
//binary_float
//binary_double
//real

//字符串型
//char(size)
//varchar2(size)
//nchar(size)
//nvarchar2(size)
//clob
//nclob

//日期和时间型
//date
//timestamp
//timestamp with local time zone
//timestamp with time zone
//interval year to moth
//interval day to second

//布尔型
//boolean

//二进制型
//clob
//nclob
//blob
//bfile 二进制文件，存储在数据库外的系统文件，只读的，数据库会将该文件当二进制文件处理
//raw(size) 用于存储二进制或字符类型数据，变长二进制数据类型，这说明采用这种数据类型存储的数据不会发生字符集转换。这种类型最多可以存储2,000字节的信息
//long raw 能存储2GB 的原始二进制数据（不用进行字符集转换的数据）
    }

    public static void postgresql(Column column) {
//数值型
//smallint	2 字节	小范围整数	-32768 到 +32767
//integer	4 字节	常用的整数	-2147483648 到 +2147483647
//bigint	8 字节	大范围整数	-9223372036854775808 到 +9223372036854775807
//decimal	可变长	用户指定的精度，精确	小数点前 131072 位；小数点后 16383 位
//numeric	可变长	用户指定的精度，精确	小数点前 131072 位；小数点后 16383 位
//real	4 字节	可变精度，不精确	6 位十进制数字精度
//double precision	8 字节	可变精度，不精确	15 位十进制数字精度
//smallserial	2 字节	自增的小范围整数	1 到 32767
//serial	4 字节	自增整数	1 到 2147483647
//bigserial	8 字节	自增的大范围整数	1 到 9223372036854775807

//字符串型
//character varying(n), varchar(n)	变长，有长度限制
//character(n), char(n)	定长，不足补空白
//text	变长，无长度限制

//日期和时间型
//timestamp [ (p) ] [ without time zone ]	8 字节	日期和时间(无时区)	4713 BC	294276 AD	1 毫秒 / 14 位
//timestamp [ (p) ] with time zone	8 字节	日期和时间，有时区	4713 BC	294276 AD	1 毫秒 / 14 位
//date	4 字节	只用于日期	4713 BC	5874897 AD	1 天
//time [ (p) ] [ without time zone ]	8 字节	只用于一日内时间	00:00:00	24:00:00	1 毫秒 / 14 位
//time [ (p) ] with time zone	12 字节	只用于一日内时间，带时区	00:00:00+1459	24:00:00-1459	1 毫秒 / 14 位
//interval [ fields ] [ (p) ]	12 字节	时间间隔	-178000000 年	178000000 年	1 毫秒 / 14 位
//timestamptz
//interval

//布尔型
//boolean	1 字节	真/假

//二进制型
//bytea	1或4字节加上实际的二进制字符串	变长的二进制字符串

    }
}
