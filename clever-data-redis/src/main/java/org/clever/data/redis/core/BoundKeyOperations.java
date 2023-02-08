package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;
import org.clever.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 对 Redis 键的操作。用于对所有实现执行通用 key-'bound' 操作。
 * <p>
 * 与其余 API 一样，如果底层连接是流水线或多模式队列，则所有方法都将返回 {@literal null}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:30 <br/>
 */
public interface BoundKeyOperations<K> {
    /**
     * 返回与此实体关联的键
     *
     * @return 与实施实体相关的密钥
     */
    K getKey();

    /**
     * 返回关联的 Redis 类型。
     *
     * @return 键类型。{@literal null} 在管道/事务中使用时。
     */
    DataType getType();

    /**
     * 返回此密钥的到期时间
     *
     * @return 到期值（以秒为单位）。 {@literal null} 在管道/事务中使用时。
     */
    Long getExpire();

    /**
     * 设置密钥的生存时间到期
     *
     * @param timeout 不得为 {@literal null}
     * @return {@literal true} 如果设置了过期时间，则 {@literal false} 否则。 {@literal null} 在管道/事务中使用时
     * @throws IllegalArgumentException 如果超时为 {@literal null}
     */
    default Boolean expire(Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        return expire(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 设置密钥的生存时间到期
     *
     * @param timeout 到期值
     * @param unit    过期单位
     * @return 如果设置了过期时间，则为 true，否则为 false。 {@literal null} 在管道/事务中使用时。
     */
    Boolean expire(long timeout, TimeUnit unit);

    /**
     * 设置密钥的生存时间到期
     *
     * @param date 失效日期
     * @return 如果设置了过期时间，则为 true，否则为 false。 {@literal null} 在管道/事务中使用时。
     */
    Boolean expireAt(Date date);

    /**
     * 设置密钥的生存时间到期
     *
     * @param expireAt 到期时间
     * @return {@literal true} 如果设置了过期时间，则 {@literal false} 否则。 {@literal null} 在管道事务中使用时
     * @throws IllegalArgumentException 如果瞬间是 {@literal null} 或太大而无法表示为 {@code Date}
     */
    default Boolean expireAt(Instant expireAt) {
        Assert.notNull(expireAt, "ExpireAt must not be null");
        return expireAt(Date.from(expireAt));
    }

    /**
     * 删除密钥的到期时间（如果有）
     *
     * @return 如果删除了过期时间，则为 true，否则为 false。 {@literal null} 在管道/事务中使用时。
     */
    Boolean persist();

    /**
     * 重命名 key <br>
     * <b>注意：</b>空集合的新名称将在添加第一个元素时传播。
     *
     * @param newKey 新 key。 不得为 {@literal null}
     */
    void rename(K newKey);
}
