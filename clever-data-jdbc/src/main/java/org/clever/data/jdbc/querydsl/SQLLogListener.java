package org.clever.data.jdbc.querydsl;

import com.querydsl.sql.SQLBaseListener;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLListenerContext;
import org.clever.core.Conv;
import org.clever.data.jdbc.support.SqlLoggerUtils;

import java.util.Collection;

/**
 * 打印执行SQL日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/21 22:32 <br/>
 */
public class SQLLogListener extends SQLBaseListener {
    @Override
    public void preExecute(SQLListenerContext context) {
        // 打印SQL see com.querydsl.sql.AbstractSQLQuery.logQuery
        Collection<SQLBindings> allSqlBindings = context.getAllSQLBindings();
        if (allSqlBindings != null && !allSqlBindings.isEmpty()) {
            for (SQLBindings sqlBindings : allSqlBindings) {
                SqlLoggerUtils.printfSql(sqlBindings.getSQL(), sqlBindings.getNullFriendlyBindings());
            }
        }
    }

    @Override
    public void executed(SQLListenerContext context) {
        Integer updateTotal = Conv.asInteger(context.getData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL), null);
        if (updateTotal != null) {
            SqlLoggerUtils.printfUpdateTotal(updateTotal);
        }
    }
}
