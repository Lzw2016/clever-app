//package org.clever.data.redis.core.convert;
//
//import org.clever.data.convert.EntityConverter;
//import org.clever.data.redis.core.convert.IndexResolver;
//import org.clever.data.redis.core.convert.RedisData;
//import org.clever.data.redis.core.mapping.RedisMappingContext;
//import org.clever.data.redis.core.mapping.RedisPersistentEntity;
//import org.clever.data.redis.core.mapping.RedisPersistentProperty;
//
///**
// * Redis 特定的 {@link EntityConverter}
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/31 13:36 <br/>
// */
//public interface RedisConverter extends EntityConverter<RedisPersistentEntity<?>, RedisPersistentProperty, Object, RedisData> {
//    @Override
//    RedisMappingContext getMappingContext();
//
//    /**
//     * @return 配置的{@link IndexResolver}，可能是{@literal null}
//     */
//    IndexResolver getIndexResolver();
//}
