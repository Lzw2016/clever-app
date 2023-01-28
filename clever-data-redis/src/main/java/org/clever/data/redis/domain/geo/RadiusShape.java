package org.clever.data.redis.domain.geo;

import org.clever.data.geo.Distance;
import org.clever.data.geo.Metric;
import org.clever.util.Assert;

/**
 * 由 {@link Distance} 定义的半径
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:34 <br/>
 */
public class RadiusShape implements GeoShape {
    private final Distance radius;

    public RadiusShape(Distance radius) {
        Assert.notNull(radius, "Distance must not be null");
        this.radius = radius;
    }

    public Distance getRadius() {
        return radius;
    }

    @Override
    public Metric getMetric() {
        return radius.getMetric();
    }
}
