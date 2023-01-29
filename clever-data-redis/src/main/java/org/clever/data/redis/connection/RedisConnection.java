package org.clever.data.redis.connection;

import org.clever.dao.DataAccessException;

import java.util.List;

/**
 * 与 Redis 服务器的连接。作为各种 Redis 客户端库（或驱动程序）的通用抽象。
 * 此外还执行底层 Redis 客户端库和 DAO 异常之间的异常转换。
 * 这些方法尽可能遵循 Redis 名称和约定。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 16:47 <br/>
 */
public interface RedisConnection extends RedisCommands, AutoCloseable {
    /**
     * 获取 {@link RedisGeoCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisGeoCommands geoCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisHashCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisHashCommands hashCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisHyperLogLogCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisHyperLogLogCommands hyperLogLogCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisKeyCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisKeyCommands keyCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisListCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisListCommands listCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisSetCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisSetCommands setCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisScriptingCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisScriptingCommands scriptingCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisServerCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisServerCommands serverCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisStreamCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisStreamCommands streamCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisStringCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisStringCommands stringCommands() {
        return this;
    }

    /**
     * 获取 {@link RedisZSetCommands}
     *
     * @return 从不为 {@literal null}
     */
    default RedisZSetCommands zSetCommands() {
        return this;
    }

    /**
     * 关闭（或退出）连接
     */
    @Override
    void close() throws DataAccessException;

    /**
     * 指示基础连接是否关闭
     *
     * @return 如果连接关闭，则为true，否则为false
     */
    boolean isClosed();

    /**
     * 返回本机连接（基础librarydriver对象）
     *
     * @return 基础，本机对象
     */
    Object getNativeConnection();

    /**
     * 指示连接是否处于“队列”（或“MULTI”）模式。
     * 排队时，所有命令都将延迟，直到发出EXEC或DISCARD命令。
     * 由于在排队过程中不会返回任何结果，因此连接将在与数据交互的所有操作上返回NULL。
     *
     * @return 如果连接处于queueMULTI模式，则为true，否则为false
     */
    boolean isQueueing();

    /**
     * 指示连接当前是否为管道连接
     *
     * @return 如果连接是管道连接，则为true，否则为false
     * @see #openPipeline()
     * @see #isQueueing()
     */
    boolean isPipelined();

    /**
     * 为此连接激活管道模式。流水线化时，所有命令都返回 null（最后通过 {@link #closePipeline()} 读取回复。
     * 当连接已经通过管道时调用此方法没有任何效果。
     * 流水线用于发出命令而不立即请求响应，而是在批处理结束时发出。
     * 虽然有点类似于MULTI，但流水线并不能保证原子性——它只是在发出大量命令时（例如在批处理场景中）试图提高性能。
     * <p>
     * 注意:
     * </p>
     * 考虑在使用此功能之前进行一些性能测试，因为在许多情况下，性能优势微乎其微，但对使用率的影响却微乎其微。
     *
     * @see #multi()
     */
    void openPipeline();

    /**
     * 执行管道中的命令并返回其结果。如果连接不是管道连接，则返回一个空集合
     *
     * @return 执行命令的结果
     * @throws RedisPipelineException 如果管道包含任何不正确的无效语句
     */
    List<Object> closePipeline() throws RedisPipelineException;

    RedisSentinelConnection getSentinelConnection();
}
