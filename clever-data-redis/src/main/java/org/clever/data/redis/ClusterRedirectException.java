package org.clever.data.redis;

import org.clever.dao.DataRetrievalFailureException;

/**
 * {@link ClusterRedirectException} 表示请求的插槽未由目标服务器提供服务，但可以在另一台服务器上获得
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:50 <br/>
 */
public class ClusterRedirectException extends DataRetrievalFailureException {
    private final int slot;
    private final String host;
    private final int port;

    /**
     * 创建新的 {@link ClusterRedirectException}
     *
     * @param slot       要重定向到的插槽
     * @param targetHost 要重定向到的主机
     * @param targetPort 主机上的端口
     * @param e          根本原因来自正在使用的数据访问 API
     */
    public ClusterRedirectException(int slot, String targetHost, int targetPort, Throwable e) {
        super(String.format("Redirect: slot %s to %s:%s.", slot, targetHost, targetPort), e);
        this.slot = slot;
        this.host = targetHost;
        this.port = targetPort;
    }

    public int getSlot() {
        return slot;
    }

    public String getTargetHost() {
        return host;
    }

    public int getTargetPort() {
        return port;
    }
}
