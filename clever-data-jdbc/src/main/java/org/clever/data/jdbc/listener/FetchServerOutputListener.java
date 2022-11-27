package org.clever.data.jdbc.listener;

import org.clever.core.Ordered;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.jdbc.core.ConnectionCallback;
import org.clever.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.Objects;

/**
 * TODO OracleLog
 * 作者：lizw <br/>
 * 创建时间：2022/03/03 16:32 <br/>
 */
public class FetchServerOutputListener implements JdbcListener {
    private final Logger logger = LoggerFactory.getLogger("db_proc");

    public final static String ENABLE_OUTPUT = "begin dbms_output.enable(); end;";
    public final static String FETCH_OUTPUT = "declare " +
            "    num integer := 1000; " +
            "begin " +
            "    dbms_output.get_lines(?, num); " +
            "    dbms_output.disable(); " +
            "end;";

    @Override
    public void beforeExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate) {
        if (!Objects.equals(dbType, DbType.ORACLE)) {
            return;
        }
//        if (!OracleLog.isEnable()) {
//            return;
//        }
        jdbcTemplate.update(ENABLE_OUTPUT, Collections.emptyMap());
    }

    @Override
    public void afterExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate, Exception exception) {
        if (!Objects.equals(dbType, DbType.ORACLE)) {
            return;
        }
//        if (!OracleLog.isEnable()) {
//            return;
//        }
        jdbcTemplate.getJdbcTemplate().execute((ConnectionCallback<Void>) con -> {
            try (CallableStatement call = con.prepareCall(FETCH_OUTPUT)) {
                call.registerOutParameter(1, Types.ARRAY, "DBMSOUTPUT_LINESARRAY");
                call.execute();
                Array array = call.getArray(1);
                if (array != null && array.getArray() != null) {
                    Object[] arr = (Object[]) array.getArray();
                    for (Object row : arr) {
                        if (row == null) {
                            continue;
                        }
                        logger.info("{}", row);
//                        OracleLog.append(row);
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
