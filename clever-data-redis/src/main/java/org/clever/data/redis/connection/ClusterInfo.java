package org.clever.data.redis.connection;

import org.clever.data.redis.core.types.RedisClientInfo.INFO;
import org.clever.util.Assert;

import java.util.Properties;

/**
 * {@link ClusterInfo} 提供对集群信息的访问，
 * 例如 {@code cluster_state} 和 {@code cluster_slots_assigned} 由 {@code CLUSTER INFO} 命令提供。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:41 <br/>
 */
public class ClusterInfo {
    public enum Info {
        STATE("cluster_state"),
        SLOTS_ASSIGNED("cluster_slots_assigned"),
        SLOTS_OK("cluster_slots_ok"),
        SLOTS_PFAIL("cluster_slots_pfail"),
        SLOTS_FAIL("cluster_slots_fail"),
        KNOWN_NODES("cluster_known_nodes"),
        SIZE("cluster_size"),
        CURRENT_EPOCH("cluster_current_epoch"),
        MY_EPOCH("cluster_my_epoch"),
        MESSAGES_SENT("cluster_stats_messages_sent"),
        MESSAGES_RECEIVED("cluster_stats_messages_received");

        final String key;

        Info(String key) {
            this.key = key;
        }
    }

    private final Properties clusterProperties;

    /**
     * 为给定的 {@link Properties} 创建新的 {@link ClusterInfo}
     *
     * @param clusterProperties 不能是 {@literal null}
     */
    public ClusterInfo(Properties clusterProperties) {
        Assert.notNull(clusterProperties, "ClusterProperties must not be null!");
        this.clusterProperties = clusterProperties;
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#STATE} 的条目
     * @see Info#STATE
     */
    public String getState() {
        return get(Info.STATE);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#SLOTS_ASSIGNED} 的条目
     * @see Info#SLOTS_ASSIGNED
     */
    public Long getSlotsAssigned() {
        return getLongValueOf(Info.SLOTS_ASSIGNED);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#SLOTS_OK} 的条目
     * @see Info#SLOTS_OK
     */
    public Long getSlotsOk() {
        return getLongValueOf(Info.SLOTS_OK);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#SLOTS_PFAIL} 的条目
     * @see Info#SLOTS_PFAIL
     */
    public Long getSlotsPfail() {
        return getLongValueOf(Info.SLOTS_PFAIL);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#SLOTS_FAIL} 的条目
     * @see Info#SLOTS_FAIL
     */
    public Long getSlotsFail() {
        return getLongValueOf(Info.SLOTS_FAIL);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#KNOWN_NODES} 的条目
     * @see Info#KNOWN_NODES
     */
    public Long getKnownNodes() {
        return getLongValueOf(Info.KNOWN_NODES);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#SIZE} 的条目
     * @see Info#SIZE
     */
    public Long getClusterSize() {
        return getLongValueOf(Info.SIZE);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#CURRENT_EPOCH} 的条目
     * @see Info#CURRENT_EPOCH
     */
    public Long getCurrentEpoch() {
        return getLongValueOf(Info.CURRENT_EPOCH);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#MESSAGES_SENT} 的条目
     * @see Info#MESSAGES_SENT
     */
    public Long getMessagesSent() {
        return getLongValueOf(Info.MESSAGES_SENT);
    }

    /**
     * @return {@literal null} 如果没有找到所请求的 {@link Info#MESSAGES_RECEIVED} 的条目
     * @see Info#MESSAGES_RECEIVED
     */
    public Long getMessagesReceived() {
        return getLongValueOf(Info.MESSAGES_RECEIVED);
    }

    /**
     * @param info 不得为空
     * @return {@literal null} 如果没有找到所请求的 {@link INFO} 的条目
     */
    public String get(Info info) {
        Assert.notNull(info, "Cannot retrieve cluster information for 'null'.");
        return get(info.key);
    }

    /**
     * @param key 不得为 {@literal null} 或 {@literal empty}
     * @return {@literal null} 如果没有找到请求的 {@code key} 的条目
     */
    public String get(String key) {
        Assert.hasText(key, "Cannot get cluster information for 'empty' / 'null' key.");
        return this.clusterProperties.getProperty(key);
    }

    private Long getLongValueOf(Info info) {
        String value = get(info);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public String toString() {
        return this.clusterProperties.toString();
    }
}
