package org.clever.data.redis.stream;

import org.clever.dao.DataAccessResourceFailureException;

/**
 * Cancelable 允许停止长时间运行的任务并释放底层资源
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 10:43 <br/>
 */
public interface Cancelable {
    /**
     * 中止并释放资源
     */
    void cancel() throws DataAccessResourceFailureException;
}
