package org.clever.data.redis.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 绑定到某个密钥的哈希操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:43 <br/>
 */
public interface BoundHashOperations<H, HK, HV> extends BoundKeyOperations<H> {
    /**
     * 删除绑定键处的给定哈希 {@code keys}
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long delete(Object... keys);

    /**
     * 确定给定的散列 {@code key} 是否存在于绑定键中
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Boolean hasKey(Object key);

    /**
     * 从绑定键的散列中获取给定 {@code key} 的值
     *
     * @param member 不得为 {@literal null}
     * @return {@literal null} 当成员不存在或在管道/事务中使用时
     */
    HV get(Object member);

    /**
     * 从绑定键的散列中获取给定 {@code keys} 的值。值按请求键的顺序排列。缺少的字段值在结果 {@link List} 中使用 {@code null} 表示
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    List<HV> multiGet(Collection<HK> keys);

    /**
     * 通过给定的 {@code delta} 在绑定键上增加散列 {@code key} 的 {@code value}
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long increment(HK key, long delta);

    /**
     * 通过给定的 {@code delta} 在绑定键上增加散列 {@code key} 的 {@code value}
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Double increment(HK key, double delta);

    /**
     * 从存储在绑定键的散列中返回一个随机键（又名字段）
     *
     * @return {@literal null} 如果散列不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    HK randomKey();

    /**
     * 从存储在绑定键的哈希中返回一个随机条目
     *
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    Map.Entry<HK, HV> randomEntry();

    /**
     * 从存储在绑定键的散列中返回一个随机键（又名字段）。如果提供的 {@code count} 参数为正，则返回不同键的列表，上限为 {@code count} 或哈希大小。
     * 如果 {@code count} 为负数，则行为会改变并且允许命令多次返回相同的键。
     * 在这种情况下，返回的键数是指定计数的绝对值。
     *
     * @param count 要返回的键数
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    List<HK> randomKeys(long count);

    /**
     * 从存储在绑定键的散列中返回一个随机条目
     *
     * @param count 要返回的条目数。必须是积极的
     * @return {@literal null} 如果散列不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/hrandfield">Redis 文档: HRANDFIELD</a>
     */
    Map<HK, HV> randomEntries(long count);

    /**
     * 获取绑定键处散列的键集（字段）
     *
     * @return {@literal null} 在管道/事务中使用时。
     */
    Set<HK> keys();

    /**
     * 返回与 {@code hashKey} 关联的值的长度。
     * 如果 {@code hashKey} 不存在，则返回 {@code 0}。
     *
     * @param hashKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long lengthOfValue(HK hashKey);

    /**
     * 获取绑定键的散列大小
     *
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long size();

    /**
     * 使用绑定键的 {@code m} 中提供的数据将多个哈希字段设置为多个值
     *
     * @param m 不得为 {@literal null}
     */
    void putAll(Map<? extends HK, ? extends HV> m);

    /**
     * 在绑定键处设置散列 {@code key} 的 {@code value}
     *
     * @param key 不得为 {@literal null}
     */
    void put(HK key, HV value);

    /**
     * 仅当 {@code key} 不存在时才设置散列 {@code key} 的 {@code value}
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Boolean putIfAbsent(HK key, HV value);

    /**
     * 获取绑定键处散列的条目集（值）
     *
     * @return {@literal null} 在管道/事务中使用时。
     */
    List<HV> values();

    /**
     * 获取绑定键的整个哈希值
     *
     * @return {@literal null} 在管道/事务中使用时。
     */
    Map<HK, HV> entries();

    /**
     * 使用 {@link Cursor} 迭代散列中的条目
     */
    Cursor<Map.Entry<HK, HV>> scan(ScanOptions options);

    /**
     * @return 从不为 {@literal null}
     */
    RedisOperations<H, ?> getOperations();
}
