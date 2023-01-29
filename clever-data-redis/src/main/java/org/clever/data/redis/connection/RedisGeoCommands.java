package org.clever.data.redis.connection;

import org.clever.data.domain.Sort;
import org.clever.data.geo.*;
import org.clever.data.redis.domain.geo.GeoReference;
import org.clever.data.redis.domain.geo.GeoShape;
import org.clever.util.Assert;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地理特定的 Redis 命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:25 <br/>
 */
public interface RedisGeoCommands {
    /**
     * 将具有给定成员 {@literal name} 的 {@link Point} 添加到 {@literal key}
     *
     * @param key    不得为 {@literal null}
     * @param point  不得为 {@literal null}
     * @param member 不得为 {@literal null}
     * @return 添加的元素数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long geoAdd(byte[] key, Point point, byte[] member);

    /**
     * 将 {@link GeoLocation} 添加到 {@literal key}
     *
     * @param key      不得为 {@literal null}
     * @param location 不得为 {@literal null}
     * @return 添加的元素数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    default Long geoAdd(byte[] key, GeoLocation<byte[]> location) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(location, "Location must not be null!");
        return geoAdd(key, location.getPoint(), location.getName());
    }

    /**
     * 将成员 {@link Point} 对的 {@link Map} 添加到 {@literal key}
     *
     * @param key                 不得为 {@literal null}
     * @param memberCoordinateMap 不得为 {@literal null}
     * @return 添加的元素数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long geoAdd(byte[] key, Map<byte[], Point> memberCoordinateMap);

    /**
     * 将 {@link GeoLocation} 添加到 {@literal key}
     *
     * @param key       不得为 {@literal null}
     * @param locations 不得为 {@literal null}
     * @return 添加的元素数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geoadd">Redis 文档: GEOADD</a>
     */
    Long geoAdd(byte[] key, Iterable<GeoLocation<byte[]>> locations);

    /**
     * 获取 {@literal member1} 和 {@literal member2} 之间的 {@link Distance}
     *
     * @param key     不得为 {@literal null}
     * @param member1 不得为 {@literal null}
     * @param member2 不得为 {@literal null}
     * @return 可以是 {@literal null}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geodist">Redis 文档: GEODIST</a>
     */
    Distance geoDist(byte[] key, byte[] member1, byte[] member2);

    /**
     * 在给定的 {@link Metric} 中获取 {@literal member1} 和 {@literal member2} 之间的 {@link Distance}
     *
     * @param key     不得为 {@literal null}
     * @param member1 不得为 {@literal null}
     * @param member2 不得为 {@literal null}
     * @param metric  不得为 {@literal null}
     * @return 可以是 {@literal null}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geodist">Redis 文档: GEODIST</a>
     */
    Distance geoDist(byte[] key, byte[] member1, byte[] member2, Metric metric);

    /**
     * 获取一个或多个 {@literal member} 位置的 Geohash 表示
     *
     * @param key     不得为 {@literal null}
     * @param members 不得为 {@literal null}
     * @return 当键或成员不存在时为空列表。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geohash">Redis 文档: GEOHASH</a>
     */
    List<String> geoHash(byte[] key, byte[]... members);

    /**
     * 获取一个或多个 {@literal member} 位置的 {@link Point} 表示
     *
     * @param key     不得为 {@literal null}
     * @param members 不得为 {@literal null}
     * @return 当成员键不存在时为空 {@link List}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geopos">Redis 文档: GEOPOS</a>
     */
    List<Point> geoPos(byte[] key, byte[]... members);

    /**
     * 获取给定 {@link Circle} 边界内的 {@literal member}
     *
     * @param key    不得为 {@literal null}
     * @param within 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/georadius">Redis 文档: GEORADIUS</a>
     */
    GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within);

    /**
     * 应用 {@link GeoRadiusCommandArgs} 在给定的 {@link Circle} 边界内获取 {@literal member}
     *
     * @param key    不得为 {@literal null}
     * @param within 不得为 {@literal null}
     * @param args   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/georadius">Redis 文档: GEORADIUS</a>
     */
    GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within, GeoRadiusCommandArgs args);

    /**
     * 获取由 {@literal members} 坐标和给定的 {@literal radius} 定义的圆内的 {@literal member}
     *
     * @param key    不得为 {@literal null}
     * @param member 不得为 {@literal null}
     * @param radius 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/georadiusbymember">Redis 文档: GEORADIUSBYMEMBER</a>
     */
    default GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, double radius) {
        return geoRadiusByMember(key, member, new Distance(radius, DistanceUnit.METERS));
    }

    /**
     * 获取由 {@literal members} 坐标定义的圆内的 {@literal member} 并给定 {@link Distance}
     *
     * @param key    不得为 {@literal null}
     * @param member 不得为 {@literal null}
     * @param radius 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时.
     * @see <a href="https://redis.io/commands/georadiusbymember">Redis 文档: GEORADIUSBYMEMBER</a>
     */
    GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius);

    /**
     * 在给定 {@link Distance} 和 {@link GeoRadiusCommandArgs} 的情况下，获取由 {@literal members} 坐标定义的圆内的 {@literal member}
     *
     * @param key    不得为 {@literal null}
     * @param member 不得为 {@literal null}
     * @param radius 不得为 {@literal null}
     * @param args   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/georadiusbymember">Redis 文档: GEORADIUSBYMEMBER</a>
     */
    GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius, GeoRadiusCommandArgs args);

    /**
     * 删除 {@literal member}
     *
     * @param key     不得为 {@literal null}
     * @param members 不得为 {@literal null}
     * @return 删除的元素数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrem">Redis 文档: ZREM</a>
     */
    Long geoRemove(byte[] key, byte[]... members);

    /**
     * 返回由给定 {@link GeoShape shape} 指定的区域边界内的地理集成员。查询的中心点由 {@link GeoReference} 提供。
     *
     * @param key       不得为 {@literal null}
     * @param reference 不得为 {@literal null}
     * @param predicate 不得为 {@literal null}
     * @param args      不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    GeoResults<GeoLocation<byte[]>> geoSearch(byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchCommandArgs args);

    /**
     * 查询位于给定 {@link GeoShape shape} 指定区域边界内的地理集成员，并将结果存储在 {@code destKey}。查询的中心点由 {@link GeoReference} 提供。
     *
     * @param key       不得为 {@literal null}
     * @param reference 不得为 {@literal null}
     * @param predicate 不得为 {@literal null}
     * @param args      不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/geosearch">Redis 文档: GEOSEARCH</a>
     */
    Long geoSearchStore(byte[] destKey, byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchStoreCommandArgs args);

    /**
     * 与 {@link RedisGeoCommands} 一起使用的参数
     */
    interface GeoCommandArgs {
        /**
         * @return 可以是 {@literal null}。
         */
        Sort.Direction getSortDirection();

        /**
         * @return 可以是 {@literal null}。
         */
        Long getLimit();

        /**
         * @return 从不 {@literal null}
         */
        Set<? extends GeoCommandArgs.GeoCommandFlag> getFlags();

        /**
         * @return {@literal true} 如果设置了 {@literal limit}
         */
        default boolean hasLimit() {
            return getLimit() != null;
        }

        /**
         * @return {@literal true} 如果已设置 {@literal sort}
         */
        default boolean hasSortDirection() {
            return getSortDirection() != null;
        }

        /**
         * @return {@literal true} 如果 {@literal flags} 不为空
         */
        default boolean hasFlags() {
            return !getFlags().isEmpty();
        }

        /**
         * 要使用的标志
         */
        interface GeoCommandFlag {
            static GeoCommandArgs.GeoCommandFlag any() {
                return GeoRadiusCommandArgs.Flag.ANY;
            }

            static GeoCommandArgs.GeoCommandFlag withCord() {
                return GeoRadiusCommandArgs.Flag.WITHCOORD;
            }

            static GeoCommandArgs.GeoCommandFlag withDist() {
                return GeoRadiusCommandArgs.Flag.WITHDIST;
            }

            static GeoCommandArgs.GeoCommandFlag storeDist() {
                return GeoRadiusCommandArgs.Flag.STOREDIST;
            }
        }
    }

    /**
     * 与 {@link RedisGeoCommands} 一起使用的其他参数（如 count/sort/...）
     */
    class GeoSearchCommandArgs implements GeoCommandArgs, Cloneable {
        protected final Set<GeoCommandFlag> flags = new LinkedHashSet<>(2, 1);
        protected Long limit;
        protected Sort.Direction sortDirection;

        private GeoSearchCommandArgs() {
        }

        /**
         * 创建新的 {@link GeoSearchCommandArgs}
         *
         * @return 从不 {@literal null}
         */
        public static GeoSearchCommandArgs newGeoSearchArgs() {
            return new GeoSearchCommandArgs();
        }

        /**
         * 设置 {@link GeoRadiusCommandArgs.Flag#WITHCOORD} 标志以同时返回匹配项目的经度、纬度坐标。
         */
        public GeoSearchCommandArgs includeCoordinates() {
            flags.add(GeoCommandFlag.withCord());
            return this;
        }

        /**
         * 设置 {@link GeoRadiusCommandArgs.Flag#WITHDIST} 标志以返回返回的项目与指定中心的距离。
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchCommandArgs includeDistance() {
            flags.add(GeoCommandFlag.withDist());
            return this;
        }

        /**
         * 应用排序方向
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchCommandArgs sort(Sort.Direction direction) {
            Assert.notNull(direction, "Sort direction must not be null");
            this.sortDirection = direction;
            return this;
        }

        /**
         * 相对于中心，从最近到最远对返回的项目进行排序
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchCommandArgs sortAscending() {
            return sort(Sort.Direction.ASC);
        }

        /**
         * 相对于中心，从最远到最近对返回的项目进行排序
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchCommandArgs sortDescending() {
            return sort(Sort.Direction.DESC);
        }

        /**
         * 将结果限制为前 N 个匹配项
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchCommandArgs limit(long count) {
            return limit(count, false);
        }

        /**
         * 将结果限制为前 N 个匹配项
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchCommandArgs limit(long count, boolean any) {
            Assert.isTrue(count > 0, "Count has to positive value.");
            limit = count;
            if (any) {
                flags.add(GeoCommandFlag.any());
            }
            return this;
        }

        /**
         * @return 从不 {@literal null}
         */
        public Set<? extends GeoCommandFlag> getFlags() {
            return flags;
        }

        /**
         * @return 可以是 {@literal null}。
         */
        public Long getLimit() {
            return limit;
        }

        /**
         * @return 可以是 {@literal null}。
         */
        public Sort.Direction getSortDirection() {
            return sortDirection;
        }

        public boolean hasAnyLimit() {
            return hasLimit() && flags.contains(GeoCommandFlag.any());
        }

        @Override
        protected GeoSearchCommandArgs clone() {
            GeoSearchCommandArgs that = new GeoSearchCommandArgs();
            that.flags.addAll(this.flags);
            that.limit = this.limit;
            that.sortDirection = this.sortDirection;
            return that;
        }
    }

    /**
     * 与 {@link RedisGeoCommands} 一起使用的其他参数（如 count/sort/...）
     */
    class GeoSearchStoreCommandArgs implements GeoCommandArgs, Cloneable {
        private final Set<GeoCommandFlag> flags = new LinkedHashSet<>(2, 1);
        private Long limit;
        private Sort.Direction sortDirection;

        private GeoSearchStoreCommandArgs() {
        }

        /**
         * 创建新的 {@link GeoSearchStoreCommandArgs}
         *
         * @return 从不 {@literal null}
         */
        public static GeoSearchStoreCommandArgs newGeoSearchStoreArgs() {
            return new GeoSearchStoreCommandArgs();
        }

        /**
         * 设置 {@link GeoRadiusCommandArgs.Flag#STOREDIST} 标志以同时存储返回项目与指定中心的距离
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchStoreCommandArgs storeDistance() {
            flags.add(GeoRadiusCommandArgs.Flag.STOREDIST);
            return this;
        }

        /**
         * 应用排序方向
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchStoreCommandArgs sort(Sort.Direction direction) {
            Assert.notNull(direction, "Sort direction must not be null");
            sortDirection = Sort.Direction.ASC;
            return this;
        }

        /**
         * 相对于中心，从最近到最远对返回的项目进行排序
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchStoreCommandArgs sortAscending() {
            return sort(Sort.Direction.ASC);
        }

        /**
         * 相对于中心，从最远到最近对返回的项目进行排序
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchStoreCommandArgs sortDescending() {
            return sort(Sort.Direction.DESC);
        }

        /**
         * 将结果限制为前 N 个匹配项
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchStoreCommandArgs limit(long count) {
            return limit(count, false);
        }

        /**
         * 将结果限制为前 N 个匹配项
         *
         * @return 从不 {@literal null}
         */
        public GeoSearchStoreCommandArgs limit(long count, boolean any) {
            Assert.isTrue(count > 0, "Count has to positive value.");
            this.limit = count;
            if (any) {
                flags.add(GeoCommandFlag.any());
            }
            return this;
        }

        /**
         * @return 从不 {@literal null}
         */
        public Set<GeoCommandFlag> getFlags() {
            return flags;
        }

        /**
         * @return 可以是 {@literal null}。
         */
        public Long getLimit() {
            return limit;
        }

        /**
         * @return 可以是 {@literal null}。
         */
        public Sort.Direction getSortDirection() {
            return sortDirection;
        }

        public boolean isStoreDistance() {
            return flags.contains(GeoCommandFlag.storeDist());
        }

        public boolean hasAnyLimit() {
            return hasLimit() && flags.contains(GeoCommandFlag.any());
        }

        @Override
        protected GeoSearchStoreCommandArgs clone() {
            GeoSearchStoreCommandArgs that = new GeoSearchStoreCommandArgs();
            that.flags.addAll(this.flags);
            that.limit = this.limit;
            that.sortDirection = this.sortDirection;
            return that;
        }
    }

    /**
     * 与 {@link RedisGeoCommands} 一起使用的其他参数（如 count/sort/...）
     */
    class GeoRadiusCommandArgs extends GeoSearchCommandArgs implements Cloneable {
        private GeoRadiusCommandArgs() {
        }

        /**
         * 创建新的 {@link GeoRadiusCommandArgs}
         *
         * @return 从不 {@literal null}
         */
        public static GeoRadiusCommandArgs newGeoRadiusArgs() {
            return new GeoRadiusCommandArgs();
        }

        /**
         * 设置 {@link GeoRadiusCommandArgs.Flag#WITHCOORD} 标志以同时返回匹配项目的经度、纬度坐标
         */
        public GeoRadiusCommandArgs includeCoordinates() {
            super.includeCoordinates();
            return this;
        }

        /**
         * 设置 {@link GeoRadiusCommandArgs.Flag#WITHDIST} 标志以返回返回的项目与指定中心的距离
         *
         * @return 从不 {@literal null}
         */
        public GeoRadiusCommandArgs includeDistance() {
            super.includeDistance();
            return this;
        }

        /**
         * 应用排序方向
         *
         * @return 从不 {@literal null}
         */
        public GeoRadiusCommandArgs sort(Sort.Direction direction) {
            super.sort(direction);
            return this;
        }

        /**
         * 相对于中心，从最近到最远对返回的项目进行排序
         *
         * @return 从不 {@literal null}
         */
        public GeoRadiusCommandArgs sortAscending() {
            super.sortAscending();
            return this;
        }

        /**
         * 相对于中心，从最远到最近对返回的项目进行排序
         *
         * @return 从不 {@literal null}
         */
        public GeoRadiusCommandArgs sortDescending() {
            super.sortDescending();
            return this;
        }

        /**
         * 将结果限制为前 N 个匹配项
         *
         * @return 从不 {@literal null}
         */
        public GeoRadiusCommandArgs limit(long count) {
            super.limit(count);
            return this;
        }

        public Set<GeoRadiusCommandArgs.Flag> getFlags() {
            return flags.stream().map(it -> (GeoRadiusCommandArgs.Flag) it).collect(Collectors.toSet());
        }

        public enum Flag implements GeoCommandFlag {
            WITHCOORD, WITHDIST, ANY, STOREDIST
        }

        @Override
        protected GeoRadiusCommandArgs clone() {
            GeoRadiusCommandArgs that = new GeoRadiusCommandArgs();
            that.flags.addAll(this.flags);
            that.limit = this.limit;
            that.sortDirection = this.sortDirection;
            return that;
        }
    }

    /**
     * {@link GeoLocation} 表示与 {@literal name} 关联的 {@link Point}
     */
    class GeoLocation<T> extends org.clever.data.redis.domain.geo.GeoLocation<T> {
        public GeoLocation(T name, Point point) {
            super(name, point);
        }

        public String toString() {
            return "GeoLocation(name=" + this.getName() + ", point=" + this.getPoint() + ")";
        }
    }

    /**
     * {@link Metric} 受 Redis 支持
     */
    enum DistanceUnit implements Metric {
        METERS(6378137, "m"),
        KILOMETERS(6378.137, "km"),
        MILES(3963.191, "mi"),
        FEET(20925646.325, "ft");

        private final double multiplier;
        private final String abbreviation;

        /**
         * 使用给定的乘数创建一个新的 {@link DistanceUnit}
         *
         * @param multiplier 地球赤道半径
         */
        DistanceUnit(double multiplier, String abbreviation) {
            this.multiplier = multiplier;
            this.abbreviation = abbreviation;
        }

        public double getMultiplier() {
            return multiplier;
        }

        @Override
        public String getAbbreviation() {
            return abbreviation;
        }
    }
}
