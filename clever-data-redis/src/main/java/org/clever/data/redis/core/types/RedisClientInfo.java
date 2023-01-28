package org.clever.data.redis.core.types;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * {@link org.clever.data.redis.core.types.RedisClientInfo} 提供有关客户端连接的一般信息和统计信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:35 <br/>
 */
public class RedisClientInfo {
    public enum INFO {
        ADDRESS_PORT("addr"),
        FILE_DESCRIPTOR("fd"),
        CONNECTION_NAME("name"),
        CONNECTION_AGE("age"),
        CONNECTION_IDLE("idle"),
        FLAGS("flags"),
        DATABSE_ID("db"),
        CHANNEL_SUBSCRIBTIONS("sub"),
        PATTERN_SUBSCRIBTIONS("psub"),
        MULIT_COMMAND_CONTEXT("multi"),
        BUFFER_LENGTH("qbuf"),
        BUFFER_FREE_SPACE("qbuf-free"),
        OUTPUT_BUFFER_LENGTH("obl"),
        OUTPUT_LIST_LENGTH("oll"),
        OUTPUT_BUFFER_MEMORY_USAGE("omem"),
        EVENTS("events"),
        LAST_COMMAND("cmd");
        final String key;

        INFO(String key) {
            this.key = key;
        }
    }

    private final Properties clientProperties;

    /**
     * Create {@link RedisClientInfo} from {@link Properties}.
     *
     * @param properties 不能是 {@literal null}
     */
    public RedisClientInfo(Properties properties) {
        Assert.notNull(properties, "Cannot initialize client information for given 'null' properties.");
        this.clientProperties = new Properties();
        this.clientProperties.putAll(properties);
    }

    /**
     * 获取客户端的地址端口
     */
    public String getAddressPort() {
        return get(INFO.ADDRESS_PORT);
    }

    /**
     * 获取套接字对应的文件描述符
     */
    public String getFileDescriptor() {
        return get(INFO.FILE_DESCRIPTOR);
    }

    /**
     * 获取客户端名称
     */
    public String getName() {
        return get(INFO.CONNECTION_NAME);
    }

    /**
     * 获取连接的总持续时间（秒）
     */
    public Long getAge() {
        return getLongValueOf(INFO.CONNECTION_AGE);
    }

    /**
     * 获取连接的空闲时间（秒）
     */
    public Long getIdle() {
        return getLongValueOf(INFO.CONNECTION_IDLE);
    }

    /**
     * 获取客户端标志
     */
    public String getFlags() {
        return get(INFO.FLAGS);
    }

    /**
     * 获取当前数据库索引
     */
    public Long getDatabaseId() {
        return getLongValueOf(INFO.DATABSE_ID);
    }

    /**
     * 获取频道订阅数
     */
    public Long getChannelSubscribtions() {
        return getLongValueOf(INFO.CHANNEL_SUBSCRIBTIONS);
    }

    /**
     * 获取模式订阅数
     */
    public Long getPatternSubscrbtions() {
        return getLongValueOf(INFO.PATTERN_SUBSCRIBTIONS);
    }

    /**
     * 获取 MULIT_COMMAND_CONTEXT 中的命令数
     */
    public Long getMultiCommandContext() {
        return getLongValueOf(INFO.MULIT_COMMAND_CONTEXT);
    }

    /**
     * 获取查询缓冲区长度
     */
    public Long getBufferLength() {
        return getLongValueOf(INFO.BUFFER_LENGTH);
    }

    /**
     * 获取查询缓冲区的可用空间
     */
    public Long getBufferFreeSpace() {
        return getLongValueOf(INFO.BUFFER_FREE_SPACE);
    }

    /**
     * 获取输出缓冲区长度
     */
    public Long getOutputBufferLength() {
        return getLongValueOf(INFO.OUTPUT_BUFFER_LENGTH);
    }

    /**
     * 获取输出缓冲区中排队的答复数
     */
    public Long getOutputListLength() {
        return getLongValueOf(INFO.OUTPUT_LIST_LENGTH);
    }

    /**
     * 获取输出缓冲区内存使用情况
     */
    public Long getOutputBufferMemoryUsage() {
        return getLongValueOf(INFO.OUTPUT_BUFFER_MEMORY_USAGE);
    }

    /**
     * 获取文件描述符事件
     */
    public String getEvents() {
        return get(INFO.EVENTS);
    }

    /**
     * 播放最后一个命令
     */
    public String getLastCommand() {
        return get(INFO.LAST_COMMAND);
    }

    /**
     * @param info 不能为空
     * @return 如果找不到请求的 {@link INFO} 的条目，则返回 {@literal null}
     */
    public String get(INFO info) {
        Assert.notNull(info, "Cannot retrieve client information for 'null'.");
        return this.clientProperties.getProperty(info.key);
    }

    /**
     * @param key 不能为 {@literal null} 或 {@literal empty}
     * @return 如果找不到请求的 {@code key} 的条目，则返回 {@literal null}
     */
    public String get(String key) {
        Assert.hasText(key, "Cannot get client information for 'empty' / 'null' key.");
        return this.clientProperties.getProperty(key);
    }

    private Long getLongValueOf(INFO info) {
        String value = get(info);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public String toString() {
        return this.clientProperties.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisClientInfo that = (RedisClientInfo) o;
        return ObjectUtils.nullSafeEquals(clientProperties, that.clientProperties);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(clientProperties);
    }

    public static class RedisClientInfoBuilder {
        public static RedisClientInfo fromString(String source) {
            Assert.notNull(source, "Cannot read client properties form 'null'.");
            Properties properties = new Properties();
            try {
                properties.load(new StringReader(source.replace(' ', '\n')));
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Properties could not be loaded from String '%s'.", source), e);
            }
            return new RedisClientInfo(properties);
        }
    }
}
