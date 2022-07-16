package org.clever.jdbc.core.namedparam;

import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.jdbc.core.SqlParameter;
import org.clever.jdbc.core.SqlParameterValue;
import org.clever.util.Assert;

import java.util.*;

/**
 * 用于命名参数解析的助手方法。
 *
 * <p>仅用于JDBC框架内的内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:51 <br/>
 */
public abstract class NamedParameterUtils {
    /**
     * 符合注释或引号起始字符条件的字符集。
     */
    private static final String[] START_SKIP = new String[]{"'", "\"", "--", "/*"};
    /**
     * 位于的字符集是相应的注释或引号结尾字符。
     */
    private static final String[] STOP_SKIP = new String[]{"'", "\"", "\n", "*/"};
    /**
     * 符合参数分隔符条件的字符集，指示SQL字符串中的参数名称已结束。
     */
    private static final String PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^";
    /**
     * 每个字符代码带有分隔符标志的索引。从技术上讲，此时只需要34到124。
     */
    private static final boolean[] separatorIndex = new boolean[128];

    static {
        for (char c : PARAMETER_SEPARATORS.toCharArray()) {
            separatorIndex[c] = true;
        }
    }

    //-------------------------------------------------------------------------
    // Core methods used by NamedParameterJdbcTemplate and SqlQuery/SqlUpdate
    //-------------------------------------------------------------------------

    /**
     * 解析SQL语句并定位任何占位符或命名参数。命名参数替换为JDBC占位符。
     *
     * @param sql SQL语句
     * @return 已解析的语句，表示为ParsedSql实例
     */
    public static ParsedSql parseSqlStatement(final String sql) {
        Assert.notNull(sql, "SQL must not be null");
        Set<String> namedParameters = new HashSet<>();
        StringBuilder sqlToUse = new StringBuilder(sql);
        List<ParameterHolder> parameterList = new ArrayList<>();
        char[] statement = sql.toCharArray();
        int namedParameterCount = 0;
        int unnamedParameterCount = 0;
        int totalParameterCount = 0;
        int escapes = 0;
        int i = 0;
        while (i < statement.length) {
            int skipToPosition;
            while (i < statement.length) {
                skipToPosition = skipCommentsAndQuotes(statement, i);
                if (i == skipToPosition) {
                    break;
                } else {
                    i = skipToPosition;
                }
            }
            if (i >= statement.length) {
                break;
            }
            char c = statement[i];
            if (c == ':' || c == '&') {
                int j = i + 1;
                if (c == ':' && j < statement.length && statement[j] == ':') {
                    // Postgres-style "::" casting operator should be skipped
                    i = i + 2;
                    continue;
                }
                String parameter;
                if (c == ':' && j < statement.length && statement[j] == '{') {
                    // :{x} style parameter
                    while (statement[j] != '}') {
                        j++;
                        if (j >= statement.length) {
                            throw new InvalidDataAccessApiUsageException(
                                    "Non-terminated named parameter declaration " + "at position "
                                            + i + " in statement: " + sql
                            );
                        }
                        if (statement[j] == ':' || statement[j] == '{') {
                            throw new InvalidDataAccessApiUsageException(
                                    "Parameter name contains invalid character '" + statement[j]
                                            + "' at position " + i + " in statement: " + sql
                            );
                        }
                    }
                    if (j - i > 2) {
                        parameter = sql.substring(i + 2, j);
                        namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter);
                        totalParameterCount = addNamedParameter(
                                parameterList, totalParameterCount, escapes, i, j + 1, parameter
                        );
                    }
                    j++;
                } else {
                    while (j < statement.length && !isParameterSeparator(statement[j])) {
                        j++;
                    }
                    if (j - i > 1) {
                        parameter = sql.substring(i + 1, j);
                        namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter);
                        totalParameterCount = addNamedParameter(
                                parameterList, totalParameterCount, escapes, i, j, parameter
                        );
                    }
                }
                i = j - 1;
            } else {
                if (c == '\\') {
                    int j = i + 1;
                    if (j < statement.length && statement[j] == ':') {
                        // escaped ":" should be skipped
                        sqlToUse.deleteCharAt(i - escapes);
                        escapes++;
                        i = i + 2;
                        continue;
                    }
                }
                if (c == '?') {
                    int j = i + 1;
                    if (j < statement.length && (statement[j] == '?' || statement[j] == '|' || statement[j] == '&')) {
                        // Postgres-style "??", "?|", "?&" operator should be skipped
                        i = i + 2;
                        continue;
                    }
                    unnamedParameterCount++;
                    totalParameterCount++;
                }
            }
            i++;
        }
        ParsedSql parsedSql = new ParsedSql(sqlToUse.toString());
        for (ParameterHolder ph : parameterList) {
            parsedSql.addNamedParameter(ph.getParameterName(), ph.getStartIndex(), ph.getEndIndex());
        }
        parsedSql.setNamedParameterCount(namedParameterCount);
        parsedSql.setUnnamedParameterCount(unnamedParameterCount);
        parsedSql.setTotalParameterCount(totalParameterCount);
        return parsedSql;
    }

    private static int addNamedParameter(List<ParameterHolder> parameterList,
                                         int totalParameterCount,
                                         int escapes,
                                         int i,
                                         int j,
                                         String parameter) {
        parameterList.add(new ParameterHolder(parameter, i - escapes, j - escapes));
        totalParameterCount++;
        return totalParameterCount;
    }

    private static int addNewNamedParameter(Set<String> namedParameters, int namedParameterCount, String parameter) {
        if (!namedParameters.contains(parameter)) {
            namedParameters.add(parameter);
            namedParameterCount++;
        }
        return namedParameterCount;
    }

    /**
     * 跳过SQL语句中的注释和引用名称。
     *
     * @param statement 包含SQL语句的字符数组
     * @param position  报表当前位置
     * @return 跳过任何评论或引用后要处理的下一个位置
     */
    private static int skipCommentsAndQuotes(char[] statement, int position) {
        for (int i = 0; i < START_SKIP.length; i++) {
            if (statement[position] == START_SKIP[i].charAt(0)) {
                boolean match = true;
                for (int j = 1; j < START_SKIP[i].length(); j++) {
                    if (statement[position + j] != START_SKIP[i].charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    int offset = START_SKIP[i].length();
                    for (int m = position + offset; m < statement.length; m++) {
                        if (statement[m] == STOP_SKIP[i].charAt(0)) {
                            boolean endMatch = true;
                            int endPos = m;
                            for (int n = 1; n < STOP_SKIP[i].length(); n++) {
                                if (m + n >= statement.length) {
                                    // last comment not closed properly
                                    return statement.length;
                                }
                                if (statement[m + n] != STOP_SKIP[i].charAt(n)) {
                                    endMatch = false;
                                    break;
                                }
                                endPos = m + n;
                            }
                            if (endMatch) {
                                // found character sequence ending comment or quote
                                return endPos + 1;
                            }
                        }
                    }
                    // character sequence ending comment or quote not found
                    return statement.length;
                }
            }
        }
        return position;
    }

    /**
     * 解析SQL语句并定位任何占位符或命名参数。
     * 命名参数被替换为JDBC占位符，任何选择列表都会扩展到所需数量的占位符。
     * 选择列表可能包含一组对象，在这种情况下，占位符将分组并用括号括起来。
     * 这允许在SQL语句中使用“expression lists”，例如：
     * <pre>{@code
     * select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))
     * }</pre>
     * <p>传入的参数值用于确定用于选择列表的占位符数量。
     * 选择列表应限制为100个或更少的元素。数据库不保证支持大量元素，并且严格依赖于供应商。
     *
     * @param parsedSql   SQL语句的解析表示形式
     * @param paramSource 命名参数的源
     * @return 带有替换参数的SQL语句
     * @see #parseSqlStatement
     */
    public static String substituteNamedParameters(ParsedSql parsedSql, SqlParameterSource paramSource) {
        String originalSql = parsedSql.getOriginalSql();
        List<String> paramNames = parsedSql.getParameterNames();
        if (paramNames.isEmpty()) {
            return originalSql;
        }
        StringBuilder actualSql = new StringBuilder(originalSql.length());
        int lastIndex = 0;
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            int[] indexes = parsedSql.getParameterIndexes(i);
            int startIndex = indexes[0];
            int endIndex = indexes[1];
            actualSql.append(originalSql, lastIndex, startIndex);
            if (paramSource != null && paramSource.hasValue(paramName)) {
                Object value = paramSource.getValue(paramName);
                if (value instanceof SqlParameterValue) {
                    value = ((SqlParameterValue) value).getValue();
                }
                if (value instanceof Iterable) {
                    Iterator<?> entryIter = ((Iterable<?>) value).iterator();
                    int k = 0;
                    while (entryIter.hasNext()) {
                        if (k > 0) {
                            actualSql.append(", ");
                        }
                        k++;
                        Object entryItem = entryIter.next();
                        if (entryItem instanceof Object[]) {
                            Object[] expressionList = (Object[]) entryItem;
                            actualSql.append('(');
                            for (int m = 0; m < expressionList.length; m++) {
                                if (m > 0) {
                                    actualSql.append(", ");
                                }
                                actualSql.append('?');
                            }
                            actualSql.append(')');
                        } else {
                            actualSql.append('?');
                        }
                    }
                } else {
                    actualSql.append('?');
                }
            } else {
                actualSql.append('?');
            }
            lastIndex = endIndex;
        }
        actualSql.append(originalSql, lastIndex, originalSql.length());
        return actualSql.toString();
    }

    /**
     * 将命名参数值的Map转换为相应的数组。
     *
     * @param parsedSql      已解析的SQL语句
     * @param paramSource    命名参数的源
     * @param declaredParams 声明的SqlParameter对象列表（可能为空）。如果指定，参数元数据将以SqlParameterValue对象的形式构建到值数组中。
     * @return 值的数组
     */
    public static Object[] buildValueArray(ParsedSql parsedSql, SqlParameterSource paramSource, List<SqlParameter> declaredParams) {
        Object[] paramArray = new Object[parsedSql.getTotalParameterCount()];
        if (parsedSql.getNamedParameterCount() > 0 && parsedSql.getUnnamedParameterCount() > 0) {
            throw new InvalidDataAccessApiUsageException(
                    "Not allowed to mix named and traditional ? placeholders. You have "
                            + parsedSql.getNamedParameterCount() + " named parameter(s) and "
                            + parsedSql.getUnnamedParameterCount() + " traditional placeholder(s) in statement: "
                            + parsedSql.getOriginalSql());
        }
        List<String> paramNames = parsedSql.getParameterNames();
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            try {
                SqlParameter param = findParameter(declaredParams, paramName, i);
                Object paramValue = paramSource.getValue(paramName);
                if (paramValue instanceof SqlParameterValue) {
                    paramArray[i] = paramValue;
                } else {
                    paramArray[i] = (param != null ?
                            new SqlParameterValue(param, paramValue) :
                            SqlParameterSourceUtils.getTypedValue(paramSource, paramName)
                    );
                }
            } catch (IllegalArgumentException ex) {
                throw new InvalidDataAccessApiUsageException(
                        "No value supplied for the SQL parameter '" + paramName + "': " + ex.getMessage()
                );
            }
        }
        return paramArray;
    }

    /**
     * 在给定的已声明参数列表中查找匹配参数。
     *
     * @param declaredParams 声明的SqlParameter对象
     * @param paramName      所需参数的名称
     * @param paramIndex     所需参数的索引
     * @return 声明的SqlParameter，如果未找到，则为null
     */
    private static SqlParameter findParameter(List<SqlParameter> declaredParams, String paramName, int paramIndex) {
        if (declaredParams != null) {
            // First pass: Look for named parameter match.
            for (SqlParameter declaredParam : declaredParams) {
                if (paramName.equals(declaredParam.getName())) {
                    return declaredParam;
                }
            }
            // Second pass: Look for parameter index match.
            if (paramIndex < declaredParams.size()) {
                SqlParameter declaredParam = declaredParams.get(paramIndex);
                // Only accept unnamed parameters for index matches.
                if (declaredParam.getName() == null) {
                    return declaredParam;
                }
            }
        }
        return null;
    }

    /**
     * 确定参数名称是否以当前位置结束，即给定字符是否符合分隔符的条件。
     */
    private static boolean isParameterSeparator(char c) {
        return (c < 128 && separatorIndex[c]) || Character.isWhitespace(c);
    }

    /**
     * 将参数类型从SqlParameterSource转换为相应的int数组。
     * 为了重用JdbcTemplate上的现有方法，这是必要的。
     * 根据解析的SQL语句信息，将任何命名参数类型放置在对象数组中的正确位置。
     *
     * @param parsedSql   已解析的SQL语句
     * @param paramSource 命名参数的源
     */
    public static int[] buildSqlTypeArray(ParsedSql parsedSql, SqlParameterSource paramSource) {
        int[] sqlTypes = new int[parsedSql.getTotalParameterCount()];
        List<String> paramNames = parsedSql.getParameterNames();
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            sqlTypes[i] = paramSource.getSqlType(paramName);
        }
        return sqlTypes;
    }

    /**
     * 将参数声明从SqlParameterSource转换为相应的SqlParameters列表。
     * 为了重用JdbcTemplate上的现有方法，这是必要的。
     * 根据解析的SQL语句信息，将命名参数的SqlParameter放置在结果列表中的正确位置。
     *
     * @param parsedSql   已解析的SQL语句
     * @param paramSource 命名参数的源
     */
    public static List<SqlParameter> buildSqlParameterList(ParsedSql parsedSql, SqlParameterSource paramSource) {
        List<String> paramNames = parsedSql.getParameterNames();
        List<SqlParameter> params = new ArrayList<>(paramNames.size());
        for (String paramName : paramNames) {
            params.add(new SqlParameter(
                    paramName,
                    paramSource.getSqlType(paramName),
                    paramSource.getTypeName(paramName)
            ));
        }
        return params;
    }

    //-------------------------------------------------------------------------
    // Convenience methods operating on a plain SQL String
    //-------------------------------------------------------------------------

    /**
     * 解析SQL语句并定位任何占位符或命名参数。命名参数替换为JDBC占位符。
     * <p>这是{@link #parseSqlStatement(String)}的快捷版本，
     * 与{@link #substituteNamedParameters(ParsedSql, SqlParameterSource)}结合使用。
     *
     * @param sql the SQL statement
     * @return the actual (parsed) SQL statement
     */
    public static String parseSqlStatementIntoString(String sql) {
        ParsedSql parsedSql = parseSqlStatement(sql);
        return substituteNamedParameters(parsedSql, null);
    }

    /**
     * 解析SQL语句并定位任何占位符或命名参数。命名参数被替换为JDBC占位符，任何选择列表都会扩展到所需数量的占位符。
     * <p>这是的快捷版本 {@link #substituteNamedParameters(ParsedSql, SqlParameterSource)}.
     *
     * @param sql         SQL语句
     * @param paramSource 命名参数的源
     * @return 带有替换参数的SQL语句
     */
    public static String substituteNamedParameters(String sql, SqlParameterSource paramSource) {
        ParsedSql parsedSql = parseSqlStatement(sql);
        return substituteNamedParameters(parsedSql, paramSource);
    }

    /**
     * 将命名参数值的Map转换为相应的数组。
     * <p>这是的快捷版本 {@link #buildValueArray(ParsedSql, SqlParameterSource, java.util.List)}.
     *
     * @param sql      SQL语句
     * @param paramMap 参数Map
     * @return 值的数组
     */
    public static Object[] buildValueArray(String sql, Map<String, ?> paramMap) {
        ParsedSql parsedSql = parseSqlStatement(sql);
        return buildValueArray(parsedSql, new MapSqlParameterSource(paramMap), null);
    }

    private static class ParameterHolder {
        private final String parameterName;
        private final int startIndex;
        private final int endIndex;

        public ParameterHolder(String parameterName, int startIndex, int endIndex) {
            this.parameterName = parameterName;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public String getParameterName() {
            return this.parameterName;
        }

        public int getStartIndex() {
            return this.startIndex;
        }

        public int getEndIndex() {
            return this.endIndex;
        }
    }
}
