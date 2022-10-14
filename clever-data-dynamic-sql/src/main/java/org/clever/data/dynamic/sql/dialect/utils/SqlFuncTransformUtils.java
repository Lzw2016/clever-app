package org.clever.data.dynamic.sql.dialect.utils;

import org.clever.data.dynamic.sql.dialect.*;
import org.clever.data.dynamic.sql.dialect.exception.SqlFuncTransformAlreadyExistsException;
import org.clever.data.dynamic.sql.utils.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/21 18:57 <br/>
 */
public class SqlFuncTransformUtils {
    /**
     * {@code Map<FuncName, List<SqlFuncTransform>>}
     */
    private static final ConcurrentMap<String, CopyOnWriteArrayList<SqlFuncTransform>> TRANSFORMS_MAP = new ConcurrentHashMap<>();

    /**
     * 注册SqlFuncTransform
     */
    public static void register(SqlFuncTransform sqlFuncTransform) {
        register(sqlFuncTransform, true);
    }

    /**
     * 注册SqlFuncTransform
     */
    public static void register(SqlFuncTransform sqlFuncTransform, boolean skipExists) {
        if (sqlFuncTransform == null) {
            return;
        }
        CopyOnWriteArrayList<SqlFuncTransform> transforms = TRANSFORMS_MAP.computeIfAbsent(sqlFuncTransform.getFuncName(), funcName -> new CopyOnWriteArrayList<>());
        if (transforms.stream().anyMatch(transform -> Objects.equals(transform.getClass(), sqlFuncTransform.getClass()))) {
            if (skipExists) {
                return;
            }
            throw new SqlFuncTransformAlreadyExistsException("当前SqlFuncTransform类型已存在，class=" + sqlFuncTransform.getClass().getName());
        }
        transforms.add(sqlFuncTransform);
    }

    /**
     * 获取 SqlFuncTransform
     *
     * @param funcName SQL函数名称
     * @param dbType   数据库类型
     */
    public static SqlFuncTransform getTransform(String funcName, DbType dbType) {
        CopyOnWriteArrayList<SqlFuncTransform> transforms = TRANSFORMS_MAP.get(funcName);
        if (transforms == null || transforms.isEmpty()) {
            return null;
        }
        for (SqlFuncTransform transform : transforms) {
            if (transform.isSupport(dbType)) {
                return transform;
            }
        }
        return null;
    }

    /**
     * 递归读取SqlFuncNode的sql变量
     */
    public static LinkedHashMap<String, Object> getSqlVariable(SqlFuncNode sqlFunc) {
        LinkedHashMap<String, Object> sqlVariable = new LinkedHashMap<>();
        for (SqlFuncNodeParam param : sqlFunc.getParams()) {
            if (SqlFuncNodeParamEnum.SQL_FUNC.equals(param.getType())) {
                SqlFuncNode childSqlFunc = param.getFunc();
                sqlVariable.putAll(getSqlVariable(childSqlFunc));
            } else {
                SqlFuncParam sqlFuncParam = param.getParam();
                if (sqlFuncParam.isVariable()) {
                    String name = sqlFuncParam.getName();
                    Object value = sqlFuncParam.getValue();
                    sqlVariable.put(name, value);
                }
            }
        }
        return sqlVariable;
    }

    /**
     * 递归转换SqlFuncNode
     */
    public static SqlFuncNode transform(SqlFuncNode src, DbType dbType) {
        SqlFuncNode target;
        final String sqlFuncName = src.getFuncName();
        SqlFuncTransform sqlFuncTransform = getTransform(sqlFuncName, dbType);
        if (sqlFuncTransform == null) {
            target = src.copy();
        } else {
            target = sqlFuncTransform.transform(dbType, src);
        }
        final List<SqlFuncNodeParam> params = target.getParams();
        for (int idx = 0; idx < params.size(); idx++) {
            SqlFuncNodeParam param = params.get(idx);
            if (SqlFuncNodeParamEnum.SQL_FUNC.equals(param.getType())) {
                SqlFuncNodeParam newParam = new SqlFuncNodeParam(transform(param.getFunc(), dbType));
                params.set(idx, newParam);
            }
        }
        return target;
    }

    /**
     * 递归获取转换后的sql函数代码 -- int uniqueNumber = 0;
     */
    public static String toSql(SqlFuncNode sqlFunc) {
        final String sqlFuncName = sqlFunc.getFuncName();
        final List<SqlFuncNodeParam> params = sqlFunc.getParams();
        final boolean paren = StringUtils.Instance.isNotBlank(sqlFuncName) || sqlFunc.isParen();
        StringBuilder sql = new StringBuilder();
        if (StringUtils.Instance.isNotBlank(sqlFuncName)) {
            sql.append(sqlFuncName);
        }
        if (paren) {
            sql.append("(");
        }
        for (int idx = 0; idx < params.size(); idx++) {
            SqlFuncNodeParam param = params.get(idx);
            if (idx > 0) {
                sql.append(", ");
            }
            if (SqlFuncNodeParamEnum.SQL_FUNC.equals(param.getType())) {
                sql.append(toSql(param.getFunc()));
            } else {
                SqlFuncParam sqlFuncParam = param.getParam();
                if (sqlFuncParam.isVariable()) {
                    sql.append("#{").append(sqlFuncParam.getName()).append("}");
                } else {
                    String literal = sqlFuncParam.getLiteral();
                    literal = StringUtils.Instance.trim(literal);
                    if (literal.startsWith("\"") && literal.endsWith("\"")) {
                        literal = "'" + literal.substring(1, literal.length() - 1) + "'";
                    }
                    sql.append(literal);
                }
            }
        }
        if (paren) {
            sql.append(")");
        }
        return sql.toString();
    }
}
