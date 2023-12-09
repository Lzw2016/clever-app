package org.clever.data.jdbc.support.features;

import org.clever.data.jdbc.Jdbc;
import org.clever.util.Assert;

/**
 * 数据库特性
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/08 20:14 <br/>
 */
public abstract class DataBaseFeatures {
    public static int MAX_WAIT_SECONDS = 60 * 30;
    protected final Jdbc jdbc;

    public DataBaseFeatures(Jdbc jdbc) {
        Assert.notNull(jdbc, "参数 jdbc 不能为空");
        this.jdbc = jdbc;
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
