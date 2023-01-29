package org.clever.data.redis.connection;

import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.KeyScanOptions;
import org.clever.data.redis.core.ScanOptions;
import org.clever.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 支持的特定于键的命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:05 <br/>
 */
public interface RedisKeyCommands {
    /**
     * 将给定的 {@code sourceKey} 复制到 {@code targetKey}
     *
     * @param sourceKey 不得为 {@literal null}
     * @param targetKey 不得为 {@literal null}
     * @param replace   是否替换现有密钥
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/copy">Redis 文档: COPY</a>
     */
    Boolean copy(byte[] sourceKey, byte[] targetKey, boolean replace);

    /**
     * 确定给定的 {@code key} 是否存在
     *
     * @param key 不得为 {@literal null}
     * @return {@literal true} 如果键存在。{@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/exists">Redis 文档: EXISTS</a>
     */
    default Boolean exists(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        Long count = exists(new byte[][]{key});
        return count != null ? count > 0 : null;
    }

    /**
     * 计算给定的 {@code keys} 存在的数量。多次提供完全相同的 {@code key} 也算多次
     *
     * @param keys 不得为 {@literal null}
     * @return 指定为参数的键中存在的键数。 {@literal null} 在管道/事务中使用时
     */
    Long exists(byte[]... keys);

    /**
     * 删除给定的 {@code keys}
     *
     * @param keys 不得为 {@literal null}
     * @return 删除的密钥数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/del">Redis 文档: DEL</a>
     */
    Long del(byte[]... keys);

    /**
     * 从键空间中取消链接 {@code keys}。与 {@link #del(byte[]...)} 不同，这里的实际内存回收是异步发生的
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/unlink">Redis 文档: UNLINK</a>
     */
    Long unlink(byte[]... keys);

    /**
     * 确定存储在 {@code key} 的类型
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/type">Redis 文档: TYPE</a>
     */
    DataType type(byte[] key);

    /**
     * 更改给定 {@code key(s)} 的最后访问时间
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/touch">Redis 文档: TOUCH</a>
     */
    Long touch(byte[]... keys);

    /**
     * 查找与给定的 {@code pattern} 匹配的所有键
     *
     * @param pattern 不得为 {@literal null}
     * @return 如果找不到匹配项，则为空 {@link Set}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/keys">Redis 文档: KEYS</a>
     */
    Set<byte[]> keys(byte[] pattern);

    /**
     * 使用 {@link Cursor} 遍历键
     *
     * @param options 不得为 {@literal null}
     * @return 从不{@literal null}
     * @see <a href="https://redis.io/commands/scan">Redis 文档: SCAN</a>
     */
    default Cursor<byte[]> scan(KeyScanOptions options) {
        return scan((ScanOptions) options);
    }

    /**
     * 使用 {@link Cursor} 遍历键
     *
     * @param options 不得为 {@literal null}
     * @return 从不{@literal null}
     * @see <a href="https://redis.io/commands/scan">Redis 文档: SCAN</a>
     */
    Cursor<byte[]> scan(ScanOptions options);

    /**
     * 从键空间返回一个随机键
     *
     * @return {@literal null} 如果没有可用的密钥或在管道或事务中使用
     * @see <a href="https://redis.io/commands/randomkey">Redis 文档: RANDOMKEY</a>
     */
    byte[] randomKey();

    /**
     * 将密钥 {@code oldKey} 重命名为 {@code newKey}
     *
     * @param oldKey 不得为 {@literal null}
     * @param newKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/rename">Redis 文档: RENAME</a>
     */
    void rename(byte[] oldKey, byte[] newKey);

    /**
     * 仅当 {@code newKey} 不存在时，才将键 {@code oldKey} 重命名为 {@code newKey}
     *
     * @param oldKey 不得为 {@literal null}
     * @param newKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/renamenx">Redis 文档: RENAMENX</a>
     */
    Boolean renameNX(byte[] oldKey, byte[] newKey);

    /**
     * 以秒为单位设置给定 {@code key} 的生存时间
     *
     * @param key     不得为 {@literal null}
     * @param seconds 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/expire">Redis 文档: EXPIRE</a>
     */
    Boolean expire(byte[] key, long seconds);

    /**
     * 设置给定 {@code key} 的生存时间（以毫秒为单位）
     *
     * @param key    不得为 {@literal null}
     * @param millis 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/pexpire">Redis 文档: PEXPIRE</a>
     */
    Boolean pExpire(byte[] key, long millis);

    /**
     * 将给定 {@code key} 的到期时间设置为 {@literal UNIX} 时间戳
     *
     * @param key      不得为 {@literal null}
     * @param unixTime 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/expireat">Redis 文档: EXPIREAT</a>
     */
    Boolean expireAt(byte[] key, long unixTime);

    /**
     * 将给定 {@code key} 的到期时间设置为以毫秒为单位的 {@literal UNIX} 时间戳
     *
     * @param key              不得为 {@literal null}
     * @param unixTimeInMillis 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/pexpireat">Redis 文档: PEXPIREAT</a>
     */
    Boolean pExpireAt(byte[] key, long unixTimeInMillis);

    /**
     * 删除给定 {@code key} 的过期时间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/persist">Redis 文档: PERSIST</a>
     */
    Boolean persist(byte[] key);

    /**
     * 使用 {@code index} 将给定的 {@code key} 移动到数据库
     *
     * @param key     不得为 {@literal null}
     * @param dbIndex 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/move">Redis 文档: MOVE</a>
     */
    Boolean move(byte[] key, int dbIndex);

    /**
     * 以秒为单位获取 {@code key} 的生存时间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/ttl">Redis 文档: TTL</a>
     */
    Long ttl(byte[] key);

    /**
     * 获取 {@code key} 的生存时间并将其转换为给定的 {@link TimeUnit}
     *
     * @param key      不得为 {@literal null}
     * @param timeUnit 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/ttl">Redis 文档: TTL</a>
     */
    Long ttl(byte[] key, TimeUnit timeUnit);

    /**
     * 以毫秒为单位获取 {@code key} 的精确生存时间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/pttl">Redis 文档: PTTL</a>
     */
    Long pTtl(byte[] key);

    /**
     * 获取 {@code key} 的精确生存时间并将其转换为给定的 {@link TimeUnit}
     *
     * @param key      不得为 {@literal null}
     * @param timeUnit 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/pttl">Redis 文档: PTTL</a>
     */
    Long pTtl(byte[] key, TimeUnit timeUnit);

    /**
     * 对 {@code key} 的元素进行排序
     *
     * @param key    不得为 {@literal null}
     * @param params 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    List<byte[]> sort(byte[] key, SortParameters params);

    /**
     * 对 {@code key} 的元素进行排序并将结果存储在 {@code storeKey} 中
     *
     * @param key      不得为 {@literal null}
     * @param params   不得为 {@literal null}
     * @param storeKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    Long sort(byte[] key, SortParameters params, byte[] storeKey);

    /**
     * 检索存储在 {@code key} 中的值的序列化版本
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @see <a href="https://redis.io/commands/dump">Redis 文档: DUMP</a>
     */
    byte[] dump(byte[] key);

    /**
     * 使用之前使用 {@link #dump(byte[])} 获得的 {@code serializedValue} 创建 {@code key}
     *
     * @param key             不得为 {@literal null}
     * @param ttlInMillis     不得为 {@literal null}
     * @param serializedValue 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/restore">Redis 文档: RESTORE</a>
     */
    default void restore(byte[] key, long ttlInMillis, byte[] serializedValue) {
        restore(key, ttlInMillis, serializedValue, false);
    }

    /**
     * 使用之前使用 {@link #dump(byte[])} 获得的 {@code serializedValue} 创建 {@code key}
     *
     * @param key             不得为 {@literal null}
     * @param ttlInMillis     不得为 {@literal null}
     * @param serializedValue 不得为 {@literal null}
     * @param replace         使用 {@literal true} 替换可能存在的值而不是错误
     * @see <a href="https://redis.io/commands/restore">Redis 文档: RESTORE</a>
     */
    void restore(byte[] key, long ttlInMillis, byte[] serializedValue, boolean replace);

    /**
     * 获取用于在给定 {@code key} 处存储值的内部表示类型
     *
     * @param key 不得为 {@literal null}
     * @return {@link ValueEncoding.RedisValueEncoding#VACANT} 如果键不存在 或 {@literal null} 在管道/事务中使用时
     * @throws IllegalArgumentException 如果 {@code key} 是 {@literal null}
     * @see <a href="https://redis.io/commands/object">Redis 文档: OBJECT ENCODING</a>
     */
    ValueEncoding encodingOf(byte[] key);

    /**
     * 获取 {@link Duration}，因为存储在给定 {@code key} 的对象是空闲的
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @throws IllegalArgumentException 如果 {@code key} 是 {@literal null}
     * @see <a href="https://redis.io/commands/object">Redis 文档: OBJECT IDLETIME</a>
     */
    Duration idletime(byte[] key);

    /**
     * 获取与指定 {@code key} 关联的值的引用数
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 如果键不存在或在管道/事务中使用
     * @throws IllegalArgumentException 如果 {@code key} 是 {@literal null}
     * @see <a href="https://redis.io/commands/object">Redis 文档: OBJECT REFCOUNT</a>
     */
    Long refcount(byte[] key);
}
