package org.clever.data.jdbc.listener;

import org.clever.core.Conv;
import org.clever.core.Ordered;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.jdbc.core.ConnectionCallback;
import org.clever.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.Objects;

/**
 * 获取Oracle dbms_output输出数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/03/03 16:32 <br/>
 */
public class OracleDbmsOutputListener implements JdbcListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public final static String ENABLE_OUTPUT = "begin dbms_output.enable(); end;";
    public final static String FETCH_OUTPUT = "declare " +
            "    num integer := 1000; " +
            "begin " +
            "    dbms_output.get_lines(?, num); " +
            "    dbms_output.disable(); " +
            "end;";

    /**
     * 是否启用收集SQLWarning输出(支持Oracle的dbms_output输出)
     */
    private final ThreadLocal<Boolean> enableSqlWarning;
    /**
     * 收集SQLWarning输出的数据缓冲区
     */
    private final ThreadLocal<StringBuilder> sqlWarningBuffer;

    public OracleDbmsOutputListener(ThreadLocal<Boolean> enableSqlWarning, ThreadLocal<StringBuilder> sqlWarningBuffer) {
        Assert.notNull(enableSqlWarning, "参数 enableSqlWarning 不能为空");
        Assert.notNull(sqlWarningBuffer, "参数 sqlWarningBuffer 不能为空");
        this.enableSqlWarning = enableSqlWarning;
        this.sqlWarningBuffer = sqlWarningBuffer;
    }

    @Override
    public void beforeExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate) {
        if (!Objects.equals(dbType, DbType.ORACLE) || !Objects.equals(enableSqlWarning.get(), true)) {
            return;
        }
        jdbcTemplate.update(ENABLE_OUTPUT, Collections.emptyMap());
    }

    @Override
    public void afterExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate, Exception exception) {
        if (!Objects.equals(dbType, DbType.ORACLE) || !Objects.equals(enableSqlWarning.get(), true)) {
            return;
        }
        jdbcTemplate.getJdbcTemplate().execute((ConnectionCallback<Void>) con -> {
            try (CallableStatement call = con.prepareCall(FETCH_OUTPUT)) {
                call.registerOutParameter(1, Types.ARRAY, "DBMSOUTPUT_LINESARRAY");
                call.execute();
                Array array = call.getArray(1);
                if (array != null && array.getArray() != null) {
                    Object[] arr = (Object[]) array.getArray();
                    for (Object row : arr) {
                        String message = Conv.asString(row, null);
                        if (message == null) {
                            continue;
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("dbms_output: {}", message);
                        }
                        // 收集 dbms_output
                        StringBuilder output = sqlWarningBuffer.get();
                        if (output == null) {
                            synchronized (sqlWarningBuffer) {
                                output = sqlWarningBuffer.get();
                                if (output == null) {
                                    output = new StringBuilder();
                                    sqlWarningBuffer.set(output);
                                }
                            }
                        }
                        output.append(message);
                        if (!message.endsWith("\n")) {
                            output.append("\n");
                        }
                    }
                    array.free();
                }
            }
            return null;
        });
    }

    @Override
    public double getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
