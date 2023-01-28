package org.clever.data.redis.domain.geo;

import org.clever.data.geo.Distance;
import org.clever.data.geo.Metric;
import org.clever.data.geo.Shape;
import org.clever.data.redis.connection.RedisGeoCommands.DistanceUnit;

/**
 * {@code GEOSEARCH} 和 {@code GEOSEARCHSTORE} 命令的搜索谓词
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:33 <br/>
 */
public interface GeoShape extends Shape {
    /**
     * 从查询中心点周围的 {@link Distance radius} 创建一个用作地理查询谓词的形状
     */
    static GeoShape byRadius(Distance radius) {
        return new RadiusShape(radius);
    }

    /**
     * 从边界框创建一个形状，用作地理查询的谓词，边界框由 {@code width} 和 {@code height} 指定
     *
     * @param width        不得为 {@literal null}
     * @param height       不得为 {@literal null}
     * @param distanceUnit 不得为 {@literal null}
     */
    static GeoShape byBox(double width, double height, DistanceUnit distanceUnit) {
        return byBox(new BoundingBox(width, height, distanceUnit));
    }

    /**
     * 创建一个形状，用作来自 {@link BoundingBox} 的地理查询的谓词
     *
     * @param boundingBox 不得为 {@literal null}
     */
    static GeoShape byBox(BoundingBox boundingBox) {
        return new BoxShape(boundingBox);
    }

    /**
     * 用于此地理谓词的指标
     */
    Metric getMetric();
}
