package org.clever.data.geo;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 用于捕获 {@link GeoResult} 及其平均距离的值对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:24 <br/>
 */
public class GeoResults<T> implements Iterable<GeoResult<T>>, Serializable {
    private static final long serialVersionUID = 8347363491300219485L;

    private final List<? extends GeoResult<T>> results;
    private final Distance averageDistance;

    /**
     * 创建一个新的 {@link GeoResults} 实例，手动计算给定 {@link GeoResult} 的距离值的平均距离
     *
     * @param results 不得为 {@literal null}
     */
    public GeoResults(List<? extends GeoResult<T>> results) {
        this(results, Metrics.NEUTRAL);
    }

    /**
     * 创建一个新的 {@link GeoResults} 实例，根据给定 {@link GeoResult} 的距离值手动计算给定 {@link Metric} 中的平均距离
     *
     * @param results 不得为 {@literal null}
     * @param metric  不得为 {@literal null}
     */
    public GeoResults(List<? extends GeoResult<T>> results, Metric metric) {
        this(results, calculateAverageDistance(results, metric));
    }

    /**
     * 根据给定的 {@link GeoResult} 和平均距离创建一个新的 {@link GeoResults} 实例
     *
     * @param results         不得为 {@literal null}
     * @param averageDistance 不得为 {@literal null}
     */
    public GeoResults(List<? extends GeoResult<T>> results, Distance averageDistance) {
        Assert.notNull(results, "Results must not be null");
        Assert.notNull(averageDistance, "Average Distance must not be null");
        this.results = results;
        this.averageDistance = averageDistance;
    }

    /**
     * 返回此列表中所有 {@link GeoResult} 的平均距离
     *
     * @return 平均距离
     */
    public Distance getAverageDistance() {
        return averageDistance;
    }

    /**
     * 返回 {@link GeoResults} 的实际内容
     */
    public List<GeoResult<T>> getContent() {
        return Collections.unmodifiableList(results);
    }

    @SuppressWarnings("unchecked")
    public Iterator<GeoResult<T>> iterator() {
        return (Iterator<GeoResult<T>>) results.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeoResults)) {
            return false;
        }
        GeoResults<?> that = (GeoResults<?>) o;
        if (!ObjectUtils.nullSafeEquals(results, that.results)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(averageDistance, that.averageDistance);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(results);
        result = 31 * result + ObjectUtils.nullSafeHashCode(averageDistance);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "GeoResults: [averageDistance: %s, results: %s]",
                averageDistance.toString(),
                StringUtils.collectionToCommaDelimitedString(results)
        );
    }

    private static Distance calculateAverageDistance(List<? extends GeoResult<?>> results, Metric metric) {
        Assert.notNull(results, "Results must not be null");
        Assert.notNull(metric, "Metric must not be null");
        if (results.isEmpty()) {
            return new Distance(0, metric);
        }
        double averageDistance = results.stream().mapToDouble(it -> it.getDistance().getValue()).average().orElse(0);
        return new Distance(averageDistance, metric);
    }
}
