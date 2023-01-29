package org.clever.data.redis.connection;

import org.clever.data.domain.Range;
import org.clever.data.redis.core.types.Expiration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 支持的特定于 String/Value 的命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:14 <br/>
 */
public interface RedisStringCommands {
    enum BitOperation {
        AND, OR, XOR, NOT
    }

    /**
     * 获取 {@code key} 的值
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道事务中使用时
     * @see <a href="https://redis.io/commands/get">Redis 文档: GET</a>
     */
    byte[] get(byte[] key);

    /**
     * 返回 {@code key} 处的值并删除该键
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getdel">Redis 文档: GETDEL</a>
     */
    byte[] getDel(byte[] key);

    /**
     * 返回 {@code key} 处的值并通过应用 {@link Expiration} 使密钥过期<br/>
     * 对 {@code EX} 使用 {@link Expiration#seconds(long)} <br />
     * 对 {@code PX} 使用 {@link Expiration#milliseconds(long)} <br />
     * 使用 {@link Expiration#unixTimestamp(long, TimeUnit)} 为 {@code EXAT | PXAT}<br />
     *
     * @param key        不得为 {@literal null}
     * @param expiration 不得为 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */
    byte[] getEx(byte[] key, Expiration expiration);

    /**
     * 设置 {@code key} 的 {@code value} 并返回其旧值
     *
     * @param key   不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 如果密钥在管道/事务中使用之前或使用时不存在
     * @see <a href="https://redis.io/commands/getset">Redis 文档: GETSET</a>
     */
    byte[] getSet(byte[] key, byte[] value);

    /**
     * 获取多个{@code keys}。值按照请求的键的顺序缺少字段值在结果{@link List}中使用{@code null}表示
     *
     * @param keys 不得为 {@literal null}
     * @return {@code null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/mget">Redis 文档: MGET</a>
     */
    List<byte[]> mGet(byte[]... keys);

    /**
     * 为 {@code key} 设置 {@code value}
     *
     * @param key   不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean set(byte[] key, byte[] value);

    /**
     * 为 {@code key} 设置 {@code value} 如果设置并根据 {@code option} 插入更/新值，则从 {@code expiration} 应用超时。
     *
     * @param key        不能是 {@literal null}.
     * @param value      不能是 {@literal null}.
     * @param expiration 不得为 {@literal null}。使用 {@link Expiration#persistent()} 不设置任何 ttl 或 {@link Expiration#keepTtl()} 以保持现有的到期时间。
     * @param option     不得为 {@literal null}。使用 {@link RedisStringCommands.SetOption#upsert()} 添加不存在的
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean set(byte[] key, byte[] value, Expiration expiration, RedisStringCommands.SetOption option);

    /**
     * 仅当 {@code key} 不存在时，为 {@code key} 设置 {@code value}
     *
     * @param key   不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/setnx">Redis 文档: SETNX</a>
     */
    Boolean setNX(byte[] key, byte[] value);

    /**
     * 为 {@code key} 设置 {@code value} 和 {@code seconds} 过期时间
     *
     * @param key     不能是 {@literal null}
     * @param seconds 不能是 {@literal null}
     * @param value   不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/setex">Redis 文档: SETEX</a>
     */
    Boolean setEx(byte[] key, long seconds, byte[] value);

    /**
     * 为 {@code key} 设置 {@code value} 和以 {@code milliseconds} 为单位的过期时间
     *
     * @param key          不得为 {@literal null}
     * @param milliseconds 不得为 {@literal null}
     * @param value        不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/psetex">Redis 文档: PSETEX</a>
     */
    Boolean pSetEx(byte[] key, long milliseconds, byte[] value);

    /**
     * 使用 {@code tuple} 中提供的键值对将多个键设置为多个值
     *
     * @param tuple 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/mset">Redis 文档: MSET</a>
     */
    Boolean mSet(Map<byte[], byte[]> tuple);

    /**
     * 仅当提供的键不存在时，才使用 {@code tuple} 中提供的键值对将多个键设置为多个值。
     *
     * @param tuple 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/msetnx">Redis 文档: MSETNX</a>
     */
    Boolean mSetNX(Map<byte[], byte[]> tuple);

    /**
     * 将存储为 {@code key} 的字符串值的整数值增加 1
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/incr">Redis 文档: INCR</a>
     */
    Long incr(byte[] key);

    /**
     * 将 {@code key} 存储的整数值增加 {@code delta}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/incrby">Redis 文档: INCRBY</a>
     */
    Long incrBy(byte[] key, long value);

    /**
     * 将 {@code key} 的浮点数值增加 {@code delta}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/incrbyfloat">Redis 文档: INCRBYFLOAT</a>
     */
    Double incrBy(byte[] key, double value);

    /**
     * 将存储为 {@code key} 字符串值的整数值减 1
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/decr">Redis 文档: DECR</a>
     */
    Long decr(byte[] key);

    /**
     * 将存储为 {@code key} 字符串值的整数值减去 {@code value}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/decrby">Redis 文档: DECRBY</a>
     */
    Long decrBy(byte[] key, long value);

    /**
     * 将 {@code value} 附加到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/append">Redis 文档: APPEND</a>
     */
    Long append(byte[] key, byte[] value);

    /**
     * 获取 {@code start} 和 {@code end} 之间的 {@code key} 值的子串
     *
     * @param key   不得为 {@literal null}
     * @param start 不得为 {@literal null}
     * @param end   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getrange">Redis 文档: GETRANGE</a>
     */
    byte[] getRange(byte[] key, long start, long end);

    /**
     * 用给定的 {@code value} 覆盖从指定的 {@code offset} 开始的 {@code key} 部分
     *
     * @param key    不得为 {@literal null}
     * @param value  不得为 {@literal null}
     * @param offset 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/setrange">Redis 文档: SETRANGE</a>
     */
    void setRange(byte[] key, byte[] value, long offset);

    /**
     * 获取 {@code key} 值的 {@code offset} 位值
     *
     * @param key    不得为 {@literal null}
     * @param offset 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getbit">Redis 文档: GETBIT</a>
     */
    Boolean getBit(byte[] key, long offset);

    /**
     * 将存储在 {@code key} 的值中的位设置为 {@code offset}
     *
     * @param key    不得为 {@literal null}
     * @param offset 不得为 {@literal null}
     * @param value  不得为 {@literal null}
     * @return 存储在 {@code offset} 的原始位值或{@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/setbit">Redis 文档: SETBIT</a>
     */
    Boolean setBit(byte[] key, long offset, boolean value);

    /**
     * 计算存储在 {@code key} 中的设置位数（人口计数）
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/bitcount">Redis 文档: BITCOUNT</a>
     */
    Long bitCount(byte[] key);

    /**
     * 计算在 {@code start} 和 {@code end} 之间存储在 {@code key} 的值的设置位数（人口计数）
     *
     * @param key   不得为 {@literal null}
     * @param start 不得为 {@literal null}
     * @param end   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/bitcount">Redis 文档: BITCOUNT</a>
     */
    Long bitCount(byte[] key, long start, long end);

    /**
     * 获取存储在给定 {@code key} 处的不同位宽和任意非（必要）对齐偏移量的操作特定整数字段
     *
     * @param key         不得为 {@literal null}
     * @param subCommands 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     */
    List<Long> bitField(byte[] key, BitFieldSubCommands subCommands);

    /**
     * 在字符串之间执行按位运算
     *
     * @param op          不得为 {@literal null}
     * @param destination 不得为 {@literal null}
     * @param keys        不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/bitop">Redis 文档: BITOP</a>
     */
    Long bitOp(RedisStringCommands.BitOperation op, byte[] destination, byte[]... keys);

    /**
     * 返回字符串中给定 {@code bit} 的第一个位集的位置
     *
     * @param key 保存实际字符串的键
     * @param bit 要查找的位值
     * @return {@literal null} 在管道/事务中使用时。第一位的位置根据请求设置为1或0
     * @see <a href="https://redis.io/commands/bitpos">Redis 文档: BITPOS</a>
     */
    default Long bitPos(byte[] key, boolean bit) {
        return bitPos(key, bit, Range.unbounded());
    }

    /**
     * 返回字符串中给定 {@code bit} 的第一个位集的位置。<br/>
     * {@link Range} start 和 end 可以包含负值，以便从字符串末尾开始索引 <strong>bytes<strong>，
     * 其中 {@literal -1} 是最后一个字节，{@literal -2} 是倒数第二个
     *
     * @param key   保存实际字符串的键
     * @param bit   要查找的位值
     * @param range 不得为 {@literal null} 使用 {@link Range#unbounded()} 不限制搜索。
     * @return {@literal null} 在管道/事务中使用时。第一位的位置根据请求设置为1或0。
     * @see <a href="https://redis.io/commands/bitpos">Redis 文档: BITPOS</a>
     */
    Long bitPos(byte[] key, boolean bit, Range<Long> range);

    /**
     * 获取存储在 {@code key} 中的值的长度
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/strlen">Redis 文档: STRLEN</a>
     */
    Long strLen(byte[] key);

    /**
     * {@code SET} 命令参数 {@code NX}, {@code XX}
     */
    enum SetOption {
        /**
         * 不要设置任何额外的命令参数
         */
        UPSERT,
        /**
         * {@code NX}
         */
        SET_IF_ABSENT,
        /**
         * {@code XX}
         */
        SET_IF_PRESENT;

        /**
         * 不要设置任何额外的命令参数
         */
        public static RedisStringCommands.SetOption upsert() {
            return UPSERT;
        }

        /**
         * {@code XX}
         */
        public static RedisStringCommands.SetOption ifPresent() {
            return SET_IF_PRESENT;
        }

        /**
         * {@code NX}
         */
        public static RedisStringCommands.SetOption ifAbsent() {
            return SET_IF_ABSENT;
        }
    }
}
