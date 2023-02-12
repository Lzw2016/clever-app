package org.clever.data.redis.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link SortParameters} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:16 <br/>
 */
public class DefaultSortParameters implements SortParameters {
    private byte[] byPattern;
    private Range limit;
    private final List<byte[]> getPattern = new ArrayList<>(4);
    private Order order;
    private Boolean alphabetic;

    /**
     * 构造一个新的 <code>DefaultSortParameters</code> 实例
     */
    public DefaultSortParameters() {
        this(null, null, null, null, null);
    }

    /**
     * 构造一个新的 <code>DefaultSortParameters</code> 实例
     */
    public DefaultSortParameters(Range limit, Order order, Boolean alphabetic) {
        this(null, limit, null, order, alphabetic);
    }

    /**
     * 构造一个新的 <code>DefaultSortParameters</code> 实例
     */
    public DefaultSortParameters(byte[] byPattern, Range limit, byte[][] getPattern, Order order, Boolean alphabetic) {
        super();
        this.byPattern = byPattern;
        this.limit = limit;
        this.order = order;
        this.alphabetic = alphabetic;
        setGetPattern(getPattern);
    }

    public byte[] getByPattern() {
        return byPattern;
    }

    public void setByPattern(byte[] byPattern) {
        this.byPattern = byPattern;
    }

    public Range getLimit() {
        return limit;
    }

    public void setLimit(Range limit) {
        this.limit = limit;
    }

    public byte[][] getGetPattern() {
        return getPattern.toArray(new byte[getPattern.size()][]);
    }

    public void addGetPattern(byte[] gPattern) {
        getPattern.add(gPattern);
    }

    public void setGetPattern(byte[][] gPattern) {
        getPattern.clear();
        if (gPattern == null) {
            return;
        }
        Collections.addAll(getPattern, gPattern);
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Boolean isAlphabetic() {
        return alphabetic;
    }

    public void setAlphabetic(Boolean alphabetic) {
        this.alphabetic = alphabetic;
    }

    //
    // builder like methods
    //

    public DefaultSortParameters order(Order order) {
        setOrder(order);
        return this;
    }

    public DefaultSortParameters alpha() {
        setAlphabetic(true);
        return this;
    }

    public DefaultSortParameters asc() {
        setOrder(Order.ASC);
        return this;
    }

    public DefaultSortParameters desc() {
        setOrder(Order.DESC);
        return this;
    }

    public DefaultSortParameters numeric() {
        setAlphabetic(false);
        return this;
    }

    public DefaultSortParameters get(byte[] pattern) {
        addGetPattern(pattern);
        return this;
    }

    public DefaultSortParameters by(byte[] pattern) {
        setByPattern(pattern);
        return this;
    }

    public DefaultSortParameters limit(long start, long count) {
        setLimit(new Range(start, count));
        return this;
    }
}
