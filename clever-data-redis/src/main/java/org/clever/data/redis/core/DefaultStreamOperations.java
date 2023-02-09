package org.clever.data.redis.core;

import org.clever.core.convert.ConversionService;
import org.clever.data.domain.Range;
import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.RedisZSetCommands.Limit;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoConsumers;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoGroups;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoStream;
import org.clever.data.redis.hash.HashMapper;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@link ListOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:51 <br/>
 */
class DefaultStreamOperations<K, HK, HV> extends AbstractOperations<K, Object> implements StreamOperations<K, HK, HV> {
    private final StreamObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    DefaultStreamOperations(RedisTemplate<K, ?> template, HashMapper<? super K, ? super HK, ? super HV> mapper) {
        super((RedisTemplate<K, Object>) template);
        this.objectMapper = new StreamObjectMapper(mapper) {
            @Override
            protected HashMapper<?, ?, ?> doGetHashMapper(ConversionService conversionService, Class<?> targetType) {
                if (isSimpleType(targetType)) {
                    return new HashMapper<Object, Object, Object>() {
                        @Override
                        public Map<Object, Object> toHash(Object object) {
                            Object key = "payload";
                            Object value = object;
                            if (!template.isEnableDefaultSerializer()) {
                                if (template.getHashKeySerializer() == null) {
                                    key = key.toString().getBytes(StandardCharsets.UTF_8);
                                }
                                if (template.getHashValueSerializer() == null) {
                                    value = serializeHashValueIfRequires((HV) object);
                                }
                            }
                            return Collections.singletonMap(key, value);
                        }

                        @Override
                        public Object fromHash(Map<Object, Object> hash) {
                            Object value = hash.values().iterator().next();
                            if (ClassUtils.isAssignableValue(targetType, value)) {
                                return value;
                            }
                            HV deserialized = deserializeHashValue((byte[]) value);
                            if (ClassUtils.isAssignableValue(targetType, deserialized)) {
                                return value;
                            }
                            return conversionService.convert(deserialized, targetType);
                        }
                    };
                }
                return super.doGetHashMapper(conversionService, targetType);
            }
        };
    }

    @Override
    public Long acknowledge(K key, String group, String... recordIds) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xAck(rawKey, group, recordIds));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecordId add(Record<K, ?> record) {
        Assert.notNull(record, "Record must not be null");
        MapRecord<K, HK, HV> input = StreamObjectMapper.toMapRecord(this, record);
        ByteRecord binaryRecord = input.serialize(keySerializer(), hashKeySerializer(), hashValueSerializer());
        return execute(connection -> connection.xAdd(binaryRecord));
    }

    @Override
    public Long delete(K key, RecordId... recordIds) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xDel(rawKey, recordIds));
    }

    @Override
    public String createGroup(K key, ReadOffset readOffset, String group) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xGroupCreate(rawKey, group, readOffset, true));
    }

    @Override
    public Boolean deleteConsumer(K key, Consumer consumer) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xGroupDelConsumer(rawKey, consumer));
    }

    @Override
    public Boolean destroyGroup(K key, String group) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xGroupDestroy(rawKey, group));
    }

    @Override
    public XInfoStream info(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xInfo(rawKey));
    }

    @Override
    public XInfoConsumers consumers(K key, String group) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xInfoConsumers(rawKey, group));
    }

    @Override
    public XInfoGroups groups(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xInfoGroups(rawKey));
    }

    @Override
    public PendingMessages pending(K key, String group, Range<?> range, long count) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xPending(rawKey, group, range, count));
    }

    @Override
    public PendingMessages pending(K key, Consumer consumer, Range<?> range, long count) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xPending(rawKey, consumer, range, count));
    }

    @Override
    public PendingMessagesSummary pending(K key, String group) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xPending(rawKey, group));
    }

    @Override
    public Long size(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xLen(rawKey));
    }

    @Override
    public List<MapRecord<K, HK, HV>> range(K key, Range<String> range, Limit limit) {
        return execute(new RecordDeserializingRedisCallback() {
            @Override
            List<ByteRecord> inRedis(RedisConnection connection) {
                return connection.xRange(rawKey(key), range, limit);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MapRecord<K, HK, HV>> read(StreamReadOptions readOptions, StreamOffset<K>... streams) {
        return execute(new RecordDeserializingRedisCallback() {
            @Override
            List<ByteRecord> inRedis(RedisConnection connection) {
                return connection.xRead(readOptions, rawStreamOffsets(streams));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MapRecord<K, HK, HV>> read(Consumer consumer, StreamReadOptions readOptions, StreamOffset<K>... streams) {
        return execute(new RecordDeserializingRedisCallback() {
            @Override
            List<ByteRecord> inRedis(RedisConnection connection) {
                return connection.xReadGroup(consumer, readOptions, rawStreamOffsets(streams));
            }
        });
    }

    @Override
    public List<MapRecord<K, HK, HV>> reverseRange(K key, Range<String> range, Limit limit) {
        return execute(new RecordDeserializingRedisCallback() {
            @Override
            List<ByteRecord> inRedis(RedisConnection connection) {
                return connection.xRevRange(rawKey(key), range, limit);
            }
        });
    }

    @Override
    public Long trim(K key, long count) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xTrim(rawKey, count));
    }

    @Override
    public Long trim(K key, long count, boolean approximateTrimming) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.xTrim(rawKey, count, approximateTrimming));
    }

    @Override
    public <V> HashMapper<V, HK, HV> getHashMapper(Class<V> targetType) {
        return objectMapper.getHashMapper(targetType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapRecord<K, HK, HV> deserializeRecord(ByteRecord record) {
        return record.deserialize(keySerializer(), hashKeySerializer(), hashValueSerializer());
    }

    protected byte[] serializeHashValueIfRequires(HV value) {
        return hashValueSerializerPresent() ?
                serialize(value, hashValueSerializer()) :
                objectMapper.getConversionService().convert(value, byte[].class);
    }

    protected boolean hashValueSerializerPresent() {
        return hashValueSerializer() != null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private byte[] serialize(Object value, RedisSerializer serializer) {
        Object _value = value;
        if (!serializer.canSerialize(value.getClass())) {
            _value = objectMapper.getConversionService().convert(value, serializer.getTargetType());
        }
        return serializer.serialize(_value);
    }

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    private StreamOffset<byte[]>[] rawStreamOffsets(StreamOffset<K>[] streams) {
        return Arrays.stream(streams)
                .map(it -> StreamOffset.create(rawKey(it.getKey()), it.getOffset()))
                .toArray(it -> new StreamOffset[it]);
    }

    abstract class RecordDeserializingRedisCallback implements RedisCallback<List<MapRecord<K, HK, HV>>> {
        public final List<MapRecord<K, HK, HV>> doInRedis(RedisConnection connection) {
            List<ByteRecord> raw = inRedis(connection);
            if (raw == null) {
                return Collections.emptyList();
            }
            List<MapRecord<K, HK, HV>> result = new ArrayList<>();
            for (ByteRecord record : raw) {
                result.add(deserializeRecord(record));
            }
            return result;
        }

        abstract List<ByteRecord> inRedis(RedisConnection connection);
    }
}
