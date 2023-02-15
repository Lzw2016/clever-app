package org.clever.data.redis.hash;

import org.clever.data.redis.core.convert.*;
import org.clever.data.redis.core.mapping.RedisMappingContext;
import org.clever.data.util.TypeInformation;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * {@link HashMapper} 基于 {@link MappingRedisConverter}。支持嵌套属性和简单类型，如 {@link String}
 *
 * <pre>{@code
 * class Person {
 *     String firstname;
 *     String lastname;
 *
 *     List<String> nicknames;
 *     List<Person> coworkers;
 *
 *     Address address;
 * }
 * // 以上表示为：
 * _class=org.example.Person
 * firstname=rand
 * lastname=al'thor
 * coworkers.[0].firstname=mat
 * coworkers.[0].nicknames.[0]=prince of the ravens
 * coworkers.[1].firstname=perrin
 * coworkers.[1].address.city=two rivers
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:00 <br/>
 */
public class ObjectHashMapper implements HashMapper<Object, byte[], byte[]> {
    private volatile static ObjectHashMapper sharedInstance;
    private final RedisConverter converter;

    /**
     * 创建新的 {@link ObjectHashMapper}
     *
     * @param customConversions 可以是 {@literal null}
     */
    public ObjectHashMapper(org.clever.data.convert.CustomConversions customConversions) {
        MappingRedisConverter mappingConverter = new MappingRedisConverter(new RedisMappingContext(), new NoOpIndexResolver(), new NoOpReferenceResolver());
        mappingConverter.setCustomConversions(customConversions == null ? new RedisCustomConversions() : customConversions);
        mappingConverter.afterPropertiesSet();
        converter = mappingConverter;
    }

    /**
     * 创建新的 {@link ObjectHashMapper}
     */
    public ObjectHashMapper() {
        this(new RedisCustomConversions());
    }

    /**
     * 使用给定的 {@link RedisConverter} 创建一个新的 {@link ObjectHashMapper} 进行转换
     *
     * @param converter 不得为 {@literal null}
     * @throws IllegalArgumentException 如果给定的 {@literal converter} 是 {@literal null}
     */
    public ObjectHashMapper(RedisConverter converter) {
        Assert.notNull(converter, "Converter must not be null!");
        this.converter = converter;
    }

    /**
     * 返回一个共享的默认 {@link ObjectHashMapper} 实例，在需要时懒惰地构建它。
     * <p>
     * <b>注意:</b> 我们强烈建议为自定义目的构建单独的 {@link ObjectHashMapper} 实例。
     * 此访问器仅用作需要简单类型强制但无法以任何其他方式访问寿命更长的 {@link ObjectHashMapper} 实例的代码路径的回退。
     *
     * @return 共享的 {@link ObjectHashMapper} 实例（从不 {@literal null}）
     */
    public static ObjectHashMapper getSharedInstance() {
        ObjectHashMapper cs = sharedInstance;
        if (cs == null) {
            synchronized (ObjectHashMapper.class) {
                cs = sharedInstance;
                if (cs == null) {
                    cs = new ObjectHashMapper();
                    sharedInstance = cs;
                }
            }
        }
        return cs;
    }

    @Override
    public Map<byte[], byte[]> toHash(Object source) {
        if (source == null) {
            return Collections.emptyMap();
        }
        RedisData sink = new RedisData();
        converter.write(source, sink);
        return sink.getBucket().rawMap();
    }

    @Override
    public Object fromHash(Map<byte[], byte[]> hash) {
        if (hash == null || hash.isEmpty()) {
            return null;
        }
        return converter.read(Object.class, new RedisData(hash));
    }

    /**
     * 将 {@code hash}（Map）转换为对象并返回转换结果
     */
    public <T> T fromHash(Map<byte[], byte[]> hash, Class<T> type) {
        return type.cast(fromHash(hash));
    }

    /**
     * {@link ReferenceResolver} 实现总是返回一个空的 {@link Map}
     */
    private static class NoOpReferenceResolver implements ReferenceResolver {
        private static final Map<byte[], byte[]> NO_REFERENCE = Collections.emptyMap();

        @Override
        public Map<byte[], byte[]> resolveReference(Object id, String keyspace) {
            return NO_REFERENCE;
        }
    }

    /**
     * {@link IndexResolver} 总是返回一个空的 {@link Set}
     */
    private static class NoOpIndexResolver implements IndexResolver {
        private static final Set<IndexedData> NO_INDEXES = Collections.emptySet();

        @Override
        public Set<IndexedData> resolveIndexesFor(TypeInformation<?> typeInformation, Object value) {
            return NO_INDEXES;
        }

        @Override
        public Set<IndexedData> resolveIndexesFor(String keyspace, String path, TypeInformation<?> typeInformation, Object value) {
            return NO_INDEXES;
        }
    }
}
