package org.clever.data.redis.connection;

/**
 * {@literal HyperLogLog} Redis 支持的特定命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:26 <br/>
 */
public interface RedisHyperLogLogCommands {
    /**
     * 将给定的 {@literal values} 添加到存储在给定的 {@literal key} 的 HyperLogLog 中
     *
     * @param key    不能是 {@literal null}
     * @param values 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/pfadd">Redis 文档: PFADD</a>
     */
    Long pfAdd(byte[] key, byte[]... values);

    /**
     * 返回HyperLogLog在 {@literal key(s)} 处观察到的结构的近似基数
     *
     * @param keys 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/pfcount">Redis 文档: PFCOUNT</a>
     */
    Long pfCount(byte[]... keys);

    /**
     * 将 {@literal sourceKeys} 处的N个不同HyperLogLog合并到单个 {@literal sourceKeys} 中
     *
     * @param destinationKey 不能是 {@literal null}
     * @param sourceKeys     不能是 {@literal null}
     * @see <a href="https://redis.io/commands/pfmerge">Redis 文档: PFMERGE</a>
     */
    void pfMerge(byte[] destinationKey, byte[]... sourceKeys);
}
