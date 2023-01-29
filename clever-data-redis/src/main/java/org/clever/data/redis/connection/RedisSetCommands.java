package org.clever.data.redis.connection;

import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanOptions;

import java.util.List;
import java.util.Set;

/**
 * Redis 支持的特定于集合的命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:16 <br/>
 */
public interface RedisSetCommands {
    /**
     * 将给定的 {@code values} 添加到 {@code key} 处
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为空.
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/sadd">Redis 文档: SADD</a>
     */
    Long sAdd(byte[] key, byte[]... values);

    /**
     * 从 {@code key} 处的设置中删除给定的 {@code values}，并返回已删除元素的数量
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为空
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/srem">Redis 文档: SREM</a>
     */
    Long sRem(byte[] key, byte[]... values);

    /**
     * 从 {@code key} 处的集合中删除并返回随机成员
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当键不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/spop">Redis 文档: SPOP</a>
     */
    byte[] sPop(byte[] key);

    /**
     * 从 {@code key} 处的集合中删除并返回 {@code 计数} 个随机成员
     *
     * @param key   不得为 {@literal null}
     * @param count 从集合中弹出的随机成员数
     * @return 如果集合不存在，则为空 {@link List}. {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/spop">Redis 文档: SPOP</a>
     */
    List<byte[]> sPop(byte[] key, long count);

    /**
     * 将 {@code values} 从 {@code srcKey} 移动到 {@code destKey}
     *
     * @param srcKey  不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/smove">Redis 文档: SMOVE</a>
     */
    Boolean sMove(byte[] srcKey, byte[] destKey, byte[] value);

    /**
     * 获取 {@code key} 处的设置大小
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/scard">Redis 文档: SCARD</a>
     */
    Long sCard(byte[] key);

    /**
     * 检查设置为 {@code key} 是否包含 {@code values}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sismember">Redis 文档: SISMEMBER</a>
     */
    Boolean sIsMember(byte[] key, byte[] value);

    /**
     * 检查设置为 {@code key} 是否包含一个或多个 {@code values}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/smismember">Redis 文档: SMISMEMBER</a>
     */
    List<Boolean> sMIsMember(byte[] key, byte[]... values);

    /**
     * 比较给定 {@code key} 的所有集合
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sdiff">Redis 文档: SDIFF</a>
     */
    Set<byte[]> sDiff(byte[]... keys);

    /**
     * 比较给定 {@code key} 的所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param destKey 不得为 {@literal null}
     * @param keys    不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sdiffstore">Redis 文档: SDIFFSTORE</a>
     */
    Long sDiffStore(byte[] destKey, byte[]... keys);

    /**
     * 返回在 {@code key} 处与所有给定集合相交的成员
     *
     * @param keys 不得为 {@literal null}
     * @return 空 {@link Set} 当键不存在时. {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sinter">Redis 文档: SINTER</a>
     */
    Set<byte[]> sInter(byte[]... keys);

    /**
     * 在 {@code key} 处交叉所有给定的集合，并将结果存储在 {@code destKey} 中
     *
     * @param destKey 不得为 {@literal null}
     * @param keys    不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sinterstore">Redis 文档: SINTERSTORE</a>
     */
    Long sInterStore(byte[] destKey, byte[]... keys);

    /**
     * 在给定的 {@code key} 处合并所有集合
     *
     * @param keys 不得为 {@literal null}
     * @return 空 {@link Set} 当键不存在时. {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sunion">Redis 文档: SUNION</a>
     */
    Set<byte[]> sUnion(byte[]... keys);

    /**
     * 将所有集合合并给定的 {@code key} 并将结果存储在 {@code destKey} 中
     *
     * @param destKey 不得为 {@literal null}
     * @param keys    不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/sunionstore">Redis 文档: SUNIONSTORE</a>
     */
    Long sUnionStore(byte[] destKey, byte[]... keys);

    /**
     * 获取 set at {@code key} 的所有元素
     *
     * @param key 不得为 {@literal null}
     * @return 空 {@link Set} 当键不存在时. {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/smembers">Redis 文档: SMEMBERS</a>
     */
    Set<byte[]> sMembers(byte[] key);

    /**
     * 从 {@code key} 处的集合中获取随机元素
     *
     * @param key 不得为 {@literal null}
     * @return 可以是 {@literal null}.
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    byte[] sRandMember(byte[] key);

    /**
     * 从 {@code key} 处获取 {@code count} 随机元素
     *
     * @param key   不得为 {@literal null}
     * @param count 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    List<byte[]> sRandMember(byte[] key, long count);

    /**
     * 使用 {@link Cursor} 迭代 set at {@code key} 中的元素
     *
     * @param key     不得为 {@literal null}
     * @param options 不得为 {@literal null}
     * @return 从不 {@literal null}
     * @see <a href="https://redis.io/commands/scan">Redis 文档: SCAN</a>
     */
    Cursor<byte[]> sScan(byte[] key, ScanOptions options);
}
