package org.clever.data.jdbc.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.DateUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/11 07:28 <br/>
 */
@SuppressWarnings({"LoggingSimilarMessage", "DuplicatedCode"})
@Slf4j
public class SqlLoggerUtils {
    public static final String QUERYDSL_UPDATE_TOTAL = "UpdateTotal";

    private static final String LOG_SQL /*         */ = "==> ExecuteSQL: {}";
    private static final String LOG_PARAMETERS /*  */ = "==> Parameters: {}";
    private static final String LOG_BATCH_PARAM /* */ = "==> BatchParam: {}";
    private static final String LOG_TOTAL /*       */ = "<==      Total: {}";
    private static final String LOG_UPDATE_TOTAL /**/ = "<==    Updated: {}";
    private static final String LOG_PROCEDURE /*   */ = "==>  Procedure: {}";
    private static final String PROCEDURE_RESULT /**/ = "<==     Result: {}";
    /**
     * 忽略的包前缀
     */
    private static final List<String> IGNORE_PACKAGE_PREFIX = Arrays.asList("java.lang.", "java.util.", "java.sql.");

    /**
     * 打印SQL以及其参数
     *
     * @param sql    sql语句
     * @param params sql参数
     */
    public static void printfSql(String sql, List<Object> params) {
        if (!log.isDebugEnabled()) {
            return;
        }
        sql = deleteWhitespace(sql);
        log.debug(LOG_SQL, sql);
        if (params != null) {
            String paramMapStr = getParamListStr(params);
            log.debug(LOG_PARAMETERS, paramMapStr);
        }
    }

    /**
     * 打印SQL以及其参数
     *
     * @param sql    sql语句
     * @param params sql参数
     */
    public static void printfSql(String sql, Object[] params) {
        List<Object> paramList = Arrays.stream(params == null ? new Object[0] : params).collect(Collectors.toList());
        printfSql(sql, paramList);
    }

    /**
     * 打印SQL以及其参数
     *
     * @param sql      sql语句
     * @param paramMap sql参数
     */
    public static void printfSql(String sql, Map<String, ?> paramMap) {
        if (!log.isDebugEnabled()) {
            return;
        }
        sql = deleteWhitespace(sql);
        log.debug(LOG_SQL, sql);
        if (paramMap != null) {
            String paramMapStr = getParamMapStr(paramMap);
            log.debug(LOG_PARAMETERS, paramMapStr);
        }
    }

    /**
     * 打印SQL以及其参数
     *
     * @param sql          sql语句
     * @param paramMapList 参数数组
     */
    public static void printfSql(String sql, Collection<Map<String, Object>> paramMapList) {
        if (!log.isDebugEnabled()) {
            return;
        }
        sql = deleteWhitespace(sql);
        log.debug(LOG_SQL, sql);
        if (paramMapList != null) {
            for (Map<String, Object> paramMap : paramMapList) {
                String paramMapStr = getParamMapStr(paramMap);
                log.debug(LOG_BATCH_PARAM, paramMapStr);
            }
        }
    }

    /**
     * 打印SQL查询结果数据量
     *
     * @param total 查询结果数据量
     */
    public static void printfTotal(int total) {
        log.debug(LOG_TOTAL, total);
    }

    /**
     * 打印SQL查询结果数据量
     *
     * @param res 查询结果Object
     */
    public static void printfTotal(Object res) {
        if (res instanceof Collection) {
            Collection<?> resCollection = (Collection<?>) res;
            log.debug(LOG_TOTAL, resCollection.size());
        } else {
            log.debug(LOG_TOTAL, res == null ? 0 : 1);
        }
    }

    /**
     * 打印SQL更新数据量
     *
     * @param updateTotal 更新数据量
     */
    public static void printfUpdateTotal(int updateTotal) {
        log.debug(LOG_UPDATE_TOTAL, updateTotal);
    }

    /**
     * 打印Procedure以及其参数
     *
     * @param name     Procedure名称
     * @param paramMap Procedure参数
     */
    public static void printfProcedure(String name, Map<String, ?> paramMap) {
        if (!log.isDebugEnabled()) {
            return;
        }
        name = deleteWhitespace(name);
        log.debug(LOG_PROCEDURE, name);
        if (paramMap != null) {
            String paramMapStr = getParamMapStr(paramMap);
            log.debug(LOG_PARAMETERS, paramMapStr);
        }
    }

    /**
     * 打印Procedure以及其参数
     *
     * @param name   Procedure名称
     * @param params Procedure参数
     */
    public static void printfProcedure(String name, Object[] params) {
        List<Object> paramList = Arrays.stream(params == null ? new Object[0] : params).collect(Collectors.toList());
        if (!log.isDebugEnabled()) {
            return;
        }
        name = deleteWhitespace(name);
        log.debug(LOG_PROCEDURE, name);
        String paramMapStr = getParamListStr(paramList);
        log.debug(LOG_PARAMETERS, paramMapStr);
    }

    /**
     * 打印执行Procedure的返回值
     */
    public static void printfProcedureResult(Map<String, Object> res) {
        log.debug(PROCEDURE_RESULT, res);
    }

    /**
     * 打印SQL查询结果数据量
     *
     * @param totals 查询结果数据量
     */
    public static void printfUpdateTotal(int[] totals) {
        log.debug(LOG_UPDATE_TOTAL, Arrays.toString(totals));
    }

    private static String getParamListStr(List<Object> params) {
        if (params == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(params.size() * 15 + 32);
        for (Object param : params) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(paramToString(param));
        }
        return sb.toString();
    }

    private static String getParamMapStr(Map<String, ?> paramMap) {
        if (paramMap == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(paramMap.size() * 15 + 32);
        for (Map.Entry<String, ?> paramEntry : paramMap.entrySet()) {
            String name = paramEntry.getKey();
            Object value = paramEntry.getValue();
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name).append("=").append(paramToString(value));
        }
        return sb.toString();
    }

    private static String paramToString(Object param) {
        StringBuilder sb = new StringBuilder(16);
        if (param instanceof Date) {
            sb.append(DateUtils.formatToString((Date) param, "yyyy-MM-dd HH:mm:ssZ"));
        } else if (param != null && param.getClass().getName().startsWith("com.querydsl.sql.types.Null")) {
            sb.append("null");
        } else {
            sb.append(param);
        }
        if (param != null) {
            String valueType = param.getClass().getName();
            for (String packagePrefix : IGNORE_PACKAGE_PREFIX) {
                if (valueType.startsWith(packagePrefix)) {
                    valueType = param.getClass().getSimpleName();
                    break;
                }
            }
            if (!valueType.startsWith("com.querydsl.sql.types.Null")) {
                sb.append("(").append(valueType).append(")");
            }
        }
        return sb.toString();
    }

    /**
     * 删除多余的空白字符<br/>
     * <b>注意: 字符串参数中的空格也会被删除</b>
     */
    public static String deleteWhitespace(String sql) {
        if (sql == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(sql.length());
        boolean preIsWhitespace = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            boolean isWhitespace = Character.isWhitespace(ch);
            if (preIsWhitespace) {
                // 之前是空白字符
                if (isWhitespace) {
                    // 当前是空白字符
                    continue;
                } else {
                    // 当前非空白字符
                    sb.append(ch);
                }
            } else {
                // 之前非空白字符
                if (isWhitespace) {
                    // 当前是空白字符
                    sb.append(' ');
                } else {
                    // 当前非空白字符
                    sb.append(ch);
                }
            }
            preIsWhitespace = isWhitespace;
        }
        return sb.toString();
    }
}
