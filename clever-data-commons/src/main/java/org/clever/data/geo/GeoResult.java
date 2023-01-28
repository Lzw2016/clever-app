package org.clever.data.geo;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.Serializable;

/**
 * 值对象捕获一些任意对象加上距离
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:25 <br/>
 */
public final class GeoResult<T> implements Serializable {
    private static final long serialVersionUID = 1637452570977581370L;

    private final T content;
    private final Distance distance;

    public GeoResult(T content, Distance distance) {
        Assert.notNull(content, "Content must not be null");
        Assert.notNull(distance, "Distance must not be null");
        this.content = content;
        this.distance = distance;
    }

    public T getContent() {
        return this.content;
    }

    public Distance getDistance() {
        return this.distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeoResult)) {
            return false;
        }
        GeoResult<?> geoResult = (GeoResult<?>) o;
        if (!ObjectUtils.nullSafeEquals(content, geoResult.content)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(distance, geoResult.distance);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(content);
        result = 31 * result + ObjectUtils.nullSafeHashCode(distance);
        return result;
    }

    @Override
    public String toString() {
        return String.format("GeoResult [content: %s, distance: %s, ]", content.toString(), distance.toString());
    }
}
