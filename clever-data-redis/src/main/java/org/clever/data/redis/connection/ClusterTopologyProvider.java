package org.clever.data.redis.connection;

/**
 * {@link ClusterTopologyProvider} 管理当前群集拓扑，并确保刷新群集信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:26 <br/>
 */
public interface ClusterTopologyProvider {
    /**
     * 获取当前已知的 {@link ClusterTopology}
     *
     * @return 从不为 {@literal null}.
     */
    ClusterTopology getTopology();
}
