package org.clever.core.annotation;

import org.clever.core.DecoratingProxy;
import org.clever.core.OrderComparator;
import org.clever.core.annotation.MergedAnnotations.SearchStrategy;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

/**
 * AnnotationAwareOrderComparator是OrderComparator的扩展，
 * 支持{@link org.clever.core.Ordered}接口以及{@link Order @Order}和{@code  javax.annotation.Priority @Priority}注解，
 * 有序实例提供的顺序值覆盖静态定义的注解值（如果有）。
 * 有关非有序对象的排序语义的详细信息，请参阅Javadoc for {@link OrderComparator}。
 *
 * <p>有关非有序对象的排序语义的详细信息，请参阅Javadoc。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:31 <br/>
 *
 * @see org.clever.core.Ordered
 * @see org.clever.core.annotation.Order
 */
public class AnnotationAwareOrderComparator extends OrderComparator {
    /**
     * 的共享默认实例 {@code AnnotationAwareOrderComparator}.
     */
    public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();

    /**
     * 该实现检查除组织之外的各种元素的@Order或@Priority。聪明的果心超类中的有序签入。
     * 此实现检查 {@link Order @Order} 或 {@code javax.annotation.Priority @Priority} 关于各种元素，除了 {@link org.clever.core.Ordered} 超类。
     */
    @Override
    protected Double findOrder(Object obj) {
        Double order = super.findOrder(obj);
        if (order != null) {
            return order;
        }
        return findOrderFromAnnotation(obj);
    }

    private Double findOrderFromAnnotation(Object obj) {
        AnnotatedElement element = (obj instanceof AnnotatedElement ? (AnnotatedElement) obj : obj.getClass());
        MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
        Double order = OrderUtils.getOrderFromAnnotations(element, annotations);
        if (order == null && obj instanceof DecoratingProxy) {
            return findOrderFromAnnotation(((DecoratingProxy) obj).getDecoratedClass());
        }
        return order;
    }

    /**
     * 此实现检索 @{@code javax.annotation.Priority} 值,
     * 允许在常规 @{@link Order} 注解：通常，如果存在多个匹配项，但只返回一个对象，则选择一个对象。
     */
    @Override
    public Integer getPriority(Object obj) {
        if (obj instanceof Class) {
            return OrderUtils.getPriority((Class<?>) obj);
        }
        Integer priority = OrderUtils.getPriority(obj.getClass());
        if (priority == null && obj instanceof DecoratingProxy) {
            return getPriority(((DecoratingProxy) obj).getDecoratedClass());
        }
        return priority;
    }

    /**
     * 使用默认值对给定列表排序 {@link AnnotationAwareOrderComparator}.
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
     * 使用默认的AnnotationAwareOrderComparator对给定数组排序。
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
     * 如有必要，使用默认的AnnotationAwareOrderComparator对给定的数组或列表进行排序。当给定任何其他值时，只需跳过排序。
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
}
