package org.clever.util.comparator;

import java.util.Comparator;

/**
 * 使比较器适应比较器接口的比较器。
 * 主要用于其他比较器的内部使用，当应该在可比物上工作时。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/14 22:51 <br/>
 *
 * @param <T> 该比较器可以比较的可比对象的类型
 * @see Comparable
 */
public class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {
    /**
     * 此默认比较器的共享实例
     *
     * @see Comparators#comparable()
     */
    @SuppressWarnings("rawtypes")
    public static final ComparableComparator INSTANCE = new ComparableComparator();

    @Override
    public int compare(T o1, T o2) {
        return o1.compareTo(o2);
    }
}
