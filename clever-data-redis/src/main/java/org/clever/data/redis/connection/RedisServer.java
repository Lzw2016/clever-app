package org.clever.data.redis.connection;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.Properties;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:34 <br/>
 */
public class RedisServer extends RedisNode {
    public enum INFO {
        NAME("name"),
        HOST("ip"),
        PORT("port"),
        RUN_ID("runid"),
        FLAGS("flags"),
        PENDING_COMMANDS("pending-commands"),
        LAST_PING_SENT("last-ping-sent"),
        LAST_OK_PING_REPLY("last-ok-ping-reply"),
        DOWN_AFTER_MILLISECONDS("down-after-milliseconds"),
        INFO_REFRESH("info-refresh"),
        ROLE_REPORTED("role-reported"),
        ROLE_REPORTED_TIME("role-reported-time"),
        CONFIG_EPOCH("config-epoch"),
        NUMBER_SLAVES("num-slaves"),
        NUMBER_OTHER_SENTINELS("num-other-sentinels"),
        BUFFER_LENGTH("qbuf"),
        BUFFER_FREE_SPACE("qbuf-free"),
        OUTPUT_BUFFER_LENGTH("obl"),
        OUTPUT_LIST_LENGTH("number-other-sentinels"),
        QUORUM("quorum"),
        FAILOVER_TIMEOUT("failover-timeout"),
        PARALLEL_SYNCS("parallel-syncs");

        final String key;

        INFO(String key) {
            this.key = key;
        }
    }

    private final Properties properties;

    /**
     * 使用给定的 {@code host}、{@code port} 创建一个新的 {@link RedisServer}
     *
     * @param host 不能是 {@literal null}
     * @param port 不能是 {@literal null}
     */
    public RedisServer(String host, int port) {
        this(host, port, new Properties());
    }

    /**
     * 使用给定的 {@code host}、{@code port} 和 {@code properties} 创建一个新的 {@link RedisServer}
     *
     * @param host       不能是 {@literal null}
     * @param port       不能是 {@literal null}
     * @param properties 可能是 {@literal null}
     */
    public RedisServer(String host, int port, Properties properties) {
        super(host, port);
        this.properties = properties;
        String name = host + ":" + port;
        if (properties != null && properties.containsKey(INFO.NAME.key)) {
            name = get(INFO.NAME);
        }
        setName(name);
    }

    /**
     * 从给定的属性创建一个新的 {@link RedisServer}
     */
    public static RedisServer newServerFrom(Properties properties) {
        String host = properties.getProperty(INFO.HOST.key, "127.0.0.1");
        int port = Integer.parseInt(properties.getProperty(INFO.PORT.key, "26379"));
        return new RedisServer(host, port, properties);
    }

    public void setQuorum(Long quorum) {
        if (quorum == null) {
            this.properties.remove(INFO.QUORUM.key);
            return;
        }
        this.properties.put(INFO.QUORUM.key, quorum.toString());
    }

    public String getRunId() {
        return get(INFO.RUN_ID);
    }

    public String getFlags() {
        return get(INFO.FLAGS);
    }

    public boolean isMaster() {
        String role = getRoleReported();
        if (!StringUtils.hasText(role)) {
            return false;
        }
        return role.equalsIgnoreCase("master");
    }

    public Long getPendingCommands() {
        return getLongValueOf(INFO.PENDING_COMMANDS);
    }

    public Long getLastPingSent() {
        return getLongValueOf(INFO.LAST_PING_SENT);
    }

    public Long getLastOkPingReply() {
        return getLongValueOf(INFO.LAST_OK_PING_REPLY);
    }

    public Long getDownAfterMilliseconds() {
        return getLongValueOf(INFO.DOWN_AFTER_MILLISECONDS);
    }

    public Long getInfoRefresh() {
        return getLongValueOf(INFO.INFO_REFRESH);
    }

    public String getRoleReported() {
        return get(INFO.ROLE_REPORTED);
    }

    public Long roleReportedTime() {
        return getLongValueOf(INFO.ROLE_REPORTED_TIME);
    }

    public Long getConfigEpoch() {
        return getLongValueOf(INFO.CONFIG_EPOCH);
    }

    public Long getNumberSlaves() {
        return getNumberReplicas();
    }

    /**
     * 获取连接的副本数
     */
    public Long getNumberReplicas() {
        return getLongValueOf(INFO.NUMBER_SLAVES);
    }

    public Long getNumberOtherSentinels() {
        return getLongValueOf(INFO.NUMBER_OTHER_SENTINELS);
    }

    public Long getQuorum() {
        return getLongValueOf(INFO.QUORUM);
    }

    public Long getFailoverTimeout() {
        return getLongValueOf(INFO.FAILOVER_TIMEOUT);
    }

    public Long getParallelSyncs() {
        return getLongValueOf(INFO.PARALLEL_SYNCS);
    }

    /**
     * @param info 不得为空
     * @return {@literal null} 如果没有找到所请求的 {@link INFO} 的条目
     */
    public String get(INFO info) {
        Assert.notNull(info, "Cannot retrieve client information for 'null'.");
        return get(info.key);
    }

    /**
     * @param key 不能是 {@literal null} or {@literal empty}.
     * @return {@literal null} 如果没有找到请求的 {@code key} 的条目
     */
    public String get(String key) {
        Assert.hasText(key, "Cannot get information for 'empty' / 'null' key.");
        return this.properties.getProperty(key);
    }

    private Long getLongValueOf(INFO info) {
        String value = get(info);
        return value == null ? null : Long.valueOf(value);
    }
}
