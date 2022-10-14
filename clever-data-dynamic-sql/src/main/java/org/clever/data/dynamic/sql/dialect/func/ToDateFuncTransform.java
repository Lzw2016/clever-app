package org.clever.data.dynamic.sql.dialect.func;

import org.clever.data.dynamic.sql.dialect.*;
import org.clever.data.dynamic.sql.dialect.exception.SqlFuncTransformException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/11 17:46 <br/>
 */
public class ToDateFuncTransform implements SqlFuncTransform {
    public static final String FUNC_NAME = "to_date";

    private static final Set<DbType> DB_TYPES = Collections.unmodifiableSet(new HashSet<DbType>() {{
        add(DbType.MYSQL);
        add(DbType.ORACLE);
        add(DbType.SQL_SERVER);
    }});

    @Override
    public String getFuncName() {
        return FUNC_NAME;
    }

    @Override
    public boolean isSupport(DbType dbType) {
        return DB_TYPES.contains(dbType);
    }

    @Override
    public SqlFuncNode transform(DbType dbType, SqlFuncNode src) {
        // to_date(param)
        final String funcName = src.getFuncName();
        final List<SqlFuncNodeParam> params = src.getParams();
        if (params == null || params.size() != 1) {
            throw new SqlFuncTransformException("SQL函数" + funcName + "参数错误");
        }
        SqlFuncNodeParam param = params.get(0).copy();
        SqlFuncNode target;
        if (DbType.MYSQL.equals(dbType)) {
            // #{param}
            target = new SqlFuncNode(null);
            target.setParen(false);
            target.getParams().add(param);
        } else if (DbType.ORACLE.equals(dbType)) {
            // TO_DATE(#{param}, 'YYYY-MM-DD')
            target = new SqlFuncNode("TO_DATE");
            target.getParams().add(param);
            SqlFuncParam sqlFuncParam = new SqlFuncParam(false, "'YYYY-MM-DD'", null, null);
            target.getParams().add(new SqlFuncNodeParam(sqlFuncParam));
        } else if (DbType.SQL_SERVER.equals(dbType)) {
            // CONVERT(datetime, #{param})
            target = new SqlFuncNode("CONVERT");
            SqlFuncParam sqlFuncParam = new SqlFuncParam(false, "datetime", null, null);
            target.getParams().add(new SqlFuncNodeParam(sqlFuncParam));
            target.getParams().add(param);
        } else {
            throw new SqlFuncTransformException("不支持的数据库:" + dbType);
        }
        return target;
    }
}
