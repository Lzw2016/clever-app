package org.clever.data.redis.core;

import org.clever.data.domain.Range;
import org.clever.data.redis.connection.RedisZSetCommands.Limit;
import org.clever.data.redis.connection.stream.*;

import java.util.List;
import java.util.Map;

/**
 * 绑定到某个键的Redis流特定操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 13:45 <br/>
 */
public interface BoundStreamOperations<K, HK, HV> {
    /**
     * 确认一个或多个记录已处理
     *
     * @param group     消费者组的名称
     * @param recordIds 记录 Id 以确认
     * @return 确认记录的长度。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xack">Redis 文档: XACK</a>
     */
    Long acknowledge(String group, String... recordIds);

    /**
     * 将记录附加到流 {@code key}
     *
     * @param body 记录体
     * @return 记录 ID。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xadd">Redis 文档: XADD</a>
     */
    RecordId add(Map<HK, HV> body);

    /**
     * 从流中删除指定的条目。返回删除的项目数，如果某些 ID 不存在，则可能与传递的 ID 数不同。
     *
     * @param recordIds 流记录 ID
     * @return 删除的条目数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xdel">Redis 文档: XDEL</a>
     */
    Long delete(String... recordIds);

    /**
     * 创建消费组
     *
     * @param group 消费者组的名称
     * @return {@literal true} 如果成功。{@literal null} 在管道/事务中使用时。
     */
    String createGroup(ReadOffset readOffset, String group);

    /**
     * 从消费者组中删除消费者
     *
     * @param consumer 由组名和消费者密钥标识的消费者
     * @return {@literal true} 如果成功。{@literal null} 在管道/事务中使用时。
     */
    Boolean deleteConsumer(Consumer consumer);

    /**
     * 销毁一个消费者组
     *
     * @param group 消费者组的名称
     * @return {@literal true} 如果成功。{@literal null} 在管道/事务中使用时。
     */
    Boolean destroyGroup(String group);

    /**
     * 获取流的长度
     *
     * @return 流的长度。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xlen">Redis 文档: XLEN</a>
     */
    Long size();

    /**
     * 从特定 {@link Range} 内的流中读取记录
     *
     * @param range 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    default List<MapRecord<K, HK, HV>> range(Range<String> range) {
        return range(range, Limit.unlimited());
    }

    /**
     * 从应用 {@link Limit} 的特定 {@link Range} 中的流中读取记录
     *
     * @param range 不得为 {@literal null}
     * @param limit 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    List<MapRecord<K, HK, HV>> range(Range<String> range, Limit limit);

    /**
     * 从 {@link ReadOffset} 读取记录
     *
     * @param readOffset 要读取的偏移量
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    default List<MapRecord<K, HK, HV>> read(ReadOffset readOffset) {
        return read(StreamReadOptions.empty(), readOffset);
    }

    /**
     * 从 {@link ReadOffset} 开始读取记录
     *
     * @param readOptions 阅读论据
     * @param readOffset  要读取的偏移量
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    List<MapRecord<K, HK, HV>> read(StreamReadOptions readOptions, ReadOffset readOffset);

    /**
     * 从 {@link ReadOffset} 开始读取记录。使用消费者组
     *
     * @param consumer   consumer/group
     * @param readOffset 要读取的偏移量
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    default List<MapRecord<K, HK, HV>> read(Consumer consumer, ReadOffset readOffset) {
        return read(consumer, StreamReadOptions.empty(), readOffset);
    }

    /**
     * 从 {@link ReadOffset} 开始读取记录。使用消费者组
     *
     * @param consumer    consumer/group
     * @param readOptions 读取参数
     * @param readOffset  要读取的偏移量
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    List<MapRecord<K, HK, HV>> read(Consumer consumer, StreamReadOptions readOptions, ReadOffset readOffset);

    /**
     * 以相反顺序从特定 {@link Range} 中的流中读取记录
     *
     * @param range 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    default List<MapRecord<K, HK, HV>> reverseRange(Range<String> range) {
        return reverseRange(range, Limit.unlimited());
    }

    /**
     * 从特定 {@link Range} 中的流中读取记录，以相反的顺序应用 {@link Limit}
     *
     * @param range 不得为 {@literal null}
     * @param limit 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    List<MapRecord<K, HK, HV>> reverseRange(Range<String> range, Limit limit);

    /**
     * 将流修剪为 {@code count} 个元素
     *
     * @param count 流的长度
     * @return 删除的条目数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xtrim">Redis 文档: XTRIM</a>
     */
    Long trim(long count);

    /**
     * 将流修剪为 {@code count} 个元素
     *
     * @param count               流的长度
     * @param approximateTrimming 修剪必须以近似的方式进行，以最大限度地提高性能
     * @return 删除的条目数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xtrim">Redis 文档: XTRIM</a>
     */
    Long trim(long count, boolean approximateTrimming);
}
