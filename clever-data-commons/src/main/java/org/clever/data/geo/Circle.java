package org.clever.data.geo;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 代表一个地理空间圈值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:20 <br/>
 */
public class Circle implements Shape {
    private static final long serialVersionUID = 5215611530535947924L;

    private final Point center;
    private final Distance radius;

    /**
     * 从给定的 {@link Point} 和半径创建一个新的 {@link Circle}
     *
     * @param center 不得为 {@literal null}
     * @param radius 不能是 {@literal null} 并且它的值大于或等于零
     */
    public Circle(Point center, Distance radius) {
        Assert.notNull(center, "Center point must not be null");
        Assert.notNull(radius, "Radius must not be null");
        Assert.isTrue(radius.getValue() >= 0, "Radius must not be negative");
        this.center = center;
        this.radius = radius;
    }

    /**
     * 从给定的 {@link Point} 和半径创建一个新的 {@link Circle}
     *
     * @param center 不得为 {@literal null}
     * @param radius radius的值必须大于或等于零
     */
    public Circle(Point center, double radius) {
        this(center, new Distance(radius));
    }

    /**
     * 从给定的坐标和半径创建一个新的 {@link Circle} 作为 {@link Distance} 和 {@link Metrics#NEUTRAL}
     *
     * @param centerX 不得为 {@literal null}
     * @param centerY 不得为 {@literal null}
     * @param radius  必须大于或等于零
     */
    public Circle(double centerX, double centerY, double radius) {
        this(new Point(centerX, centerY), new Distance(radius));
    }

    /**
     * 返回 {@link Circle} 的中心
     *
     * @return 永远不会是 {@literal null}
     */
    public Point getCenter() {
        return center;
    }

    /**
     * 返回 {@link Circle} 的半径
     */
    public Distance getRadius() {
        return radius;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Circle)) {
            return false;
        }
        Circle circle = (Circle) o;
        if (!ObjectUtils.nullSafeEquals(center, circle.center)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(radius, circle.radius);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(center);
        result = 31 * result + ObjectUtils.nullSafeHashCode(radius);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Circle: [center=%s, radius=%s]", center, radius);
    }
}
