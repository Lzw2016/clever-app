package org.clever.data.jdbc.meta.inner;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.meta.model.Column;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 不同数据库需要做映射 defaultValue
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/20 17:57 <br/>
 */
@SuppressWarnings({"CodeBlock2Expr", "DuplicatedCode"})
public class DefaultValueMapping {
    @FunctionalInterface
    public interface SupportConvert {
        boolean support(String defaultValue);
    }

    @FunctionalInterface
    public interface TypeConvert {
        String convert(String defaultValue, Column column);
    }

    public static final TypeConvert NONE_CONVERT = (defaultValue, column) -> defaultValue;

    // Map<srcDbType, Map<targetDbType, List<Tuple<support, convert>>>>
    public static final ConcurrentMap<DbType, ConcurrentMap<DbType, List<TupleTwo<SupportConvert, TypeConvert>>>> CONVERT_MAP = new ConcurrentHashMap<>();

    public static void register(DbType dbType, DbType targetDbType, SupportConvert support, TypeConvert convert) {
        ConcurrentMap<DbType, List<TupleTwo<SupportConvert, TypeConvert>>> level_1 = CONVERT_MAP.computeIfAbsent(dbType, type -> new ConcurrentHashMap<>());
        List<TupleTwo<SupportConvert, TypeConvert>> level_2 = level_1.computeIfAbsent(targetDbType, type -> new LinkedList<>());
        level_2.add(TupleTwo.creat(support, convert));
    }

    protected static void mysql2postgresql() {
        final DbType targetDbType = DbType.POSTGRE_SQL;
        register(DbType.MYSQL, targetDbType, defaultValue -> defaultValue.startsWith("current_timestamp"), (defaultValue, column) -> {
            return "now()";
        });
    }

    protected static void mysql2oracle() {
        final DbType targetDbType = DbType.POSTGRE_SQL;
        register(DbType.MYSQL, targetDbType, defaultValue -> defaultValue.startsWith("current_timestamp"), (defaultValue, column) -> {
            return "sysdate";
        });
    }

    protected static void oracle2mysql() {
        final DbType targetDbType = DbType.MYSQL;
        register(DbType.ORACLE, targetDbType, defaultValue -> defaultValue.startsWith("sysdate"), (defaultValue, column) -> {
            return "current_timestamp(3)";
        });
    }

    protected static void oracle2postgresql() {
        final DbType targetDbType = DbType.POSTGRE_SQL;
        register(DbType.ORACLE, targetDbType, defaultValue -> defaultValue.startsWith("sysdate"), (defaultValue, column) -> {
            return "now()";
        });
    }

    protected static void postgresql2mysql() {
        final DbType targetDbType = DbType.MYSQL;
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("now()"), (defaultValue, column) -> {
            return "current_timestamp(3)";
        });
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("current_timestamp"), (defaultValue, column) -> {
            return "current_timestamp(3)";
        });
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("localtimestamp"), (defaultValue, column) -> {
            return "current_timestamp(3)";
        });
        // nextval('auto_increment_id_id_seq'::regclass)
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.contains("::") && defaultValue.endsWith(")"), (defaultValue, column) -> {
            return "";
        });
        // '-1'::integer | NULL::numeric
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.contains("::") && !defaultValue.endsWith(")") && !defaultValue.endsWith("'"), (defaultValue, column) -> {
            return defaultValue.substring(0, defaultValue.indexOf("::"));
        });
    }

    protected static void postgresql2oracle() {
        final DbType targetDbType = DbType.MYSQL;
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("now()"), (defaultValue, column) -> {
            return "sysdate";
        });
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("current_timestamp"), (defaultValue, column) -> {
            return "sysdate";
        });
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("localtimestamp"), (defaultValue, column) -> {
            return "sysdate";
        });
        // nextval('auto_increment_id_id_seq'::regclass)
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.startsWith("nextval("), (defaultValue, column) -> {
            return "";
        });
        // '-1'::integer | NULL::numeric
        register(DbType.POSTGRE_SQL, targetDbType, defaultValue -> defaultValue.contains("::") && !defaultValue.endsWith(")") && !defaultValue.endsWith("'"), (defaultValue, column) -> {
            return defaultValue.substring(0, defaultValue.indexOf("::"));
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
        ConcurrentMap<DbType, List<TupleTwo<SupportConvert, TypeConvert>>> level_1 = CONVERT_MAP.get(column.getTable().getSchema().getDbType());
        if (level_1 == null) {
            return NONE_CONVERT;
        }
        List<TupleTwo<SupportConvert, TypeConvert>> level_2 = level_1.get(targetDbType);
        if (level_2 == null || level_2.isEmpty()) {
            return NONE_CONVERT;
        }
        String defaultValue = StringUtils.lowerCase(StringUtils.trimToEmpty(column.getDefaultValue()));
        for (TupleTwo<SupportConvert, TypeConvert> two : level_2) {
            SupportConvert support = two.getValue1();
            TypeConvert convert = two.getValue2();
            if (convert != null && support != null && support.support(defaultValue)) {
                return convert;
            }
        }
        return NONE_CONVERT;
    }

    protected static String defaultValue(String defaultValue) {
        return StringUtils.trim(defaultValue);
    }

    public static String mysql(Column column) {
        String defaultValue = StringUtils.lowerCase(StringUtils.trimToEmpty(column.getDefaultValue()));
        TypeConvert convert = getConvert(column, DbType.MYSQL);
        defaultValue = convert.convert(defaultValue, column);
        return defaultValue(defaultValue);
    }

    public static String oracle(Column column) {
        String defaultValue = StringUtils.lowerCase(StringUtils.trimToEmpty(column.getDefaultValue()));
        TypeConvert convert = getConvert(column, DbType.ORACLE);
        defaultValue = convert.convert(defaultValue, column);
        return defaultValue(defaultValue);
    }

    public static String postgresql(Column column) {
        String defaultValue = StringUtils.lowerCase(StringUtils.trimToEmpty(column.getDefaultValue()));
        TypeConvert convert = getConvert(column, DbType.POSTGRE_SQL);
        defaultValue = convert.convert(defaultValue, column);
        return defaultValue(defaultValue);
    }
}
