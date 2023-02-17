package org.clever.data.redis.core;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.support.DefaultConversionService;
import org.clever.data.convert.CustomConversions;
import org.clever.data.redis.connection.stream.MapRecord;
import org.clever.data.redis.connection.stream.ObjectRecord;
import org.clever.data.redis.connection.stream.Record;
import org.clever.data.redis.connection.stream.StreamRecords;
import org.clever.data.redis.core.convert.RedisCustomConversions;
import org.clever.data.redis.hash.HashMapper;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 为 Stream 对象转换提供 {@link HashMapper} 的实用程序。
 * <p>
 * 此实用程序可以使用通用的 {@link HashMapper} 或专门适应 {@link ObjectHashMapper} 将传入数据转换为字节数组的要求。
 * 可以对此类进行子类化以覆盖特定对象映射策略的模板方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:59 <br/>
 *
 * @see ObjectHashMapper
 * @see #doGetHashMapper(ConversionService, Class)
 */
class StreamObjectMapper {
    private final static RedisCustomConversions customConversions = new RedisCustomConversions();
    private final static ConversionService conversionService;

    private final HashMapper<Object, Object, Object> mapper;
    private final HashMapper<Object, Object, Object> objectHashMapper;

    static {
        DefaultConversionService cs = new DefaultConversionService();
        customConversions.registerConvertersIn(cs);
        conversionService = cs;
    }

    /**
     * 创建一个新的 {@link StreamObjectMapper}
     *
     * @param mapper 配置的 {@link HashMapper}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    StreamObjectMapper(HashMapper<?, ?, ?> mapper) {
        Assert.notNull(mapper, "HashMapper must not be null");
        this.mapper = (HashMapper) mapper;
        // TODO ObjectHashMapper
//        if (mapper instanceof ObjectHashMapper) {
//            ObjectHashMapper ohm = (ObjectHashMapper) mapper;
//            this.objectHashMapper = new HashMapper<Object, Object, Object>() {
//                @Override
//                public Map<Object, Object> toHash(Object object) {
//                    return (Map) ohm.toHash(object);
//                }
//
//                @Override
//                public Object fromHash(Map<Object, Object> hash) {
//                    Map<byte[], byte[]> map = hash.entrySet().stream()
//                            .collect(Collectors.toMap(
//                                    e -> conversionService.convert(e.getKey(), byte[].class),
//                                    e -> conversionService.convert(e.getValue(), byte[].class)
//                            ));
//                    return ohm.fromHash(map);
//                }
//            };
//        } else {
//            this.objectHashMapper = null;
//        }
        this.objectHashMapper = null;
    }

    /**
     * 将给定的 {@link Record} 转换为 {@link MapRecord}
     *
     * @param provider {@link HashMapper} 的提供者为 {@link ObjectRecord} 应用映射
     * @param source   源值
     * @return 转换后的 {@link MapRecord}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <K, V, HK, HV> MapRecord<K, HK, HV> toMapRecord(HashMapperProvider<HK, HV> provider, Record<K, V> source) {
        if (source instanceof ObjectRecord) {
            ObjectRecord entry = ((ObjectRecord) source);
            if (entry.getValue() instanceof Map) {
                return StreamRecords.newRecord().in(source.getStream()).withId(source.getId()).ofMap((Map) entry.getValue());
            }
            return entry.toMapRecord(provider.getHashMapper(entry.getValue().getClass()));
        }
        if (source instanceof MapRecord) {
            return (MapRecord<K, HK, HV>) source;
        }
        return Record.of(((HashMapper) provider.getHashMapper(source.getClass())).toHash(source)).withStreamKey(source.getStream());
    }

    /**
     * 将给定的 {@link Record} 转换为 {@link ObjectRecord}
     *
     * @param source     源值
     * @param provider   {@link HashMapper} 的提供者为 {@link ObjectRecord} 应用映射
     * @param targetType 所需的目标类型
     * @return 转换后的 {@link ObjectRecord}
     */
    static <K, V, HK, HV> ObjectRecord<K, V> toObjectRecord(MapRecord<K, HK, HV> source, HashMapperProvider<HK, HV> provider, Class<V> targetType) {
        return source.toObjectRecord(provider.getHashMapper(targetType));
    }

    /**
     * 将 {@link MapRecord}s 的 {@link List} 映射到 {@link ObjectRecord} 的 {@link List}。
     * 针对空、单元素和多元素列表转换进行了优化。
     *
     * @param records            应映射的 {@link MapRecord}
     * @param hashMapperProvider 从中获取实际 {@link HashMapper} 的提供程序。 不得为 {@literal null}
     * @param targetType         请求的 {@link Class target type}
     * @return 如果 {@code records} 为 {@literal null}，则生成的 {@link List} 的 {@link ObjectRecord} 或 {@literal null}
     */
    static <K, V, HK, HV> List<ObjectRecord<K, V>> toObjectRecords(List<MapRecord<K, HK, HV>> records, HashMapperProvider<HK, HV> hashMapperProvider, Class<V> targetType) {
        if (records == null) {
            return null;
        }
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        if (records.size() == 1) {
            return Collections.singletonList(toObjectRecord(records.get(0), hashMapperProvider, targetType));
        }
        List<ObjectRecord<K, V>> transformed = new ArrayList<>(records.size());
        HashMapper<V, HK, HV> hashMapper = hashMapperProvider.getHashMapper(targetType);
        for (MapRecord<K, HK, HV> record : records) {
            transformed.add(record.toObjectRecord(hashMapper));
        }
        return transformed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    final <V, HK, HV> HashMapper<V, HK, HV> getHashMapper(Class<V> targetType) {
        return (HashMapper) doGetHashMapper(conversionService, targetType);
    }

    /**
     * 返回实际的 {@link HashMapper}。可以被子类覆盖
     *
     * @param conversionService 使用的 {@link ConversionService}
     * @param targetType        目标类型
     * @return 获取特定类型的 {@link HashMapper}
     */
    protected HashMapper<?, ?, ?> doGetHashMapper(ConversionService conversionService, Class<?> targetType) {
        return this.objectHashMapper != null ? objectHashMapper : this.mapper;
    }

    /**
     * 检查给定的类型是否是简单类型，如 {@link CustomConversions#isSimpleType(Class)} 所示
     *
     * @param targetType 要检查的类型。不得为 {@literal null}
     * @return {@literal true} 如果 {@link Class targetType} 是一个简单的类型
     * @see CustomConversions#isSimpleType(Class)
     */
    boolean isSimpleType(Class<?> targetType) {
        return customConversions.isSimpleType(targetType);
    }

    /**
     * @return 使用 {@link ConversionService}
     */
    ConversionService getConversionService() {
        return conversionService;
    }
}
