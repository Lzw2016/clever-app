//package org.clever.data.jdbc;
//
//import org.clever.core.Conv;
//import org.clever.core.tuples.TupleTwo;
//import org.clever.data.dynamic.sql.dialect.DbType;
//import org.clever.data.jdbc.support.OracleLog;
//import org.clever.data.jdbc.support.ProcedureJdbcCall;
//import org.clever.data.jdbc.support.SqlUtils;
//import lombok.Getter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.simple.SimpleJdbcCall;
//import org.springframework.util.Assert;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/03 16:56 <br/>
// */
//@Slf4j
//public class Dao {
//    /**
//     * <pre>{@code
//     * 缓存调用存储过程调用对象(提高性能~20倍)
//     * Map<dataSourceName@procedure_name, SimpleJdbcCall>
//     * 参考: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/simple/SimpleJdbcCall.html
//     * }</pre>
//     */
//    private final static ConcurrentMap<String, SimpleJdbcCall> JDBC_CALL_CACHE = new ConcurrentHashMap<>();
//
//    @Getter
//    private final MyBatis myBatis;
//
//    public Dao(MyBatis myBatis) {
//        Assert.notNull(myBatis, "参数myBatis不能为空");
//        this.myBatis = myBatis;
//    }
//
//    public Jdbc getJdbc() {
//        return myBatis.getJdbc();
//    }
//
//    /**
//     * 获取序列的下一个值
//     *
//     * @param seqName 序列名称
//     * @return 序列的下一个值
//     */
//    public long nextId(String seqName) {
//        Map<String, Object> paramMap = new HashMap<>(1);
//        paramMap.put("seqName", seqName);
//        if (Objects.equals(getJdbc().getDbType(), DbType.POSTGRE_SQL)) {
//            return getJdbc().queryLong("select next_id(:seqName)", paramMap);
//        } else if (Objects.equals(getJdbc().getDbType(), DbType.MYSQL)) {
//            Map<String, Object> data = callGet("next_id", seqName, "", 1);
//            return Conv.asLong(data.get("p_current_value"));
//        } else {
//            return getJdbc().queryLong("select next_id(:seqName) from dual", paramMap);
//        }
//    }
//
//    /**
//     * 获取分布式id 的下一个值
//     */
//    public long snowId() {
//        return this.snowId("N_A");
//    }
//
//    /**
//     * 获取分布式id 的下一个值
//     *
//     * @param seqName 序列名称
//     * @return 分布式ID的下一个值
//     */
//    public long snowId(String seqName) {
//        Map<String, Object> paramMap = new HashMap<>(1);
//        paramMap.put("seqName", seqName);
//        if (Objects.equals(getJdbc().getDbType(), DbType.POSTGRE_SQL)) {
//            return getJdbc().queryLong("select snow_id(:seqName)", paramMap);
//        } else if (Objects.equals(getJdbc().getDbType(), DbType.MYSQL)) {
//            Map<String, Object> data = callGet("snow_id", seqName);
//            return Conv.asLong(data.get("p_snow_id"));
//        } else {
//            return getJdbc().queryLong("select snow_id(:seqName) from dual", paramMap);
//        }
//    }
//
//    public long lotGet(long itemId, String... lotValue) {
//        Map<String, Object> paramMap = new HashMap<>();
//        if (Objects.equals(getJdbc().getDbType(), DbType.POSTGRE_SQL)) {
//            paramMap.put("i_itemId", itemId);
//            paramMap.put("lot", lotValue);
//            return getJdbc().queryLong("select lot_get(:i_itemId,:lot)", paramMap);
//        } else {
//            String[] paramList = new String[lotValue.length + 1];
//            paramList[0] = itemId + "";
//            System.arraycopy(lotValue, 0, paramList, 1, lotValue.length);
//            Map<String, Object> ret = this.callGet("LOT_GET", (Object[]) paramList);
//            return DbConv.asLong(ret.get("LOT_ID"));
//        }
//    }
//
//    /**
//     * 获取对应ID规则的下一个值
//     *
//     * @param idName 规则名称
//     * @return 对应ID规则的下一个值
//     */
//    public Object nextCode(String idName) {
//        Map<String, Object> paramMap = new HashMap<>(1);
//        paramMap.put("idName", idName);
//        if (Objects.equals(getJdbc().getDbType(), DbType.POSTGRE_SQL)) {
//            return getJdbc().queryString("select next_code(:idName)", paramMap);
//        } else {
//            return getJdbc().queryString("select next_code(:idName) from dual", paramMap);
//        }
//    }
//
//    /**
//     * 借助数据库进行排他锁, 锁的名称必须在 sys_lock 表中存在
//     */
//    public void lock(String lockName) {
//        this.call("lock_it", lockName);
//    }
//
//    /**
//     * 执行存储过程
//     *
//     * @param procedureName 存贮过程名称
//     */
//    public void call(String procedureName) {
//        call(procedureName, Collections.emptyList());
//    }
//
//    /**
//     * 执行存储过程
//     *
//     * @param procedureName 存贮过程名称
//     * @param params        参数
//     */
//    public void call(String procedureName, Object... params) {
//        try {
//            OracleLog.enable();
//            TupleTwo<String, Map<String, Object>> sqlInfo = SqlUtils.getCallSql(procedureName, Arrays.asList(params));
//            this.getJdbc().update(sqlInfo.getValue1(), sqlInfo.getValue2());
//        } finally {
//            OracleLog.disable();
//        }
//    }
//
//    // 调用函数
//    // public Map<String, Object> callFuc(String fucName) {
//    //     SimpleJdbcCall jdbcCall = new ProcedureJdbcCall(getJdbc())
//    //             .withFunctionName(fucName)
//    //             .withNamedBinding();
//    //     MapSqlParameterSource sqlParameter = new MapSqlParameterSource();
//    //     return jdbcCall.execute(sqlParameter);
//    // }
//
//    /**
//     * 执行存储过程
//     *
//     * @param procedureName 存贮过程名称
//     */
//    public Map<String, Object> callGet(String procedureName) {
//        return callGet(procedureName, new HashMap<>());
//    }
//
//    /**
//     * 执行存储过程
//     *
//     * @param procedureName 存贮过程名称
//     * @param params        参数
//     */
//    public Map<String, Object> callGet(String procedureName, List<?> params) {
//        return callGet(procedureName, params.toArray());
//    }
//
//    /**
//     * 执行存储过程
//     *
//     * @param procedureName 存贮过程名称
//     * @param params        参数
//     */
//    public Map<String, Object> callGet(final String procedureName, Object... params) {
//        try {
//            final Jdbc jdbc = getJdbc();
//            OracleLog.enable();
//            SimpleJdbcCall jdbcCall = JDBC_CALL_CACHE.computeIfAbsent(
//                    String.format("%s@%s", jdbc.getDataSourceName(), procedureName),
//                    key -> new ProcedureJdbcCall(jdbc)
//                            .withProcedureName(procedureName)
//            );
//            return jdbcCall.execute(params);
//        } finally {
//            OracleLog.disable();
//        }
//    }
//
//    /**
//     * 执行存储过程
//     *
//     * @param procedureName 存贮过程名称
//     * @param paramMap      参数
//     */
//    public Map<String, Object> callGet(final String procedureName, Map<String, ?> paramMap) {
//        try {
//            final Jdbc jdbc = getJdbc();
//            OracleLog.enable();
//            SimpleJdbcCall jdbcCall = JDBC_CALL_CACHE.computeIfAbsent(
//                    String.format("%s@%s", jdbc.getDataSourceName(), procedureName),
//                    key -> new ProcedureJdbcCall(jdbc)
//                            .withProcedureName(procedureName)
//                            .withNamedBinding()
//            );
//            MapSqlParameterSource sqlParameter = new MapSqlParameterSource(paramMap);
//            return jdbcCall.execute(sqlParameter);
//        } finally {
//            OracleLog.disable();
//        }
//    }
//}
