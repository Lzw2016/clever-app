package org.clever.data.redis.core;

import org.clever.data.domain.Range;
import org.clever.data.redis.connection.RedisZSetCommands.Limit;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoConsumers;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoGroups;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoStream;
import org.clever.data.redis.hash.HashMapper;
import org.clever.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Redis 流具体操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:57 <br/>
 */
public interface StreamOperations<K, HK, HV> extends HashMapperProvider<HK, HV> {
    /**
     * 确认一个或多个记录已处理
     *
     * @param key       stream key
     * @param group     消费者组的名称
     * @param recordIds 记录 id 以确认
     * @return 确认记录的长度。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xack">Redis 文档: XACK</a>
     */
    Long acknowledge(K key, String group, String... recordIds);

    /**
     * 确认一个或多个记录已处理
     *
     * @param key       stream key
     * @param group     消费者组的名称
     * @param recordIds 记录 id 以确认
     * @return 确认记录的长度。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xack">Redis 文档: XACK</a>
     */
    default Long acknowledge(K key, String group, RecordId... recordIds) {
        return acknowledge(key, group, Arrays.stream(recordIds).map(RecordId::getValue).toArray(String[]::new));
    }

    /**
     * 确认给定记录已处理
     *
     * @param group  消费者组的名称
     * @param record {@link Record} 确认
     * @return 确认记录的长度。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xack">Redis 文档: XACK</a>
     */
    default Long acknowledge(String group, Record<K, ?> record) {
        return acknowledge(record.getStream(), group, record.getId());
    }

    /**
     * 将记录附加到流 {@code key}
     *
     * @param key     stream key
     * @param content 将内容记录为 Map
     * @return 记录 ID。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xadd">Redis 文档: XADD</a>
     */
    default RecordId add(K key, Map<? extends HK, ? extends HV> content) {
        return add(StreamRecords.newRecord().in(key).ofMap(content));
    }

    /**
     * 将一条记录附加到流中，该记录由保存字段值对的 {@link Map} 支持
     *
     * @param record 要追加的记录
     * @return 记录 ID。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xadd">Redis 文档: XADD</a>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default RecordId add(MapRecord<K, ? extends HK, ? extends HV> record) {
        return add((Record) record);
    }

    /**
     * 将给定值支持的记录追加到流中。该值被映射为散列并序列化
     *
     * @param record 不得为 {@literal null}
     * @return 记录 ID。 {@literal null} 在管道/事务中使用时。
     * @see MapRecord
     * @see ObjectRecord
     */
    RecordId add(Record<K, ?> record);

    /**
     * 从流中删除指定的记录。返回删除的记录数，如果某些 ID 不存在，则可能与传递的 ID 数不同。
     *
     * @param key       stream key
     * @param recordIds 流记录 ID
     * @return 删除的条目数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xdel">Redis 文档: XDEL</a>
     */
    default Long delete(K key, String... recordIds) {
        return delete(key, Arrays.stream(recordIds).map(RecordId::of).toArray(RecordId[]::new));
    }

    /**
     * 从流中删除给定的 {@link Record}
     *
     * @param record 不得为 {@literal null}
     * @return {@link Mono} 发出已删除记录的数量
     */
    default Long delete(Record<K, ?> record) {
        return delete(record.getStream(), record.getId());
    }

    /**
     * 从流中删除指定的记录。返回删除的记录数，如果某些 ID 不存在，则可能与传递的 ID 数不同
     *
     * @param key       stream key
     * @param recordIds 流记录 ID
     * @return {@link Mono} 发出删除记录的数量
     * @see <a href="https://redis.io/commands/xdel">Redis 文档: XDEL</a>
     */
    Long delete(K key, RecordId... recordIds);

    /**
     * 在 {@link ReadOffset#latest() latest offset} 创建一个消费者组。如果流不存在，此命令会创建该流。
     *
     * @param key   存储流的 {@literal key}
     * @param group 消费者组的名称
     * @return {@literal OK} 如果成功。 {@literal null} 在管道/事务中使用时。
     */
    default String createGroup(K key, String group) {
        return createGroup(key, ReadOffset.latest(), group);
    }

    /**
     * 创建消费组。如果流不存在，此命令会创建该流。
     *
     * @param key        存储流的 {@literal key}
     * @param readOffset {@link ReadOffset} 应用
     * @param group      消费者组的名称
     * @return {@literal OK} 如果成功。{@literal null} 在管道/事务中使用时。
     */
    String createGroup(K key, ReadOffset readOffset, String group);

    /**
     * 从消费者组中删除消费者
     *
     * @param key      stream key
     * @param consumer 由组名和消费者密钥标识的消费者
     * @return {@literal true} 如果成功。 {@literal null} 在管道/事务中使用时。
     */
    Boolean deleteConsumer(K key, Consumer consumer);

    /**
     * 销毁一个消费者组
     *
     * @param key   stream key
     * @param group 消费者组的名称
     * @return {@literal true} 如果成功。{@literal null} 在管道/事务中使用时。
     */
    Boolean destroyGroup(K key, String group);

    /**
     * 为存储在指定 {@literal key} 的流获取有关特定 {@literal consumer group} 中每个消费者的信息
     *
     * @param key   存储流的 {@literal key}
     * @param group {@literal 消费者组} 的名称
     * @return {@literal null} 在管道/事务中使用时。
     */
    XInfoConsumers consumers(K key, String group);

    /**
     * 获取与存储在指定 {@literal key} 的流关联的 {@literal consumer groups} 的信息
     *
     * @param key 存储流的 {@literal key}
     * @return {@literal null} 在管道/事务中使用时。
     */
    XInfoGroups groups(K key);

    /**
     * 获取有关存储在指定 {@literal key} 中的流的一般信息
     *
     * @param key 存储流的 {@literal key}
     * @return {@literal null} 在管道/事务中使用时。
     */
    XInfoStream info(K key);

    /**
     * 获取给定 {@literal consumer group} 的 {@link PendingMessagesSummary}
     *
     * @param key   存储流的 {@literal key}。 不得为 {@literal null}
     * @param group {@literal consumer group} 的名称。不得为 {@literal null}
     * @return 在管道/事务中使用时，给定 {@literal consumer group} 或 {@literal null} 中未决消息的摘要
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    PendingMessagesSummary pending(K key, String group);

    /**
     * 已获取有关给定 {@link Consumer} 的所有待处理消息的详细信息
     *
     * @param key      存储流的 {@literal key}。不得为 {@literal null}
     * @param consumer 消费者为其获取 {@link PendingMessages}。不得为 {@literal null}
     * @return 给定 {@link Consumer} 或的未决消息 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    default PendingMessages pending(K key, Consumer consumer) {
        return pending(key, consumer, Range.unbounded(), -1L);
    }

    /**
     * 获取有关{@literal consumer group}中给定{@link Range} 的未决{@link PendingMessage 消息} 的详细信息。
     *
     * @param key   存储流的 {@literal key}。不得为 {@literal null}
     * @param group {@literal consumer group} 的名称。 不得为 {@literal null}
     * @param range 要在其中搜索的消息 ID 范围。 不得为 {@literal null}
     * @param count 限制结果的数量
     * @return 在管道/事务中使用时，给定 {@literal 消费者组} 或 {@literal null} 的未决消息
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    PendingMessages pending(K key, String group, Range<?> range, long count);

    /**
     * 获取有关给定 {@link Range} 和 {@literal consumer group} 内的 {@link Consumer} 的未决 {@link PendingMessage 消息} 的详细信息
     *
     * @param key      存储流的 {@literal key}。 不得为 {@literal null}
     * @param consumer {@link Consumer} 的名称。不得为 {@literal null}
     * @param range    要在其中搜索的消息 ID 范围。不得为 {@literal null}
     * @param count    限制结果的数量
     * @return 给定 {@link Consumer} 或的未决消息 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    PendingMessages pending(K key, Consumer consumer, Range<?> range, long count);

    /**
     * 获取流的长度
     *
     * @param key stream key
     * @return 流的长度。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xlen">Redis 文档: XLEN</a>
     */
    Long size(K key);

    /**
     * 从特定 {@link Range} 内的流中读取记录
     *
     * @param key   stream key
     * @param range 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    default List<MapRecord<K, HK, HV>> range(K key, Range<String> range) {
        return range(key, range, Limit.unlimited());
    }

    /**
     * 从应用 {@link Limit} 的特定 {@link Range} 中的流中读取记录
     *
     * @param key   stream key
     * @param range 不得为 {@literal null}
     * @param limit 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    List<MapRecord<K, HK, HV>> range(K key, Range<String> range, Limit limit);

    /**
     * 从特定 {@link Range} 中的流中读取所有记录作为 {@link ObjectRecord}
     *
     * @param targetType 有效载荷的目标类型
     * @param key        stream key
     * @param range      不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    default <V> List<ObjectRecord<K, V>> range(Class<V> targetType, K key, Range<String> range) {
        return range(targetType, key, range, Limit.unlimited());
    }

    /**
     * 从特定 {@link Range} 中的流中读取记录，应用 {@link Limit} 作为 {@link ObjectRecord}
     *
     * @param targetType 有效载荷的目标类型
     * @param key        stream key
     * @param range      不得为 {@literal null}
     * @param limit      不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    default <V> List<ObjectRecord<K, V>> range(Class<V> targetType, K key, Range<String> range, Limit limit) {
        Assert.notNull(targetType, "Target type must not be null");
        return map(range(key, range, limit), targetType);
    }

    /**
     * 从一个或多个 {@link StreamOffset} 中读取记录
     *
     * @param streams 要读取的流
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    @SuppressWarnings("unchecked")
    default List<MapRecord<K, HK, HV>> read(StreamOffset<K>... streams) {
        return read(StreamReadOptions.empty(), streams);
    }

    /**
     * 从一个或多个 {@link StreamOffset} 作为 {@link ObjectRecord} 读取记录
     *
     * @param targetType 有效载荷的目标类型
     * @param streams    要读取的流
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    @SuppressWarnings("unchecked")
    default <V> List<ObjectRecord<K, V>> read(Class<V> targetType, StreamOffset<K>... streams) {
        return read(targetType, StreamReadOptions.empty(), streams);
    }

    /**
     * 从一个或多个 {@link StreamOffset} 中读取记录
     *
     * @param readOptions 读取的参数
     * @param streams     要读取的流
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    @SuppressWarnings("unchecked")
    List<MapRecord<K, HK, HV>> read(StreamReadOptions readOptions, StreamOffset<K>... streams);

    /**
     * 从一个或多个 {@link StreamOffset} 作为 {@link ObjectRecord} 读取记录
     *
     * @param targetType  有效载荷的目标类型
     * @param readOptions 读取的参数
     * @param streams     要读取的流。
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    @SuppressWarnings("unchecked")
    default <V> List<ObjectRecord<K, V>> read(Class<V> targetType, StreamReadOptions readOptions, StreamOffset<K>... streams) {
        Assert.notNull(targetType, "Target type must not be null");
        return map(read(readOptions, streams), targetType);
    }

    /**
     * 使用消费者组从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param consumer consumer/group
     * @param streams  要读取的流
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    @SuppressWarnings("unchecked")
    default List<MapRecord<K, HK, HV>> read(Consumer consumer, StreamOffset<K>... streams) {
        return read(consumer, StreamReadOptions.empty(), streams);
    }

    /**
     * 使用消费者组作为 {@link ObjectRecord} 从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param targetType 有效载荷的目标类型
     * @param consumer   consumer/group
     * @param streams    要读取的流
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    @SuppressWarnings("unchecked")
    default <V> List<ObjectRecord<K, V>> read(Class<V> targetType, Consumer consumer, StreamOffset<K>... streams) {
        return read(targetType, consumer, StreamReadOptions.empty(), streams);
    }

    /**
     * 使用消费者组从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param consumer    consumer/group
     * @param readOptions 读取的参数
     * @param streams     要读取的流
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    @SuppressWarnings("unchecked")
    List<MapRecord<K, HK, HV>> read(Consumer consumer, StreamReadOptions readOptions, StreamOffset<K>... streams);

    /**
     * 使用消费者组作为 {@link ObjectRecord} 从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param targetType  有效载荷的目标类型
     * @param consumer    consumer/group.
     * @param readOptions 读取的参数
     * @param streams     要读取的流
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    @SuppressWarnings("unchecked")
    default <V> List<ObjectRecord<K, V>> read(Class<V> targetType, Consumer consumer, StreamReadOptions readOptions, StreamOffset<K>... streams) {
        Assert.notNull(targetType, "Target type must not be null");
        return map(read(consumer, readOptions, streams), targetType);
    }

    /**
     * 以相反顺序从特定 {@link Range} 中的流中读取记录
     *
     * @param key   stream key
     * @param range 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    default List<MapRecord<K, HK, HV>> reverseRange(K key, Range<String> range) {
        return reverseRange(key, range, Limit.unlimited());
    }

    /**
     * 从特定 {@link Range} 中的流中读取记录，以相反的顺序应用 {@link Limit}
     *
     * @param key   stream key
     * @param range 不得为 {@literal null}
     * @param limit 不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    List<MapRecord<K, HK, HV>> reverseRange(K key, Range<String> range, Limit limit);

    /**
     * 以与 {@link ObjectRecord} 相反的顺序从特定 {@link Range} 中的流中读取记录
     *
     * @param targetType 有效载荷的目标类型
     * @param key        stream key
     * @param range      不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    default <V> List<ObjectRecord<K, V>> reverseRange(Class<V> targetType, K key, Range<String> range) {
        return reverseRange(targetType, key, range, Limit.unlimited());
    }

    /**
     * 从特定 {@link Range} 中的流中读取记录，以与 {@link ObjectRecord} 相反的顺序应用 {@link Limit}
     *
     * @param targetType 有效载荷的目标类型
     * @param key        stream key
     * @param range      不得为 {@literal null}
     * @param limit      不得为 {@literal null}
     * @return 列出结果流的成员。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    default <V> List<ObjectRecord<K, V>> reverseRange(Class<V> targetType, K key, Range<String> range, Limit limit) {
        Assert.notNull(targetType, "Target type must not be null");
        return map(reverseRange(key, range, limit), targetType);
    }

    /**
     * 将流修剪为 {@code count} 个元素
     *
     * @param key   stream key
     * @param count 流的长度
     * @return 删除的条目数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xtrim">Redis 文档: XTRIM</a>
     */
    Long trim(K key, long count);

    /**
     * 将流修剪为 {@code count} 个元素
     *
     * @param key                 stream key
     * @param count               流的长度
     * @param approximateTrimming 修剪必须以近似的方式进行，以最大限度地提高性能
     * @return 删除的条目数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/xtrim">Redis 文档: XTRIM</a>
     */
    Long trim(K key, long count, boolean approximateTrimming);

    /**
     * 获取特定类型的 {@link HashMapper}
     *
     * @param targetType 不得为 {@literal null}
     * @return 适用于给定类型的 {@link HashMapper}
     */
    @Override
    <V> HashMapper<V, HK, HV> getHashMapper(Class<V> targetType);

    /**
     * 将记录从 {@link MapRecord} 映射到 {@link ObjectRecord}
     *
     * @param record     流记录 Map
     * @param targetType 有效载荷的目标类型
     * @return the mapped {@link ObjectRecord}.
     */
    default <V> ObjectRecord<K, V> map(MapRecord<K, HK, HV> record, Class<V> targetType) {
        Assert.notNull(record, "Record must not be null");
        Assert.notNull(targetType, "Target type must not be null");
        return StreamObjectMapper.toObjectRecord(record, this, targetType);
    }

    /**
     * 将记录从 {@link MapRecord} 映射到 {@link ObjectRecord}
     *
     * @param records    流记录 Map
     * @param targetType 有效负载的目标类型
     * @return 映射的 {@link ObjectRecord object records}.
     */
    default <V> List<ObjectRecord<K, V>> map(List<MapRecord<K, HK, HV>> records, Class<V> targetType) {
        Assert.notNull(records, "Records must not be null");
        Assert.notNull(targetType, "Target type must not be null");
        return StreamObjectMapper.toObjectRecords(records, this, targetType);
    }

    /**
     * 使用配置的序列化程序将 {@link ByteRecord} 反序列化为 {@link MapRecord}
     *
     * @param record 流记录 Map
     * @return 反序列化 {@link MapRecord}
     */
    MapRecord<K, HK, HV> deserializeRecord(ByteRecord record);
}
