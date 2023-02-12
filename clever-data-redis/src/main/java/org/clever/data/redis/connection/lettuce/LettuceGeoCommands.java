package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoSearch;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.api.async.RedisGeoAsyncCommands;
import org.clever.core.convert.converter.Converter;
import org.clever.data.geo.*;
import org.clever.data.redis.connection.RedisGeoCommands;
import org.clever.data.redis.domain.geo.GeoReference;
import org.clever.data.redis.domain.geo.GeoShape;
import org.clever.util.Assert;

import java.util.*;
import java.util.Map.Entry;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:32 <br/>
 */
class LettuceGeoCommands implements RedisGeoCommands {
    private final LettuceConnection connection;

    LettuceGeoCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public Long geoAdd(byte[] key, Point point, byte[] member) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(point, "Point must not be null!");
        Assert.notNull(member, "Member must not be null!");
        return connection.invoke().just(RedisGeoAsyncCommands::geoadd, key, point.getX(), point.getY(), member);
    }

    @Override
    public Long geoAdd(byte[] key, Map<byte[], Point> memberCoordinateMap) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(memberCoordinateMap, "MemberCoordinateMap must not be null!");
        List<Object> values = new ArrayList<>();
        for (Entry<byte[], Point> entry : memberCoordinateMap.entrySet()) {
            values.add(entry.getValue().getX());
            values.add(entry.getValue().getY());
            values.add(entry.getKey());
        }
        return geoAdd(key, values);
    }

    @Override
    public Long geoAdd(byte[] key, Iterable<GeoLocation<byte[]>> locations) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(locations, "Locations must not be null!");
        List<Object> values = new ArrayList<>();
        for (GeoLocation<byte[]> location : locations) {
            values.add(location.getPoint().getX());
            values.add(location.getPoint().getY());
            values.add(location.getName());
        }
        return geoAdd(key, values);
    }


    private Long geoAdd(byte[] key, Collection<Object> values) {
        return connection.invoke().just(it -> it.geoadd(key, values.toArray()));
    }

    @Override
    public Distance geoDist(byte[] key, byte[] member1, byte[] member2) {
        return geoDist(key, member1, member2, DistanceUnit.METERS);
    }

    @Override
    public Distance geoDist(byte[] key, byte[] member1, byte[] member2, Metric metric) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(member1, "Member1 must not be null!");
        Assert.notNull(member2, "Member2 must not be null!");
        Assert.notNull(metric, "Metric must not be null!");
        GeoArgs.Unit geoUnit = LettuceConverters.toGeoArgsUnit(metric);
        Converter<Double, Distance> distanceConverter = LettuceConverters.distanceConverterForMetric(metric);
        return connection.invoke().from(RedisGeoAsyncCommands::geodist, key, member1, member2, geoUnit).get(distanceConverter);
    }

    @Override
    public List<String> geoHash(byte[] key, byte[]... members) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(members, "Members must not be null!");
        Assert.noNullElements(members, "Members must not contain null!");
        return connection.invoke().fromMany(RedisGeoAsyncCommands::geohash, key, members).toList(it -> it.getValueOrElse(null));
    }

    @Override
    public List<Point> geoPos(byte[] key, byte[]... members) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(members, "Members must not be null!");
        Assert.noNullElements(members, "Members must not contain null!");
        return connection.invoke().fromMany(RedisGeoAsyncCommands::geopos, key, members).toList(LettuceConverters::geoCoordinatesToPoint);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(within, "Within must not be null!");
        Converter<Set<byte[]>, GeoResults<GeoLocation<byte[]>>> geoResultsConverter = LettuceConverters.bytesSetToGeoResultsConverter();
        return connection.invoke()
                .from(it -> it.georadius(
                        key,
                        within.getCenter().getX(),
                        within.getCenter().getY(),
                        within.getRadius().getValue(),
                        LettuceConverters.toGeoArgsUnit(within.getRadius().getMetric())
                )).get(geoResultsConverter);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within, GeoRadiusCommandArgs args) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(within, "Within must not be null!");
        Assert.notNull(args, "Args must not be null!");
        GeoArgs geoArgs = LettuceConverters.toGeoArgs(args);
        Converter<List<GeoWithin<byte[]>>, GeoResults<GeoLocation<byte[]>>> geoResultsConverter = LettuceConverters.geoRadiusResponseToGeoResultsConverter(within.getRadius().getMetric());
        return connection.invoke().from(it -> it.georadius(
                key,
                within.getCenter().getX(),
                within.getCenter().getY(),
                within.getRadius().getValue(),
                LettuceConverters.toGeoArgsUnit(within.getRadius().getMetric()),
                geoArgs
        )).get(geoResultsConverter);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, double radius) {
        return geoRadiusByMember(key, member, new Distance(radius, DistanceUnit.METERS));
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(member, "Member must not be null!");
        Assert.notNull(radius, "Radius must not be null!");
        GeoArgs.Unit geoUnit = LettuceConverters.toGeoArgsUnit(radius.getMetric());
        Converter<Set<byte[]>, GeoResults<GeoLocation<byte[]>>> converter = LettuceConverters.bytesSetToGeoResultsConverter();
        return connection.invoke().from(RedisGeoAsyncCommands::georadiusbymember, key, member, radius.getValue(), geoUnit).get(converter);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius, GeoRadiusCommandArgs args) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(member, "Member must not be null!");
        Assert.notNull(radius, "Radius must not be null!");
        Assert.notNull(args, "Args must not be null!");
        GeoArgs.Unit geoUnit = LettuceConverters.toGeoArgsUnit(radius.getMetric());
        GeoArgs geoArgs = LettuceConverters.toGeoArgs(args);
        Converter<List<GeoWithin<byte[]>>, GeoResults<GeoLocation<byte[]>>> geoResultsConverter = LettuceConverters.geoRadiusResponseToGeoResultsConverter(radius.getMetric());
        return connection.invoke().from(RedisGeoAsyncCommands::georadiusbymember, key, member, radius.getValue(), geoUnit, geoArgs).get(geoResultsConverter);
    }

    @Override
    public Long geoRemove(byte[] key, byte[]... values) {
        return connection.zSetCommands().zRem(key, values);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public GeoResults<GeoLocation<byte[]>> geoSearch(byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchCommandArgs args) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(reference, "Reference must not be null!");
        Assert.notNull(predicate, "GeoPredicate must not be null!");
        Assert.notNull(args, "GeoSearchCommandArgs must not be null!");
        GeoSearch.GeoRef<byte[]> ref = LettuceConverters.toGeoRef(reference);
        GeoSearch.GeoPredicate lettucePredicate = LettuceConverters.toGeoPredicate(predicate);
        GeoArgs geoArgs = LettuceConverters.toGeoArgs(args);
        return connection.invoke().from(RedisGeoAsyncCommands::geosearch, key, ref, lettucePredicate, geoArgs).get(LettuceConverters.geoRadiusResponseToGeoResultsConverter(predicate.getMetric()));
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Long geoSearchStore(byte[] destKey, byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchStoreCommandArgs args) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(reference, "Reference must not be null!");
        Assert.notNull(predicate, "GeoPredicate must not be null!");
        Assert.notNull(args, "GeoSearchCommandArgs must not be null!");
        GeoSearch.GeoRef<byte[]> ref = LettuceConverters.toGeoRef(reference);
        GeoSearch.GeoPredicate lettucePredicate = LettuceConverters.toGeoPredicate(predicate);
        GeoArgs geoArgs = LettuceConverters.toGeoArgs(args);
        return connection.invoke().just(connection -> connection.geosearchstore(destKey, key, ref, lettucePredicate, geoArgs, args.isStoreDistance()));
    }
}
