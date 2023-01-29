package org.clever.data.redis.connection;

import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanOptions;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 支持的特定于哈希的命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:18 <br/>
 */
public interface RedisHashCommands {
    /**
     * 设置哈希{@code 字段} 的{@code 值}
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hset">Redis 文档: HSET</a>
     */
    Boolean hSet(byte[] key, byte[] field, byte[] value);

    /**
     * 仅当 {@code field} 不存在时才设置散列 {@code field} 的 {@code value}
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hsetnx">Redis 文档: HSETNX</a>
     */
    Boolean hSetNX(byte[] key, byte[] field, byte[] value);

    /**
     * 从 {@code key} 的散列中获取给定 {@code field} 的值
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @return {@literal null} 当键或字段不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hget">Redis 文档: HGET</a>
     */
    byte[] hGet(byte[] key, byte[] field);

    /**
     * 从 {@code key} 的散列中获取给定 {@code fields} 的值。<br/>
     * 值按请求键的顺序排列。缺少的字段值在结果 {@link List} 中使用 {@code null} 表示。
     *
     * @param key    不能是 {@literal null}
     * @param fields 不能是 {@literal empty}
     * @return 如果密钥不存在，则为空 {@link List}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hmget">Redis 文档: HMGET</a>
     */
    List<byte[]> hMGet(byte[] key, byte[]... fields);

    /**
     * 使用 {@code hashes} 中提供的数据将多个哈希字段设置为多个值
     *
     * @param key    不能是 {@literal null}
     * @param hashes 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/hmset">Redis 文档: HMSET</a>
     */
    void hMSet(byte[] key, Map<byte[], byte[]> hashes);

    /**
     * 通过给定的 {@code delta} 增加散列 {@code 字段} 的 {@code value}
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @param delta 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hincrby">Redis 文档: HINCRBY</a>
     */
    Long hIncrBy(byte[] key, byte[] field, long delta);

    /**
     * 通过给定的 {@code delta} 增加散列 {@code 字段} 的 {@code value}
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @param delta 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hincrbyfloat">Redis 文档: HINCRBYFLOAT</a>
     */
    Double hIncrBy(byte[] key, byte[] field, double delta);

    /**
     * 确定给定的散列 {@code field} 是否存在
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hexits">Redis 文档: HEXISTS</a>
     */
    Boolean hExists(byte[] key, byte[] field);

    /**
     * 删除给定的散列 {@code fields}
     *
     * @param key    不能是 {@literal null}
     * @param fields must not be {@literal empty}.
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hdel">Redis 文档: HDEL</a>
     */
    Long hDel(byte[] key, byte[]... fields);

    /**
     * 获取 {@code key} 处的散列大小
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hlen">Redis 文档: HLEN</a>
     */
    Long hLen(byte[] key);

    /**
     * 在 {@code key} 处获取散列的键集（字段）
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hkeys">Redis 文档: HKEYS</a>?
     */
    Set<byte[]> hKeys(byte[] key);

    /**
     * 在 {@code field} 获取哈希的条目集（值）
     *
     * @param key 不能是 {@literal null}
     * @return 如果密钥不存在，则为空 {@link List}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hvals">Redis 文档: HVALS</a>
     */
    List<byte[]> hVals(byte[] key);

    /**
     * 获取存储在 {@code key} 的整个哈希
     *
     * @param key 不能是 {@literal null}
     * @return 清空 {@link Map} 如果键不存在或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hgetall">Redis 文档: HGETALL</a>
     */
    Map<byte[], byte[]> hGetAll(byte[] key);

    /**
     * 从存储在 {@code key} 的哈希中返回一个随机字段
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    byte[] hRandField(byte[] key);

    /**
     * 从散列中返回一个随机字段及其存储在 {@code key} 中的值
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    Map.Entry<byte[], byte[]> hRandFieldWithValues(byte[] key);

    /**
     * 从存储在 {@code key} 的哈希中返回一个随机字段。<br/>
     * 如果提供的 {@code count} 参数为正，则返回不同字段的列表，上限为 {@code count} 或哈希大小。<br/>
     * 如果 {@code count} 为负数，则行为会发生变化，并且允许该命令多次返回同一字段。在这种情况下，返回的字段数是指定计数的绝对值。
     *
     * @param key   不能是 {@literal null}
     * @param count number of fields to return.
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    List<byte[]> hRandField(byte[] key, long count);

    /**
     * 从散列中返回一个随机字段及其存储在 {@code key} 中的值。 <br/>
     * 如果提供的 {@code count} 参数为正，则返回不同字段的列表，上限为 {@code count} 或哈希大小。 <br/>
     * 如果 {@code count} 为负数，则行为会发生变化，并且允许该命令多次返回同一字段。
     * 在这种情况下，返回的字段数是指定计数的绝对值。
     *
     * @param key   不能是 {@literal null}
     * @param count number of fields to return.
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    List<Map.Entry<byte[], byte[]>> hRandFieldWithValues(byte[] key, long count);

    /**
     * 使用 {@link Cursor} 遍历 {@code key} 处哈希中的条目
     *
     * @param key     不能是 {@literal null}
     * @param options 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/hscan">Redis 文档: HSCAN</a>
     */
    Cursor<Map.Entry<byte[], byte[]>> hScan(byte[] key, ScanOptions options);

    /**
     * 返回存储在 {@code key} 的散列中与 {@code field} 关联的值的长度。 <br/>
     * 如果 {@code key} 或 {@code field} 不存在，则返回 {@code 0}。
     *
     * @param key   不能是 {@literal null}
     * @param field 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hstrlen">Redis 文档: HSTRLEN</a>
     */
    Long hStrLen(byte[] key, byte[] field);
}
