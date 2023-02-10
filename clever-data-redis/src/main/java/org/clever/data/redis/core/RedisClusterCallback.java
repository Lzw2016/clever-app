package org.clever.data.redis.core;

import org.clever.dao.DataAccessException;
import org.clever.data.redis.connection.RedisClusterConnection;

/**
 * 针对群集Redis环境执行的低级操作的回调接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:56 <br/>
 */
public interface RedisClusterCallback<T> {
    /**
     * 通过活动的Redis连接由 {@link ClusterOperations} 调用。不需要关心激活或关闭连接或处理异常。
     *
     * @param connection 从不为 {@literal null}
     */
    T doInRedis(RedisClusterConnection connection) throws DataAccessException;
}
