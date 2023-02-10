package org.clever.data.redis.core;

import org.clever.data.geo.*;
import org.clever.data.redis.connection.DataType;
import org.clever.data.redis.connection.RedisGeoCommands;
import org.clever.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.clever.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.clever.data.redis.domain.geo.GeoReference;
import org.clever.data.redis.domain.geo.GeoShape;

import java.util.List;
import java.util.Map;

/**
 * {@link BoundGeoOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:17 <br/>
 */
class DefaultBoundGeoOperations<K, M> extends DefaultBoundKeyOperations<K> implements BoundGeoOperations<K, M> {
    private final GeoOperations<K, M> ops;

    /**
     * 构造一个新的 {@code DefaultBoundGeoOperations}
     *
     * @param key        不得为 {@literal null}
     * @param operations 不得为 {@literal null}
     */
    DefaultBoundGeoOperations(K key, RedisOperations<K, M> operations) {
        super(key, operations);
        this.ops = operations.opsForGeo();
    }

    @Override
    public Long add(Point point, M member) {
        return ops.add(getKey(), point, member);
    }

    @Override
    public Long add(GeoLocation<M> location) {
        return ops.add(getKey(), location);
    }

    @Override
    public Long add(Map<M, Point> memberCoordinateMap) {
        return ops.add(getKey(), memberCoordinateMap);
    }

    @Override
    public Long add(Iterable<GeoLocation<M>> locations) {
        return ops.add(getKey(), locations);
    }

    @Override
    public Distance distance(M member1, M member2) {
        return ops.distance(getKey(), member1, member2);
    }

    @Override
    public Distance distance(M member1, M member2, Metric unit) {
        return ops.distance(getKey(), member1, member2, unit);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> hash(M... members) {
        return ops.hash(getKey(), members);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Point> position(M... members) {
        return ops.position(getKey(), members);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(Circle within) {
        return ops.radius(getKey(), within);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(Circle within, GeoRadiusCommandArgs param) {
        return ops.radius(getKey(), within, param);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(K key, M member, double radius) {
        return ops.radius(getKey(), member, radius);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(M member, Distance distance) {
        return ops.radius(getKey(), member, distance);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(M member, Distance distance, GeoRadiusCommandArgs param) {
        return ops.radius(getKey(), member, distance, param);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long remove(M... members) {
        return ops.remove(getKey(), members);
    }

    @Override
    public GeoResults<GeoLocation<M>> search(GeoReference<M> reference, GeoShape geoPredicate, RedisGeoCommands.GeoSearchCommandArgs args) {
        return ops.search(getKey(), reference, geoPredicate, args);
    }

    @Override
    public Long searchAndStore(K destKey, GeoReference<M> reference, GeoShape geoPredicate, RedisGeoCommands.GeoSearchStoreCommandArgs args) {
        return ops.searchAndStore(getKey(), destKey, reference, geoPredicate, args);
    }

    @Override
    public DataType getType() {
        return DataType.ZSET;
    }
}
