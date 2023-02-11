package org.clever.data.redis.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 映射在哈希上工作的特定操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:42 <br/>
 */
public interface HashOperations<H, HK, HV> {
    /**
     * 删除给定的哈希 {@code hashKeys}
     *
     * @param key      不得为 {@literal null}
     * @param hashKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long delete(H key, Object... hashKeys);

    /**
     * 确定给定的哈希 {@code hashKey} 是否存在
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Boolean hasKey(H key, Object hashKey);

    /**
     * 从 {@code key} 处的哈希中获取给定 {@code hashKey} 的值。
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 当键或哈希键在管道/事务中不存在或使用时
     */
    HV get(H key, Object hashKey);

    /**
     * 从 {@code key} 处的哈希中获取给定 {@code hashKeys} 的值。
     * 值按请求的键的顺序排列 缺少的字段值在生成的 {@link List} 中使用 {@code null} 表示。
     *
     * @param key      不得为 {@literal null}
     * @param hashKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    List<HV> multiGet(H key, Collection<HK> hashKeys);

    /**
     * 通过给定的 {@code delta} 递增哈希 {@code hashKey} 的哈希 {@code value}
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long increment(H key, HK hashKey, long delta);

    /**
     * 通过给定的 {@code delta} 递增哈希 {@code hashKey} 的哈希 {@code value}
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Double increment(H key, HK hashKey, double delta);

    /**
     * 从存储在 {@code key} 的哈希返回随机哈希键（又名字段）
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    HK randomKey(H key);

    /**
     * 从存储在 {@code key} 的哈希返回随机条目
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    Map.Entry<HK, HV> randomEntry(H key);

    /**
     * 从存储在 {@code key} 的哈希返回随机哈希键（又名字段）。
     * 如果提供的 {@code count} 参数为正，则返回不同哈希键的列表，上限为 {@code count} 或哈希大小。
     * 如果 {@code count} 为负数，则行为会更改，并且允许命令多次返回相同的哈希键。
     * 在这种情况下，返回字段数是指定计数的绝对值。
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的字段数
     * @return {@literal null} 如果密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    List<HK> randomKeys(H key, long count);

    /**
     * 从存储在 {@code key} 的哈希返回随机条目
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的字段数。必须是积极的
     * @return {@literal null} 如果键不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    Map<HK, HV> randomEntries(H key, long count);

    /**
     * 获取 {@code key} 处哈希的键集（字段）。
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Set<HK> keys(H key);

    /**
     * 返回与 {@code hashKey} 关联的值的长度。如果 {@code key} 或 {@code hashKey} 不存在，则返回 {@code 0}。
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long lengthOfValue(H key, HK hashKey);

    /**
     * 获取 {@code key} 处的哈希大小
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long size(H key);

    /**
     * 使用 {@code m} 中提供的数据将多个哈希字段设置为多个值
     *
     * @param key 不得为 {@literal null}
     * @param m   不得为 {@literal null}
     */
    void putAll(H key, Map<? extends HK, ? extends HV> m);

    /**
     * 设置哈希 {@code hashKey} 的 {@code value}
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     */
    void put(H key, HK hashKey, HV value);

    /**
     * 仅当 {@code hashKey} 不存在时才设置哈希 {@code hashKey} 的 {@code value}
     *
     * @param key     不得为 {@literal null}
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Boolean putIfAbsent(H key, HK hashKey, HV value);

    /**
     * 获取 {@code key} 处哈希的条目集（值）
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    List<HV> values(H key);

    /**
     * 获取存储在 {@code key} 处的整个哈希
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Map<HK, HV> entries(H key);

    /**
     * 使用 {@link Cursor} 在 {@code key} 处循环访问哈希中的条目。 <br />
     * <strong>重要:</strong> 完成后调用 {@link Cursor#close()} 以避免资源泄漏。
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Cursor<Map.Entry<HK, HV>> scan(H key, ScanOptions options);

    /**
     * @return 不为 {@literal null}
     */
    RedisOperations<H, ?> getOperations();
}
