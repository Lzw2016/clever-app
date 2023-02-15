package org.clever.util.comparator;

import org.clever.util.Assert;

import java.util.Comparator;

/**
 * 一个比较器，它将安全地比较空值低于或高于其他对象。
 * 可以装饰给定的比较器或处理可比物。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/14 22:50 <br/>
 *
 * @param <T> 该比较器可以比较的对象类型
 * @see Comparable
 */
public class NullSafeComparator<T> implements Comparator<T> {
    /**
     * 此比较器的共享默认实例，处理的空值低于非空对象
     *
     * @see Comparators#nullsLow()
     */
    @SuppressWarnings("rawtypes")
    public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

    /**
     * 此比较器的共享默认实例，将空值处理高于非空对象
     *
     * @see Comparators#nullsHigh()
     */
    @SuppressWarnings("rawtypes")
    public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<>(false);

    private final Comparator<T> nonNullComparator;
    private final boolean nullsLow;

    /**
     * 创建一个 NullSafeComparator，该比较器根据提供的标志对 {@code null} 进行排序，处理可比对象。
     * <p>当比较两个非空对象时，将使用它们的可比较实现：这意味着非空元素（此比较器将应用于）需要实现可比较。
     * <p>为方便起见，您可以使用默认共享实例：{@code NullSafeComparator.NULLS_LOW} 和 {@code NullSafeComparator.NULLS_HIGH}
     *
     * @param nullsLow 是将空值处理得低于还是高于非空对象
     * @see Comparable
     * @see #NULLS_LOW
     * @see #NULLS_HIGH
     */
    @SuppressWarnings("unchecked")
    private NullSafeComparator(boolean nullsLow) {
        this.nonNullComparator = ComparableComparator.INSTANCE;
        this.nullsLow = nullsLow;
    }

    /**
     * 创建一个 NullSafeComparator，该比较器根据提供的标志对 {@code null} 进行排序，修饰给定的比较器
     * <p>比较两个非空对象时，将使用指定的比较器。
     * 给定的基础比较器必须能够处理将应用此比较器的元素。
     *
     * @param comparator 比较两个非 NULL 对象时要使用的比较器
     * @param nullsLow   是将空值处理得低于还是高于非空对象
     */
    public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
        Assert.notNull(comparator, "Non-null Comparator is required");
        this.nonNullComparator = comparator;
        this.nullsLow = nullsLow;
    }

    @Override
    public int compare(T o1, T o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return (this.nullsLow ? -1 : 1);
        }
        if (o2 == null) {
            return (this.nullsLow ? 1 : -1);
        }
        return this.nonNullComparator.compare(o1, o2);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NullSafeComparator)) {
            return false;
        }
        NullSafeComparator<T> otherComp = (NullSafeComparator<T>) other;
        return (this.nonNullComparator.equals(otherComp.nonNullComparator) && this.nullsLow == otherComp.nullsLow);
    }

    @Override
    public int hashCode() {
        return this.nonNullComparator.hashCode() * (this.nullsLow ? -1 : 1);
    }

    @Override
    public String toString() {
        return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " + (this.nullsLow ? "nulls low" : "nulls high");
    }
}
