package org.clever.data.jdbc.support.features;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.data.jdbc.Jdbc;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/08 20:15 <br/>
 */
@Slf4j
public class MySQLFeatures extends DataBaseFeatures {
    public MySQLFeatures(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public boolean getLock(String lockName) {
        checkLockName(lockName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockName", lockName);
        Long locked = jdbc.queryLong("select get_lock(:lockName, -1) from dual", paramMap);
        return Conv.asBoolean(locked, false);
    }

    @Override
    public boolean getLock(String lockName, int waitSeconds) {
        checkLockNameAndWait(lockName, waitSeconds);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockName", lockName);
        paramMap.put("waitSeconds", waitSeconds);
        Long locked = jdbc.queryLong("select get_lock(:lockName, :waitSeconds) from dual", paramMap);
        return Conv.asBoolean(locked, false);
    }

    @Override
    public boolean releaseLock(String lockName) {
        checkLockName(lockName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockName", lockName);
        Long released = jdbc.queryLong("select release_lock(:lockName) from dual", paramMap);
        return Conv.asBoolean(released, false);
    }
}
