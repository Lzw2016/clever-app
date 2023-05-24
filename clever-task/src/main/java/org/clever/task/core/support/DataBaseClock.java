package org.clever.task.core.support;

import org.clever.core.SystemClock;
import org.clever.data.jdbc.Jdbc;
import org.clever.task.core.TaskDataSource;

/**
 * 数据库时钟
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/05/24 15:07 <br/>
 */
public class DataBaseClock implements WheelTimer.Clock {
    private final Jdbc jdbc;
    private volatile long offset = 0;

    public DataBaseClock() {
        jdbc = TaskDataSource.getJdbc();
        syncTime();
    }

    /**
     * 同步数据库时间
     */
    public void syncTime() {
        long t1 = System.currentTimeMillis();
        long t2 = jdbc.currentDate().getTime();
        long t4 = System.currentTimeMillis();
        offset = (t2 - t1) - (t4 - t2);
    }

    @Override
    public long currentTimeMillis() {
        return SystemClock.now() + offset;
    }
}
