package org.clever.data.jdbc.support.features;

import lombok.Getter;
import org.clever.core.Assert;
import org.clever.data.jdbc.Jdbc;

/**
 * 数据库特性
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/08 20:14 <br/>
 */
public abstract class DataBaseFeatures {
    public static int MAX_WAIT_SECONDS = 60 * 30;
    @Getter
    protected final Jdbc jdbc;

    public DataBaseFeatures(Jdbc jdbc) {
        Assert.notNull(jdbc, "参数 jdbc 不能为null");
        this.jdbc = jdbc;
    }

    protected void checkLockName(String lockName) {
        Assert.isNotBlank(lockName, "参数 lockName 不能为空");
    }

    protected void checkLockNameAndWait(String lockName, int waitSeconds) {
        Assert.isNotBlank(lockName, "参数 lockName 不能为空");
        Assert.isTrue(waitSeconds > 0 && waitSeconds <= MAX_WAIT_SECONDS, "参数 waitSeconds 取值必须在：1 ~ " + MAX_WAIT_SECONDS + "范围内");
    }

    /**
     * 阻塞一直到获取锁为止
     */
    public abstract boolean getLock(String lockName);

    /**
     * 阻塞一直到获取锁为止(可以设置超时时间)
     */
    public abstract boolean getLock(String lockName, int waitSeconds);

    /**
     * 释放锁
     */
    public abstract boolean releaseLock(String lockName);
}
