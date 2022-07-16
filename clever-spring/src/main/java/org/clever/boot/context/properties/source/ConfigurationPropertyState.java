package org.clever.boot.context.properties.source;

import org.clever.util.Assert;

import java.util.function.Predicate;

/**
 * 来自 {@link ConfigurationPropertySource}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:43 <br/>
 */
public enum ConfigurationPropertyState {
    /**
     * {@link ConfigurationPropertySource} 至少有一个匹配项 {@link ConfigurationProperty}.
     */
    PRESENT,
    /**
     * {@link ConfigurationPropertySource} 没有匹配项 {@link ConfigurationProperty ConfigurationProperties}.
     */
    ABSENT,
    /**
     * 不可能确定 {@link ConfigurationPropertySource} 具有匹配项 {@link ConfigurationProperty ConfigurationProperties} 或者没有
     */
    UNKNOWN;

    /**
     * 使用谓词搜索给定的iterable，以确定内容是否为 {@link #PRESENT} 或 {@link #ABSENT}.
     *
     * @param <T>       数据类型
     * @param source    源无法搜索
     * @param predicate 用于测试是否存在的谓词
     * @return 如果iterable包含匹配项，则为 {@link #PRESENT}，否则为 {@link #ABSENT}
     */
    static <T> ConfigurationPropertyState search(Iterable<T> source, Predicate<T> predicate) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(predicate, "Predicate must not be null");
        for (T item : source) {
            if (predicate.test(item)) {
                return PRESENT;
            }
        }
        return ABSENT;
    }
}
