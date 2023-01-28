package org.clever.data.redis.domain.geo;

import org.clever.data.geo.Circle;
import org.clever.data.geo.Point;
import org.clever.data.redis.connection.RedisGeoCommands;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * {@code GEOSEARCH} 和 {@code GEOSEARCHSTORE} 命令的参考点。提供从地理集成员或参考点创建 {@link GeoReference} 的工厂方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:29 <br/>
 */
public interface GeoReference<T> {
    /**
     * 从 geoset 成员创建一个 {@link GeoReference}
     *
     * @param member 不得为 {@literal null}
     */
    static <T> GeoReference<T> fromMember(T member) {
        Assert.notNull(member, "Geoset member must not be null");
        return new GeoMemberReference<>(member);
    }

    /**
     * 从 {@link RedisGeoCommands.GeoLocation geoset member} 创建一个 {@link GeoReference}
     *
     * @param member 不得为 {@literal null}
     */
    static <T> GeoReference<T> fromMember(RedisGeoCommands.GeoLocation<T> member) {
        Assert.notNull(member, "GeoLocation must not be null");
        return new GeoMemberReference<>(member.getName());
    }

    /**
     * 从 {@link Circle#getCenter() 圆心点} 创建一个 {@link GeoReference}
     *
     * @param within 不得为 {@literal null}
     */
    static <T> GeoReference<T> fromCircle(Circle within) {
        Assert.notNull(within, "Circle must not be null");
        return fromCoordinate(within.getCenter());
    }

    /**
     * 从 WGS84 经/纬度坐标创建 {@link GeoReference}
     */
    static <T> GeoReference<T> fromCoordinate(double longitude, double latitude) {
        return new GeoCoordinateReference<>(longitude, latitude);
    }

    /**
     * 从 WGS84 经/纬度坐标创建 {@link GeoReference}
     *
     * @param location 不得为 {@literal null}
     */
    static <T> GeoReference<T> fromCoordinate(RedisGeoCommands.GeoLocation<?> location) {
        Assert.notNull(location, "GeoLocation must not be null");
        Assert.notNull(location.getPoint(), "GeoLocation point must not be null");
        return fromCoordinate(location.getPoint());
    }

    /**
     * 从 WGS84 经/纬度坐标创建 {@link GeoReference}
     *
     * @param point 不得为 {@literal null}
     */
    static <T> GeoReference<T> fromCoordinate(Point point) {
        Assert.notNull(point, "Reference point must not be null");
        return fromCoordinate(point.getX(), point.getY());
    }

    class GeoMemberReference<T> implements GeoReference<T> {
        private final T member;

        public GeoMemberReference(T member) {
            this.member = member;
        }

        public T getMember() {
            return member;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GeoReference.GeoMemberReference)) {
                return false;
            }
            GeoMemberReference<?> that = (GeoMemberReference<?>) o;
            return ObjectUtils.nullSafeEquals(member, that.member);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(member);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [member=" + member + ']';
        }
    }

    class GeoCoordinateReference<T> implements GeoReference<T> {
        private final double longitude;
        private final double latitude;

        public GeoCoordinateReference(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GeoReference.GeoCoordinateReference)) {
                return false;
            }
            GeoCoordinateReference<?> that = (GeoCoordinateReference<?>) o;
            if (longitude != that.longitude) {
                return false;
            }
            return latitude == that.latitude;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(longitude);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(latitude);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [" + longitude + "," + latitude + ']';
        }
    }
}
