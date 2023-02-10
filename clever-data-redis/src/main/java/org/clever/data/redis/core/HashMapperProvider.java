package org.clever.data.redis.core;

import org.clever.data.redis.hash.HashMapper;

/**
 * 返回给定 {@link Class type} 的 {@link HashMapper} 的函数。
 * <p>
 * 根据所请求的 {@link Class target type} 的序列化策略，此接口的实现者可以返回泛型或特定的 {@link HashMapper} 实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:58 <br/>
 */
@FunctionalInterface
public interface HashMapperProvider<HK, HV> {
    /**
     * 获取特定类型的 {@link HashMapper}
     *
     * @param targetType 不得为 {@literal null}
     * @param <V>        值目标类型
     * @return 适用于给定类型的 {@link HashMapper}
     */
    <V> HashMapper<V, HK, HV> getHashMapper(Class<V> targetType);
}
