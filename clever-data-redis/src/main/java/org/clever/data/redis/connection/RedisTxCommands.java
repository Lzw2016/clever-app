package org.clever.data.redis.connection;

import java.util.List;

/**
 * Redis 支持的 Transaction/Batch 特定命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:19 <br/>
 */
public interface RedisTxCommands {
    /**
     * 标记事务块的开始。 <br>
     * 命令将排队，然后可以通过调用 {@link #exec()} 执行或使用 {@link #discard()} 回滚
     *
     * @see <a href="https://redis.io/commands/multi">Redis 文档: MULTI</a>
     */
    void multi();

    /**
     * 在以 {@link #multi()} 开始的事务中执行所有排队的命令。 <br>
     * 如果与 {@link #watch(byte[]...)} 一起使用，如果任何被监视的键被修改，操作将失败。
     *
     * @return 每个已执行命令的回复列表
     * @see <a href="https://redis.io/commands/exec">Redis 文档: EXEC</a>
     */
    List<Object> exec();

    /**
     * 丢弃在 {@link #multi()} 之后发出的所有命令
     *
     * @see <a href="https://redis.io/commands/discard">Redis 文档: DISCARD</a>
     */
    void discard();

    /**
     * 在以 {@link #multi()} 开始的事务期间观察给定的 {@code keys} 的修改
     *
     * @param keys 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/watch">Redis 文档: WATCH</a>
     */
    void watch(byte[]... keys);

    /**
     * 刷新所有以前的 {@link #watch(byte[]...)} 键
     *
     * @see <a href="https://redis.io/commands/unwatch">Redis 文档: UNWATCH</a>
     */
    void unwatch();
}
