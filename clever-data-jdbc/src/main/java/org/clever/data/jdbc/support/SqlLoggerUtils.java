package org.clever.data.jdbc.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.DateUtils;

import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/11 07:28 <br/>
 */
@Slf4j
public class SqlLoggerUtils {
    private static final String Log_Sql /*         */ = "==> ExecuteSQL: {}";
    private static final String Log_Parameters /*  */ = "==> Parameters: {}";
    private static final String Log_Batch_Param /* */ = "==> BatchParam: {}";
    private static final String Log_Total /*       */ = "<==      Total: {}";
    private static final String Log_Update_Total /**/ = "<==    Updated: {}";
    /**
     * 忽略的包前缀
     */
    private static final List<String> Ignore_Package_Prefix = Arrays.asList("java.lang.", "java.util.", "java.sql.");

    /**
     * 打印SQL以及其参数
     *
     * @param sql    sql语句
     * @param params sql参数
     */
    public static void printfSql(String sql, List<Object> params) {
        // noinspection DuplicatedCode
        if (!log.isDebugEnabled()) {
            return;
        }
        sql = deleteWhitespace(sql);
        log.debug(Log_Sql, sql);
        if (params != null) {
            String paramMapStr = getParamMapStr(params);
            log.debug(Log_Parameters, paramMapStr);
        }
    }

    /**
     * 打印SQL以及其参数
     *
     * @param sql      sql语句
     * @param paramMap sql参数
     */
    public static void printfSql(String sql, Map<String, Object> paramMap) {
        // noinspection DuplicatedCode
        if (!log.isDebugEnabled()) {
            return;
        }
        sql = deleteWhitespace(sql);
        log.debug(Log_Sql, sql);
        if (paramMap != null) {
            String paramMapStr = getParamMapStr(paramMap);
            log.debug(Log_Parameters, paramMapStr);
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
        log.debug(Log_Sql, sql);
        if (paramMapList != null) {
            for (Map<String, Object> paramMap : paramMapList) {
                String paramMapStr = getParamMapStr(paramMap);
                log.debug(Log_Batch_Param, paramMapStr);
            }
        }
    }

    /**
     * 打印SQL查询结果数据量
     *
     * @param total 查询结果数据量
     */
    public static void printfTotal(int total) {
        log.debug(Log_Total, total);
    }

    /**
     * 打印SQL查询结果数据量
     *
     * @param res 查询结果Object
     */
    public static void printfTotal(Object res) {
        if (res instanceof Collection) {
            Collection<?> resCollection = (Collection<?>) res;
            log.debug(Log_Total, resCollection.size());
        } else {
            log.debug(Log_Total, res == null ? 0 : 1);
        }
    }

    /**
     * 打印SQL更新数据量
     *
     * @param updateTotal 更新数据量
     */
    public static void printfUpdateTotal(int updateTotal) {
        log.debug(Log_Update_Total, updateTotal);
    }

    /**
     * 打印SQL查询结果数据量
     *
     * @param totals 查询结果数据量
     */
    public static void printfUpdateTotal(int[] totals) {
        log.debug(Log_Update_Total, Arrays.toString(totals));
    }

    private static String getParamMapStr(List<Object> params) {
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

    private static String getParamMapStr(Map<String, Object> paramMap) {
        if (paramMap == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(paramMap.size() * 15 + 32);
        for (Map.Entry<String, Object> paramEntry : paramMap.entrySet()) {
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
            for (String packagePrefix : Ignore_Package_Prefix) {
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
