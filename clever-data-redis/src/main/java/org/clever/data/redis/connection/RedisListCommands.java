package org.clever.data.redis.connection;

import org.clever.util.CollectionUtils;

import java.util.List;

/**
 * Redis 支持的列表特定命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:15 <br/>
 */
public interface RedisListCommands {
    /**
     * 列表插入位置
     */
    enum Position {
        BEFORE, AFTER
    }

    /**
     * 列表移动方向
     */
    enum Direction {
        LEFT, RIGHT;

        /**
         * {@link RedisListCommands.Direction#LEFT} 的别名
         */
        public static RedisListCommands.Direction first() {
            return LEFT;
        }

        /**
         * {@link RedisListCommands.Direction#RIGHT} 的别名
         */
        public static RedisListCommands.Direction last() {
            return RIGHT;
        }
    }

    /**
     * 将 {@code values} 附加到 {@code key}
     *
     * @param key    不能是 {@literal null}
     * @param values 不得为空
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/rpush">Redis 文档: RPUSH</a>
     */
    Long rPush(byte[] key, byte[]... values);

    /**
     * 返回存储在给定 {@literal key} 的列表中匹配元素的索引. <br />
     * 需要 Redis 6.0.6 或更新版本.
     *
     * @param key     不能是 {@literal null}
     * @param element 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/lpos">Redis 文档: LPOS</a>
     */
    default Long lPos(byte[] key, byte[] element) {
        return CollectionUtils.firstElement(lPos(key, element, null, null));
    }

    /**
     * 返回存储在给定 {@literal key} 的列表中匹配元素的索引. <br />
     * 需要 Redis 6.0.6 或更新版本.
     *
     * @param key     不能是 {@literal null}
     * @param element 不能是 {@literal null}
     * @param rank    指定要返回的第一个元素的“排名”，以防有多个匹配项。等级 1 表示返回第一个匹配项，等级 2 表示返回第二个匹配项，依此类推
     * @param count   要返回的匹配项数
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/lpos">Redis 文档: LPOS</a>
     */
    List<Long> lPos(byte[] key, byte[] element, Integer rank, Integer count);

    /**
     * 将 {@code values} 添加到 {@code key}
     *
     * @param key    不能是 {@literal null}
     * @param values 不得为空
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    Long lPush(byte[] key, byte[]... values);

    /**
     * 仅当列表存在时才将 {@code values} 附加到 {@code key}
     *
     * @param key   不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/rpushx">Redis 文档: RPUSHX</a>
     */
    Long rPushX(byte[] key, byte[] value);

    /**
     * 仅当列表存在时才将 {@code values} 添加到 {@code key}
     *
     * @param key   不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/lpushx">Redis 文档: LPUSHX</a>
     */
    Long lPushX(byte[] key, byte[] value);

    /**
     * 获取存储在 {@code key} 的列表的大小
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/llen">Redis 文档: LLEN</a>
     */
    Long lLen(byte[] key);

    /**
     * 从位于 {@code key} 的列表中获取 {@code start} 和 {@code end} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @return 如果键不存在或范围不包含值，则为空 {@link List}。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lrange">Redis 文档: LRANGE</a>
     */
    List<byte[]> lRange(byte[] key, long start, long end);

    /**
     * 将 {@code key} 处的列表修剪为 {@code start} 和 {@code end} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @see <a href="https://redis.io/commands/ltrim">Redis 文档: LTRIM</a>
     */
    void lTrim(byte[] key, long start, long end);

    /**
     * 在 {@code key} 的 {@code index} 表单列表中获取元素
     *
     * @param key   不能是 {@literal null}
     * @param index 零基指数值。使用负数指定从尾部开始的元素
     * @return {@literal null} 当索引超出范围或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/lindex">Redis 文档: LINDEX</a>
     */
    byte[] lIndex(byte[] key, long index);

    /**
     * 为 {@code key} 插入 {@code value} {@link RedisListCommands.Position#BEFORE}
     * 或 {@link RedisListCommands.Position#AFTER} 现有的 {@code pivot}
     *
     * @param key   不能是 {@literal null}
     * @param where 不能是 {@literal null}
     * @param pivot 不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/linsert">Redis 文档: LINSERT</a>
     */
    Long lInsert(byte[] key, RedisListCommands.Position where, byte[] pivot, byte[] value);

    /**
     * 原子地返回并删除存储在 {@code sourceKey} 列表的第一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素推送到 first/last 元素（head/tail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的列表
     *
     * @param sourceKey      不能是 {@literal null}
     * @param destinationKey 不能是 {@literal null}
     * @param from           不能是 {@literal null}
     * @param to             不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/lmove">Redis 文档: LMOVE</a>
     * @see #bLMove(byte[], byte[], RedisListCommands.Direction, RedisListCommands.Direction, double)
     */
    byte[] lMove(byte[] sourceKey, byte[] destinationKey, RedisListCommands.Direction from, RedisListCommands.Direction to);

    /**
     * 原子地返回并删除存储在 {@code sourceKey} 列表的第一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素推送到 first/last 元素（head/tail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的列表.
     * <p>
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param sourceKey      不能是 {@literal null}
     * @param destinationKey 不能是 {@literal null}
     * @param from           不能是 {@literal null}
     * @param to             不能是 {@literal null}
     * @param timeout        不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/blmove">Redis 文档: BLMOVE</a>
     * @see #lMove(byte[], byte[], RedisListCommands.Direction, RedisListCommands.Direction)
     */
    byte[] bLMove(byte[] sourceKey, byte[] destinationKey, RedisListCommands.Direction from, RedisListCommands.Direction to, double timeout);

    /**
     * 在 {@code index} 设置 {@code value} 列表元素
     *
     * @param key   不能是 {@literal null}
     * @param index 不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/lset">Redis 文档: LSET</a>
     */
    void lSet(byte[] key, long index, byte[] value);

    /**
     * 从存储在 {@code key} 的列表中删除前 {@code count} 次出现的 {@code value}
     *
     * @param key   不能是 {@literal null}
     * @param count 不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/lrem">Redis 文档: LREM</a>
     */
    Long lRem(byte[] key, long count, byte[] value);

    /**
     * 删除并返回存储在 {@code key} 列表中的第一个元素
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/lpop">Redis 文档: LPOP</a>
     */
    byte[] lPop(byte[] key);

    /**
     * 删除并返回存储在 {@code key} 列表中的第一个 {@code} 元素
     *
     * @param key   不能是 {@literal null}
     * @param count 不能是 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/lpop">Redis 文档: LPOP</a>
     */
    List<byte[]> lPop(byte[] key, long count);

    /**
     * 删除并返回存储在 {@code key} 列表中的最后一个元素
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/rpop">Redis 文档: RPOP</a>
     */
    byte[] rPop(byte[] key);

    /**
     * 删除并返回存储在 {@code key} 列表中的最后一个 {@code} 元素
     *
     * @param key   不能是 {@literal null}
     * @param count 不能是 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/rpop">Redis 文档: RPOP</a>
     */
    List<byte[]> rPop(byte[] key, long count);

    /**
     * 从存储在 {@code keys} 的列表中删除并返回第一个元素. <br>
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param timeout 阻塞秒数
     * @param keys    不能是 {@literal null}
     * @return 当无法弹出任何元素且达到超时时为空 {@link List}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/blpop">Redis 文档: BLPOP</a>
     * @see #lPop(byte[])
     */
    List<byte[]> bLPop(int timeout, byte[]... keys);

    /**
     * 从存储在 {@code keys} 的列表中删除并返回最后一个元素. <br>
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param timeout seconds to block.
     * @param keys    不能是 {@literal null}
     * @return 空 {@link List}，当无法弹出任何元素并且达到超时时。{@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/brpop">Redis 文档: BRPOP</a>
     * @see #rPop(byte[])
     */
    List<byte[]> bRPop(int timeout, byte[]... keys);

    /**
     * 从 {@code srcKey} 处的列表中删除最后一个元素，将其附加到 {@code dstKey} 并返回其值
     *
     * @param srcKey 不能是 {@literal null}
     * @param dstKey 不能是 {@literal null}
     * @return 可以是 {@literal null}
     * @see <a href="https://redis.io/commands/rpoplpush">Redis 文档: RPOPLPUSH</a>
     */
    byte[] rPopLPush(byte[] srcKey, byte[] dstKey);

    /**
     * 从 {@code srcKey} 处的列表中删除最后一个元素，将其附加到 {@code dstKey} 并返回其值。 <br>
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param timeout 秒阻止
     * @param srcKey  不能是 {@literal null}
     * @param dstKey  不能是 {@literal null}
     * @return 可以是 {@literal null}
     * @see <a href="https://redis.io/commands/brpoplpush">Redis 文档: BRPOPLPUSH</a>
     * @see #rPopLPush(byte[], byte[])
     */
    byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey);
}
