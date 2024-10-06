package org.clever.data.jdbc.support.features;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.SystemClock;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.Jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/08 20:52 <br/>
 */
@Slf4j
public class PostgreSQLFeatures extends DataBaseFeatures {
    /**
     * 模拟 waitSeconds 时的休眠时间间隔(单位毫秒)
     */
    public static int WAIT_PRECISION = 50;

    public PostgreSQLFeatures(Jdbc jdbc) {
        super(jdbc);
    }

    protected TupleTwo<Integer, Integer> getLockKey(String lockName) {
        return TupleTwo.creat(lockName.hashCode(), -1 * lockName.length());
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean getLock(String lockName) {
        checkLockName(lockName);
        TupleTwo<Integer, Integer> lockKey = getLockKey(lockName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockKey1", lockKey.getValue1());
        paramMap.put("lockKey2", lockKey.getValue2());
        jdbc.queryString("select pg_advisory_lock(:lockKey1, :lockKey2)", paramMap);
        return true;
    }

    @Override
    public boolean getLock(String lockName, int waitSeconds) {
        checkLockNameAndWait(lockName, waitSeconds);
        long now = SystemClock.now();
        final long waitEndTime = now + (waitSeconds * 1000L);
        Boolean locked;
        while (true) {
            TupleTwo<Integer, Integer> lockKey = getLockKey(lockName);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("lockKey1", lockKey.getValue1());
            paramMap.put("lockKey2", lockKey.getValue2());
            locked = jdbc.queryBoolean("select pg_try_advisory_lock(:lockKey1, :lockKey2)", paramMap);
            if (Objects.equals(locked, true)) {
                break;
            }
            try {
                // noinspection BusyWait
                Thread.sleep(WAIT_PRECISION);
            } catch (InterruptedException e) {
                log.warn("休眠等待失败", e);
            }
            now = SystemClock.now();
            if (now > waitEndTime) {
                // 已超时
                break;
            }
        }
        return Optional.ofNullable(locked).orElse(false);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean releaseLock(String lockName) {
        checkLockName(lockName);
        TupleTwo<Integer, Integer> lockKey = getLockKey(lockName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockKey1", lockKey.getValue1());
        paramMap.put("lockKey2", lockKey.getValue2());
        Boolean released = jdbc.queryBoolean("select pg_advisory_unlock(:lockKey1, :lockKey2)", paramMap);
        return Optional.ofNullable(released).orElse(false);
    }
}
