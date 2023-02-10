package org.clever.data.redis.core;

import org.clever.dao.DataAccessException;

/**
 * 回调对代理“会话”执行所有操作（基本上对相同的底层Redis连接）。
 * 允许通过使用 multi/discard/exec/watch/unwatch 命令进行“事务”。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:26 <br/>
 */
public interface SessionCallback<T> {
    /**
     * 在同一会话内执行所有给定操作
     *
     * @param operations Redis操作
     * @return 返回值
     */
    <K, V> T execute(RedisOperations<K, V> operations) throws DataAccessException;
}
