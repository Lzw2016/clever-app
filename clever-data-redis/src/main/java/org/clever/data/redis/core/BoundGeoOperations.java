package org.clever.data.redis.core;

import org.clever.data.geo.*;
import org.clever.data.redis.domain.geo.BoundingBox;
import org.clever.data.redis.domain.geo.GeoReference;
import org.clever.data.redis.domain.geo.GeoShape;

import java.util.List;
import java.util.Map;

import static org.clever.data.redis.connection.RedisGeoCommands.*;

/**
 * {@link GeoOperations} 绑定到某个键
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:29 <br/>
 */
public interface BoundGeoOperations<K, M> extends BoundKeyOperations<K> {
    /**
     * 将具有给定成员 {@literal name} 的 {@link Point} 添加到 {@literal key}
     *
     * @param point  不得为 {@literal null}
     * @param member 不得为 {@literal null}
     * @return 添加的元素数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long add(Point point, M member);

    /**
     * 将 {@link GeoLocation} 添加到 {@literal key}
     *
     * @param location 不得为 {@literal null}
     * @return 添加的元素数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long add(GeoLocation<M> location);

    /**
     * 将成员 {@link Point} 对的 {@link Map} 添加到 {@literal key}
     *
     * @param memberCoordinateMap 不得为 {@literal null}
     * @return 添加的元素数。{@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long add(Map<M, Point> memberCoordinateMap);

    /**
     * 将 {@link GeoLocation} 添加到 {@literal key}
     *
     * @param locations 不得为 {@literal null}
     * @return 添加的元素数。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long add(Iterable<GeoLocation<M>> locations);

    /**
     * 获取 {@literal member1} 和 {@literal member2} 之间的 {@link Distance}
     *
     * @param member1 不得为 {@literal null}
     * @param member2 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/geodist">Redis 文档: GEODIST</a>
     */
    Distance distance(M member1, M member2);

    /**
     * 在给定的 {@link Metric} 中获取 {@literal member1} 和 {@literal member2} 之间的 {@link Distance}
     *
     * @param member1 不得为 {@literal null}
     * @param member2 不得为 {@literal null}
     * @param metric  不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/geodist">Redis 文档: GEODIST</a>
     */
    Distance distance(M member1, M member2, Metric metric);

    /**
     * 获取一个或多个 {@literal member} 位置的 Geohash 表示
     *
     * @param members 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geohash">Redis 文档: GEOHASH</a>
     */
    @SuppressWarnings("unchecked")
    List<String> hash(M... members);

    /**
     * 获取一个或多个 {@literal member} 位置的 {@link Point} 表示
     *
     * @param members 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geopos">Redis 文档: GEOPOS</a>
     */
    @SuppressWarnings("unchecked")
    List<Point> position(M... members);

    /**
     * 获取给定 {@link Circle} 边界内的 {@literal member}
     *
     * @param within 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/georadius">Redis 文档: GEORADIUS</a>
     */
    GeoResults<GeoLocation<M>> radius(Circle within);

    /**
     * 应用 {@link GeoRadiusCommandArgs} 获取给定 {@link Circle} 边界内的 {@literal member}
     *
     * @param within 不得为 {@literal null}
     * @param args   不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/georadius">Redis 文档: GEORADIUS</a>
     */
    GeoResults<GeoLocation<M>> radius(Circle within, GeoRadiusCommandArgs args);

    /**
     * 获取由 {@literal members} 坐标和给定的 {@literal radius} 定义的圆内的 {@literal member}
     *
     * @param member 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/georadiusbymember">Redis 文档: GEORADIUSBYMEMBER</a>
     */
    GeoResults<GeoLocation<M>> radius(K key, M member, double radius);

    /**
     * 获取由 {@literal members} 坐标定义的圆内的 {@literal member}，并给定 {@literal radius} 应用 {@link Metric}
     *
     * @param member   不得为 {@literal null}
     * @param distance 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/georadiusbymember">Redis 文档: GEORADIUSBYMEMBER</a>
     */
    GeoResults<GeoLocation<M>> radius(M member, Distance distance);

    /**
     * 获取由 {@literal members} 坐标定义的圆内的 {@literal member}，并给定 {@literal radius} 应用 {@link Metric} 和 {@link GeoRadiusCommandArgs}
     *
     * @param member   不得为 {@literal null}
     * @param distance 不得为 {@literal null}
     * @param args     不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/georadiusbymember">Redis 文档: GEORADIUSBYMEMBER</a>
     */
    GeoResults<GeoLocation<M>> radius(M member, Distance distance, GeoRadiusCommandArgs args);

    /**
     * 删除 {@literal member}
     *
     * @param members 不得为 {@literal null}
     * @return 删除的元素数。{@literal null} 在管道/事务中使用时。
     */
    @SuppressWarnings("unchecked")
    Long remove(M... members);

    /**
     * 获取给定 {@link Circle} 边界内的 {@literal member}
     *
     * @param within 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    default GeoResults<GeoLocation<M>> search(Circle within) {
        return search(
                GeoReference.fromCircle(within),
                GeoShape.byRadius(within.getRadius()),
                GeoSearchCommandArgs.newGeoSearchArgs()
        );
    }

    /**
     * 使用 {@link GeoReference} 作为给定 {@link Distance radius} 边界内的查询中心获取 {@literal member}
     *
     * @param reference 不得为 {@literal null}
     * @param radius    不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    default GeoResults<GeoLocation<M>> search(GeoReference<M> reference, Distance radius) {
        return search(reference, radius, GeoSearchCommandArgs.newGeoSearchArgs());
    }

    /**
     * 使用 {@link GeoReference} 作为查询中心在给定的 {@link Distance radius} 应用 {@link GeoRadiusCommandArgs} 的边界内获取 {@literal member}
     *
     * @param reference 不得为 {@literal null}
     * @param radius    不得为 {@literal null}
     * @param args      不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    default GeoResults<GeoLocation<M>> search(GeoReference<M> reference, Distance radius, GeoSearchCommandArgs args) {
        return search(reference, GeoShape.byRadius(radius), args);
    }

    /**
     * 使用 {@link GeoReference} 作为给定边界框边界内的查询中心获取 {@literal member}
     *
     * @param reference   不得为 {@literal null}
     * @param boundingBox 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    default GeoResults<GeoLocation<M>> search(GeoReference<M> reference, BoundingBox boundingBox) {
        return search(reference, boundingBox, GeoSearchCommandArgs.newGeoSearchArgs());
    }

    /**
     * 在应用 {@link GeoRadiusCommandArgs} 的给定边界框的边界内，使用 {@link GeoReference} 作为查询中心获取 {@literal member}
     *
     * @param reference   不得为 {@literal null}
     * @param boundingBox 不得为 {@literal null}
     * @param args        不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    default GeoResults<GeoLocation<M>> search(GeoReference<M> reference, BoundingBox boundingBox, GeoSearchCommandArgs args) {
        return search(reference, GeoShape.byBox(boundingBox), args);
    }

    /**
     * 使用 {@link GeoReference} 作为查询中心在应用 {@link GeoRadiusCommandArgs} 的给定 {@link GeoShape 谓词} 范围内获取 {@literal member}
     *
     * @param reference    不得为 {@literal null}
     * @param geoPredicate 不得为 {@literal null}
     * @param args         不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    GeoResults<GeoLocation<M>> search(GeoReference<M> reference, GeoShape geoPredicate, GeoSearchCommandArgs args);

    /**
     * 获取给定 {@link Circle} 边界内的 {@literal member} 并将结果存储在 {@code destKey} 中
     *
     * @param within 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearchstore">Redis 文档: GEOSEARCHSTORE</a>
     */
    default Long searchAndStore(K destKey, Circle within) {
        return searchAndStore(
                destKey,
                GeoReference.fromCircle(within),
                GeoShape.byRadius(within.getRadius()),
                GeoSearchStoreCommandArgs.newGeoSearchStoreArgs()
        );
    }

    /**
     * 使用 {@link GeoReference} 作为给定 {@link Distance radius} 边界内的查询中心获取 {@literal member}，并将结果存储在 {@code destKey}
     *
     * @param reference 不得为 {@literal null}
     * @param radius    不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearchstore">Redis 文档: GEOSEARCHSTORE</a>
     */
    default Long searchAndStore(K destKey, GeoReference<M> reference, Distance radius) {
        return searchAndStore(destKey, reference, radius, GeoSearchStoreCommandArgs.newGeoSearchStoreArgs());
    }

    /**
     * 使用 {@link GeoReference} 作为查询中心在给定的 {@link Distance radius} 应用 {@link GeoRadiusCommandArgs} 的边界内获取 {@literal member}，并将结果存储在 {@code destKey}
     *
     * @param reference 不得为 {@literal null}
     * @param radius    不得为 {@literal null}
     * @param args      不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearchstore">Redis 文档: GEOSEARCHSTORE</a>
     */
    default Long searchAndStore(K destKey, GeoReference<M> reference, Distance radius, GeoSearchStoreCommandArgs args) {
        return searchAndStore(destKey, reference, GeoShape.byRadius(radius), args);
    }

    /**
     * 使用 {@link GeoReference} 作为给定边界框边界内的查询中心获取 {@literal member}，并将结果存储在 {@code destKey}
     *
     * @param reference   不得为 {@literal null}
     * @param boundingBox 不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearchstore">Redis 文档: GEOSEARCHSTORE</a>
     */
    default Long searchAndStore(K destKey, GeoReference<M> reference, BoundingBox boundingBox) {
        return searchAndStore(destKey, reference, boundingBox, GeoSearchStoreCommandArgs.newGeoSearchStoreArgs());
    }

    /**
     * 使用 {@link GeoReference} 作为查询中心在应用 {@link GeoRadiusCommandArgs} 的给定边界框边界内获取 {@literal member}，并将结果存储在 {@code destKey}
     *
     * @param reference   不得为 {@literal null}
     * @param boundingBox 不得为 {@literal null}
     * @param args        不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearchstore">Redis 文档: GEOSEARCHSTORE</a>
     */
    default Long searchAndStore(K destKey, GeoReference<M> reference, BoundingBox boundingBox, GeoSearchStoreCommandArgs args) {
        return searchAndStore(destKey, reference, GeoShape.byBox(boundingBox), args);
    }

    /**
     * 使用 {@link GeoReference} 作为查询中心在应用 {@link GeoRadiusCommandArgs} 的给定 {@link GeoShape 谓词} 边界内获取 {@literal member}，并将结果存储在 {@code destKey}
     *
     * @param reference    不得为 {@literal null}
     * @param geoPredicate 不得为 {@literal null}
     * @param args         不得为 {@literal null}
     * @return 永远不要 {@literal null} 除非在管道/交易中使用
     * @see <a href="https://redis.io/commands/geosearchstore">Redis 文档: GEOSEARCHSTORE</a>
     */
    Long searchAndStore(K destKey, GeoReference<M> reference, GeoShape geoPredicate, GeoSearchStoreCommandArgs args);
}
