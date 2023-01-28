package org.clever.data.redis.domain.geo;

import org.clever.data.geo.Point;
import org.clever.util.ObjectUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 13:17 <br/>
 */
public class GeoLocation<T> {
    private final T name;
    private final Point point;

    public GeoLocation(T name, Point point) {
        this.name = name;
        this.point = point;
    }

    public T getName() {
        return this.name;
    }

    public Point getPoint() {
        return this.point;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeoLocation)) {
            return false;
        }
        GeoLocation<?> that = (GeoLocation<?>) o;
        if (!ObjectUtils.nullSafeEquals(name, that.name)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(point, that.point);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(name);
        result = 31 * result + ObjectUtils.nullSafeHashCode(point);
        return result;
    }

    public String toString() {
        return "GeoLocation(name=" + this.getName() + ", point=" + this.getPoint() + ")";
    }
}
