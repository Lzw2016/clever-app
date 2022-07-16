package org.clever.core;

import org.clever.util.ObjectUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 比较器({@link Comparator})实现有序({@link Ordered})对象，按顺序值升序排序，分别按优先级降序排序。
 *
 * <h3>{@code PriorityOrdered} 对象</h3>
 * <p>{@link PriorityOrdered} 对象的排序优先级将高于普通排序对象。相同顺序对象
 *
 * <h3>相同顺序对象</h3>
 * <p>具有相同顺序值的对象将按照相对于具有相同顺序值的其他对象的任意顺序进行排序。
 *
 * <h3>非有序对象</h3>
 * <p>任何不提供自己的order值的对象都会隐式地指定一个{@link Ordered#LOWEST_PRECEDENCE}，
 * 因此相对于具有相同顺序值的其他对象，以任意顺序结束于已排序集合的末尾。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:37 <br/>
 *
 * @see Ordered
 * @see PriorityOrdered
 * @see java.util.List#sort(java.util.Comparator)
 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
 */
public class OrderComparator implements Comparator<Object> {
    /**
     * 的共享默认实例 {@code OrderComparator}.
     */
    public static final OrderComparator INSTANCE = new OrderComparator();

    /**
     * 使用给定的源提供者构建一个调整的顺序比较器。
     *
     * @param sourceProvider 要使用的订单源提供程序
     * @return 自适应比较器
     */
    public Comparator<Object> withSourceProvider(OrderSourceProvider sourceProvider) {
        return (o1, o2) -> doCompare(o1, o2, sourceProvider);
    }

    @Override
    public int compare(Object o1, Object o2) {
        return doCompare(o1, o2, null);
    }

    private int doCompare(Object o1, Object o2, OrderSourceProvider sourceProvider) {
        boolean p1 = (o1 instanceof PriorityOrdered);
        boolean p2 = (o2 instanceof PriorityOrdered);
        if (p1 && !p2) {
            return -1;
        } else if (p2 && !p1) {
            return 1;
        }
        int i1 = getOrder(o1, sourceProvider);
        int i2 = getOrder(o2, sourceProvider);
        return Integer.compare(i1, i2);
    }

    /**
     * 确定给定对象的顺序值。
     * <p>默认实现根据给定的OrderComparator进行检查。
     * {@link OrderSourceProvider}使用{@link #findOrder}并返回到常规的{@link #getOrder(Object)}调用。
     *
     * @param obj 要检查的对象
     * @return order值，或{@code Ordered.LOWEST_PRECEDENCE}作为回退
     */
    private int getOrder(Object obj, OrderSourceProvider sourceProvider) {
        Integer order = null;
        if (obj != null && sourceProvider != null) {
            Object orderSource = sourceProvider.getOrderSource(obj);
            if (orderSource != null) {
                if (orderSource.getClass().isArray()) {
                    for (Object source : ObjectUtils.toObjectArray(orderSource)) {
                        order = findOrder(source);
                        if (order != null) {
                            break;
                        }
                    }
                } else {
                    order = findOrder(orderSource);
                }
            }
        }
        return (order != null ? order : getOrder(obj));
    }

    /**
     * 确定给定对象的顺序值。
     * <p>默认实现通过委托给{@link #findOrder}来检查有序接口。可以在子类中重写。
     *
     * @param obj 要检查的对象
     * @return order值，或{@code Ordered.LOWEST_PRECEDENCE}作为回退
     */
    protected int getOrder(Object obj) {
        if (obj != null) {
            Integer order = findOrder(obj);
            if (order != null) {
                return order;
            }
        }
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 查找给定对象指示的顺序值。
     * <p>默认实现根据有序接口进行检查。可以在子类中重写。
     *
     * @param obj 要检查的对象
     * @return 订单值，如果未找到，则为null
     */
    protected Integer findOrder(Object obj) {
        return (obj instanceof Ordered ? ((Ordered) obj).getOrder() : null);
    }

    /**
     * 确定给定对象的优先级值（如果有）。
     * <p>默认实现始终返回null。
     * 除了“order”语义之外，子类还可以覆盖该属性，为特定类型的值赋予“priority”特征。
     * 优先级表示它除了用于在list/array中排序外，还可以用于选择一个对象而不是另一个对象。
     *
     * @param obj 要检查的对象
     * @return 优先级值，如果没有，则为null
     */
    public Integer getPriority(Object obj) {
        return null;
    }

    /**
     * 使用默认OrderComparator对给定列表进行排序。
     * <p>优化为跳过大小为0或1的列表的排序，以避免不必要的数组提取。
     *
     * @param list 要排序的列表
     * @see java.util.List#sort(java.util.Comparator)
     */
    public static void sort(List<?> list) {
        if (list.size() > 1) {
            list.sort(INSTANCE);
        }
    }

    /**
     * 使用默认OrderComparator对给定数组排序。
     * <p>优化为跳过大小为0或1的列表的排序，以避免不必要的数组提取。
     *
     * @param array 要排序的数组
     * @see java.util.Arrays#sort(Object[], java.util.Comparator)
     */
    public static void sort(Object[] array) {
        if (array.length > 1) {
            Arrays.sort(array, INSTANCE);
        }
    }

    /**
     * 如有必要，使用默认的OrderComparator对给定的数组或列表进行排序。当给定任何其他值时，只需跳过排序。
     * <p>优化为跳过大小为0或1的列表的排序，以避免不必要的数组提取。
     *
     * @param value 要排序的数组或列表
     * @see java.util.Arrays#sort(Object[], java.util.Comparator)
     */
    public static void sortIfNecessary(Object value) {
        if (value instanceof Object[]) {
            sort((Object[]) value);
        } else if (value instanceof List) {
            sort((List<?>) value);
        }
    }

    /**
     * 策略接口，为给定对象提供订单源。
     */
    @FunctionalInterface
    public interface OrderSourceProvider {
        /**
         * 返回指定对象的订单源，即应检查订单值以替换给定对象的对象。
         * <p>也可以是订单源对象的数组。
         * <p>如果返回的对象没有指示任何顺序，则比较器将返回到检查原始对象。
         *
         * @param obj 要为其查找订单源的对象
         * @return 该对象的订单源，如果未找到，则为null
         */
        Object getOrderSource(Object obj);
    }
}
