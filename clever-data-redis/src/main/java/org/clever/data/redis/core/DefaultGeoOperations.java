package org.clever.data.redis.core;

import org.clever.data.geo.*;
import org.clever.data.redis.connection.RedisGeoCommands;
import org.clever.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.clever.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.clever.data.redis.domain.geo.GeoReference;
import org.clever.data.redis.domain.geo.GeoReference.GeoMemberReference;
import org.clever.data.redis.domain.geo.GeoShape;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link GeoOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:54 <br/>
 */
class DefaultGeoOperations<K, M> extends AbstractOperations<K, M> implements GeoOperations<K, M> {

    /**
     * 创建新的 {@link DefaultGeoOperations}
     *
     * @param template 不得为 {@literal null}
     */
    DefaultGeoOperations(RedisTemplate<K, M> template) {
        super(template);
    }

    @Override
    public Long add(K key, Point point, M member) {
        byte[] rawKey = rawKey(key);
        byte[] rawMember = rawValue(member);
        return execute(connection -> connection.geoAdd(rawKey, point, rawMember));
    }

    @Override
    public Long add(K key, GeoLocation<M> location) {
        return add(key, location.getPoint(), location.getName());
    }

    @Override
    public Long add(K key, Map<M, Point> memberCoordinateMap) {
        byte[] rawKey = rawKey(key);
        Map<byte[], Point> rawMemberCoordinateMap = new HashMap<>();
        for (M member : memberCoordinateMap.keySet()) {
            byte[] rawMember = rawValue(member);
            rawMemberCoordinateMap.put(rawMember, memberCoordinateMap.get(member));
        }
        return execute(connection -> connection.geoAdd(rawKey, rawMemberCoordinateMap));
    }

    @Override
    public Long add(K key, Iterable<GeoLocation<M>> locations) {
        Map<M, Point> memberCoordinateMap = new LinkedHashMap<>();
        for (GeoLocation<M> location : locations) {
            memberCoordinateMap.put(location.getName(), location.getPoint());
        }
        return add(key, memberCoordinateMap);
    }

    @Override
    public Distance distance(K key, M member1, M member2) {
        byte[] rawKey = rawKey(key);
        byte[] rawMember1 = rawValue(member1);
        byte[] rawMember2 = rawValue(member2);
        return execute(connection -> connection.geoDist(rawKey, rawMember1, rawMember2));
    }

    @Override
    public Distance distance(K key, M member1, M member2, Metric metric) {
        byte[] rawKey = rawKey(key);
        byte[] rawMember1 = rawValue(member1);
        byte[] rawMember2 = rawValue(member2);
        return execute(connection -> connection.geoDist(rawKey, rawMember1, rawMember2, metric));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> hash(K key, M... members) {
        byte[] rawKey = rawKey(key);
        // noinspection ConfusingArgumentToVarargsMethod
        byte[][] rawMembers = rawValues(members);
        return execute(connection -> connection.geoHash(rawKey, rawMembers));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Point> position(K key, M... members) {
        byte[] rawKey = rawKey(key);
        // noinspection ConfusingArgumentToVarargsMethod
        byte[][] rawMembers = rawValues(members);
        return execute(connection -> connection.geoPos(rawKey, rawMembers));
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(K key, Circle within) {
        byte[] rawKey = rawKey(key);
        GeoResults<GeoLocation<byte[]>> raw = execute(connection -> connection.geoRadius(rawKey, within));
        return deserializeGeoResults(raw);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(K key, Circle within, GeoRadiusCommandArgs args) {
        byte[] rawKey = rawKey(key);
        GeoResults<GeoLocation<byte[]>> raw = execute(connection -> connection.geoRadius(rawKey, within, args));
        return deserializeGeoResults(raw);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(K key, M member, double radius) {
        byte[] rawKey = rawKey(key);
        byte[] rawMember = rawValue(member);
        GeoResults<GeoLocation<byte[]>> raw = execute(connection -> connection.geoRadiusByMember(rawKey, rawMember, radius));
        return deserializeGeoResults(raw);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(K key, M member, Distance distance) {
        byte[] rawKey = rawKey(key);
        byte[] rawMember = rawValue(member);
        GeoResults<GeoLocation<byte[]>> raw = execute(connection -> connection.geoRadiusByMember(rawKey, rawMember, distance));
        return deserializeGeoResults(raw);
    }

    @Override
    public GeoResults<GeoLocation<M>> radius(K key, M member, Distance distance, GeoRadiusCommandArgs param) {
        byte[] rawKey = rawKey(key);
        byte[] rawMember = rawValue(member);
        GeoResults<GeoLocation<byte[]>> raw = execute(connection -> connection.geoRadiusByMember(rawKey, rawMember, distance, param));
        return deserializeGeoResults(raw);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long remove(K key, M... members) {
        byte[] rawKey = rawKey(key);
        // noinspection ConfusingArgumentToVarargsMethod
        byte[][] rawMembers = rawValues(members);
        return execute(connection -> connection.zRem(rawKey, rawMembers));
    }

    @Override
    public GeoResults<GeoLocation<M>> search(K key, GeoReference<M> reference, GeoShape geoPredicate, RedisGeoCommands.GeoSearchCommandArgs args) {
        byte[] rawKey = rawKey(key);
        GeoReference<byte[]> rawMember = getGeoReference(reference);
        GeoResults<GeoLocation<byte[]>> raw = execute(connection -> connection.geoSearch(rawKey, rawMember, geoPredicate, args));
        return deserializeGeoResults(raw);
    }

    @Override
    public Long searchAndStore(K key, K destKey, GeoReference<M> reference, GeoShape geoPredicate, RedisGeoCommands.GeoSearchStoreCommandArgs args) {
        byte[] rawKey = rawKey(key);
        byte[] rawDestKey = rawKey(destKey);
        GeoReference<byte[]> rawMember = getGeoReference(reference);
        return execute(connection -> connection.geoSearchStore(rawDestKey, rawKey, rawMember, geoPredicate, args));
    }

    @SuppressWarnings("unchecked")
    private GeoReference<byte[]> getGeoReference(GeoReference<M> reference) {
        return reference instanceof GeoReference.GeoMemberReference ?
                GeoReference.fromMember(rawValue(((GeoMemberReference<M>) reference).getMember())) :
                (GeoReference<byte[]>) reference;
    }
}
