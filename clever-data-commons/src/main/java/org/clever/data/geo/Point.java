package org.clever.data.geo;

import org.clever.util.Assert;

import java.io.Serializable;
import java.util.Locale;

/**
 * 表示地理空间点值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/26 23:38 <br/>
 */
public class Point implements Serializable {
    private static final long serialVersionUID = 3583151228933783558L;

    private final double x;
    private final double y;

    /**
     * 从给定的 {@code x}，{@code y} 坐标创建一个 {@link Point}
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 从给定的 {@link Point} 坐标创建一个 {@link Point}
     *
     * @param point 不得为 {@literal null}
     */
    public Point(Point point) {
        Assert.notNull(point, "Source point must not be null");
        this.x = point.x;
        this.y = point.y;
    }

    /**
     * 返回 {@link Point} 的 x 坐标
     */
    public double getX() {
        return x;
    }

    /**
     * 返回 {@link Point} 的 y 坐标
     */
    public double getY() {
        return y;
    }

    @Override
    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Point)) {
            return false;
        }
        Point other = (Point) obj;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        // noinspection RedundantIfStatement
        if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Point [x=%f, y=%f]", x, y);
    }
}
