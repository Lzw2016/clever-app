package org.clever.data.redis;

import org.clever.dao.DataAccessResourceFailureException;

/**
 * {@link DataAccessResourceFailureException} 表示集群状态的当前本地快照不再代表实际的远程状态。
 * 这可能会发生节点从集群中删除，槽被迁移到其他节点等等。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:28 <br/>
 */
public class ClusterStateFailureException extends DataAccessResourceFailureException {
    public ClusterStateFailureException(String msg) {
        super(msg);
    }

    public ClusterStateFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
