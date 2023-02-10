package org.clever.data.redis.core;

import org.clever.data.domain.Range;
import org.clever.data.redis.connection.DataType;
import org.clever.data.redis.connection.RedisZSetCommands.Limit;
import org.clever.data.redis.connection.stream.*;

import java.util.List;
import java.util.Map;

/**
 * {@link BoundStreamOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:31 <br/>
 */
class DefaultBoundStreamOperations<K, HK, HV> extends DefaultBoundKeyOperations<K> implements BoundStreamOperations<K, HK, HV> {
    private final StreamOperations<K, HK, HV> ops;

    /**
     * 构造一个新的<code>DefaultBoundSetOperations</code>实例
     */
    DefaultBoundStreamOperations(K key, RedisOperations<K, ?> operations) {
        super(key, operations);
        this.ops = operations.opsForStream();
    }

    @Override
    public Long acknowledge(String group, String... recordIds) {
        return ops.acknowledge(getKey(), group, recordIds);
    }

    @Override
    public RecordId add(Map<HK, HV> body) {
        return ops.add(getKey(), body);
    }

    @Override
    public Long delete(String... recordIds) {
        return ops.delete(getKey(), recordIds);
    }

    @Override
    public String createGroup(ReadOffset readOffset, String group) {
        return ops.createGroup(getKey(), readOffset, group);
    }

    @Override
    public Boolean deleteConsumer(Consumer consumer) {
        return ops.deleteConsumer(getKey(), consumer);
    }

    @Override
    public Boolean destroyGroup(String group) {
        return ops.destroyGroup(getKey(), group);
    }

    @Override
    public Long size() {
        return ops.size(getKey());
    }

    @Override
    public List<MapRecord<K, HK, HV>> range(Range<String> range, Limit limit) {
        return ops.range(getKey(), range, limit);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MapRecord<K, HK, HV>> read(StreamReadOptions readOptions, ReadOffset readOffset) {
        return ops.read(readOptions, StreamOffset.create(getKey(), readOffset));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MapRecord<K, HK, HV>> read(Consumer consumer, StreamReadOptions readOptions, ReadOffset readOffset) {
        return ops.read(consumer, readOptions, StreamOffset.create(getKey(), readOffset));
    }

    @Override
    public List<MapRecord<K, HK, HV>> reverseRange(Range<String> range, Limit limit) {
        return ops.reverseRange(getKey(), range, limit);
    }

    @Override
    public Long trim(long count) {
        return trim(count, false);
    }

    @Override
    public Long trim(long count, boolean approximateTrimming) {
        return ops.trim(getKey(), count, approximateTrimming);
    }

    @Override
    public DataType getType() {
        return DataType.STREAM;
    }
}
