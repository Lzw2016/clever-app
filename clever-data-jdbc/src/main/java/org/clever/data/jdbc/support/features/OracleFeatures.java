package org.clever.data.jdbc.support.features;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.support.ProcedureJdbcCall;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/08 21:39 <br/>
 */
@Slf4j
public class OracleFeatures extends DataBaseFeatures {
    protected final SimpleJdbcCall allocateUnique;
    protected final SimpleJdbcCall request;
    protected final SimpleJdbcCall requestWithoutTimeout;
    protected final SimpleJdbcCall release;

    public OracleFeatures(Jdbc jdbc) {
        super(jdbc);
        // dbms_lock.allocate_unique(lockname => lock_name, lockhandle => lock_handle);
        allocateUnique = new ProcedureJdbcCall(jdbc)
            .withoutProcedureColumnMetaDataAccess()
            .withCatalogName("dbms_lock")
            .withProcedureName("allocate_unique")
            .withNamedBinding()
            .declareParameters(
                new SqlParameter("lockname", Types.VARCHAR),
                new SqlOutParameter("lockhandle", Types.VARCHAR)
            );
        allocateUnique.compile();
        // lock_result := dbms_lock.request(
        //     lockhandle => lock_handle,
        //     lockmode => dbms_lock.x_mode,
        //     timeout => 3,
        //     release_on_commit => true
        // );
        // dbms_lock.x_mode 说明：
        //      ss_mode     2   对象的子部分，加上了Share锁
        //      sx_mode     3   对象的子部分，加上了Exclusive锁
        //      s_mode      4   对象加上了Share锁
        //      ssx_mode    5   对象加上了Share锁，其子部分加上了Exclusive锁
        //      x_mode      6   对象加上了Exclusive锁
        // 返回值：
        //      0   成功
        //      1   超时
        //      2   死锁
        //      3   参数错误
        //      4   已经拥有该锁
        //      5   无效的lock_handle
        // 注意：dbms_lock.request 函数的release_on_commit参数只能在下面代码中使用:
        //      DECLARE
        //          lock_result number;
        //      BEGIN
        //      lock_result := dbms_lock.request(
        //          lockhandle => '123',
        //          lockmode => dbms_lock.x_mode,
        //          timeout => 3,
        //          release_on_commit => true
        //      );
        //      dbms_output.put_line('lock_result=' || lock_result);
        //      END;
        request = new ProcedureJdbcCall(jdbc)
            .withoutProcedureColumnMetaDataAccess()
            .withCatalogName("dbms_lock")
            .withFunctionName("request")
            .withNamedBinding()
            .declareParameters(
                new SqlOutParameter("result", Types.INTEGER),
                new SqlParameter("lockhandle", Types.VARCHAR),
                new SqlParameter("lockmode", Types.INTEGER),
                new SqlParameter("timeout", Types.INTEGER)
                // new SqlParameter("release_on_commit", Types.BOOLEAN)
            );
        request.compile();
        requestWithoutTimeout = new ProcedureJdbcCall(jdbc)
            .withoutProcedureColumnMetaDataAccess()
            .withCatalogName("dbms_lock")
            .withFunctionName("request")
            .withNamedBinding()
            .declareParameters(
                new SqlOutParameter("result", Types.INTEGER),
                new SqlParameter("lockhandle", Types.VARCHAR),
                new SqlParameter("lockmode", Types.INTEGER)
            );
        requestWithoutTimeout.compile();
        // dbms_lock.release(lock_handle);
        // 返回值：
        //      0   成功
        //      3   参数错误
        //      4   不拥有lock_handle指定的锁
        //      5   非法锁lock_handle
        release = new ProcedureJdbcCall(jdbc)
            .withoutProcedureColumnMetaDataAccess()
            .withCatalogName("dbms_lock")
            .withFunctionName("release")
            .withNamedBinding()
            .declareParameters(
                new SqlOutParameter("result", Types.INTEGER),
                new SqlParameter("lockhandle", Types.VARCHAR)
            );
        release.compile();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean getLock(String lockName) {
        checkLockName(lockName);
        String lockHandle = getLockHandle(lockName);
        if (StringUtils.isBlank(lockHandle)) {
            return false;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockhandle", lockHandle);
        paramMap.put("lockmode", 6);
        Integer locked = requestWithoutTimeout.executeFunction(Integer.class, paramMap);
        // 返回值：
        //      0   成功
        //      1   超时
        //      2   死锁
        //      3   参数错误
        //      4   已经拥有该锁
        //      5   无效的lock_handle
        return Objects.equals(locked, 0) || Objects.equals(locked, 4);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean getLock(String lockName, int waitSeconds) {
        checkLockNameAndWait(lockName, waitSeconds);
        String lockHandle = getLockHandle(lockName);
        if (StringUtils.isBlank(lockHandle)) {
            return false;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockhandle", lockHandle);
        paramMap.put("lockmode", 6);
        paramMap.put("timeout", waitSeconds);
        Integer locked = request.executeFunction(Integer.class, paramMap);
        // 返回值：
        //      0   成功
        //      1   超时
        //      2   死锁
        //      3   参数错误
        //      4   已经拥有该锁
        //      5   无效的lock_handle
        return Objects.equals(locked, 0) || Objects.equals(locked, 4);
    }

    @Override
    public boolean releaseLock(String lockName) {
        checkLockName(lockName);
        String lockHandle = getLockHandle(lockName);
        if (StringUtils.isBlank(lockHandle)) {
            return false;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockhandle", lockHandle);
        Integer released = release.executeFunction(Integer.class, paramMap);
        // 返回值：
        //      0   成功
        //      3   参数错误
        //      4   不拥有lock_handle指定的锁
        //      5   非法锁lock_handle
        return Objects.equals(released, 0) || Objects.equals(released, 4);
    }

    protected String getLockHandle(String lockName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockname", lockName);
        Map<String, Object> res = allocateUnique.execute(paramMap);
        return Conv.asString(res.get("lockhandle"), null);
    }
}
