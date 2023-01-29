package org.clever.data.redis.connection;

/**
 * Redis 支持的命令的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:01 <br/>
 */
public interface RedisCommands extends RedisKeyCommands,
        RedisStringCommands,
        RedisListCommands,
        RedisSetCommands,
        RedisZSetCommands,
        RedisHashCommands,
        RedisTxCommands,
        RedisPubSubCommands,
        RedisConnectionCommands,
        RedisServerCommands,
        RedisStreamCommands,
        RedisScriptingCommands,
        RedisGeoCommands,
        RedisHyperLogLogCommands {
    /**
     * 给定命令的“Native”或“raw”执行以及给定参数。命令按原样执行，尽可能少地“interpretation” - 由调用者负责处理参数或结果。
     *
     * @param command 要执行的命令。不得为 {@literal null}
     * @param args    可能的命令参数（可能为空）
     * @return 执行结果。可以是 {@literal null}
     */
    Object execute(String command, byte[]... args);
}
