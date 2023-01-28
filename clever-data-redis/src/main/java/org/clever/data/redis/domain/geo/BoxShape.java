package org.clever.data.redis.domain.geo;

import org.clever.data.geo.Metric;
import org.clever.util.Assert;

/**
 * 由宽度和高度定义的边界框
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:38 <br/>
 */
public class BoxShape implements GeoShape {
    private final BoundingBox boundingBox;

    public BoxShape(BoundingBox boundingBox) {
        Assert.notNull(boundingBox, "BoundingBox must not be null");
        this.boundingBox = boundingBox;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @Override
    public Metric getMetric() {
        return boundingBox.getHeight().getMetric();
    }
}
