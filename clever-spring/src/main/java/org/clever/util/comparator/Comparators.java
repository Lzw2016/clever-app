package org.clever.util.comparator;

import java.util.Comparator;

/**
 * 方便的入口点，具有通用类型工厂方法，适用于常见的 Spring {@link Comparator} 变体
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/15 20:42 <br/>
 */
public abstract class Comparators {
    /**
     * 返回 {@link Comparable} 适配器
     *
     * @see ComparableComparator#INSTANCE
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> comparable() {
        return ComparableComparator.INSTANCE;
    }

    /**
     * 返回一个 {@link Comparable} 适配器，该适配器接受空值并将其排序为低于非空值
     *
     * @see NullSafeComparator#NULLS_LOW
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullsLow() {
        return NullSafeComparator.NULLS_LOW;
    }

    /**
     * 返回给定比较器的装饰器，该比较器接受空值并将它们排序为低于非空值
     *
     * @see NullSafeComparator#NullSafeComparator(boolean)
     */
    public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
        return new NullSafeComparator<>(comparator, true);
    }

    /**
     * 返回一个 {@link Comparable} 适配器，该适配器接受空值并将它们排序为高于非空值
     *
     * @see NullSafeComparator#NULLS_HIGH
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullsHigh() {
        return NullSafeComparator.NULLS_HIGH;
    }

    /**
     * 返回给定比较器的装饰器，该比较器接受空值并将它们排序为高于非空值
     *
     * @see NullSafeComparator#NullSafeComparator(boolean)
     */
    public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
        return new NullSafeComparator<>(comparator, false);
    }
}
