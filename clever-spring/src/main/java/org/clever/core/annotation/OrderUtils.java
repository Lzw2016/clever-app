package org.clever.core.annotation;

import org.clever.core.annotation.MergedAnnotations.SearchStrategy;
import org.clever.util.ConcurrentReferenceHashMap;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

/**
 * 基于类型声明确定对象顺序的通用工具。
 * 处理{@link Order}注释以及{@code javax.annotation.Priority}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:32 <br/>
 *
 * @see Order
 */
public abstract class OrderUtils {
    /**
     * 非注释类的缓存标记。
     */
    private static final Object NOT_ANNOTATED = new Object();
    private static final String JAVAX_PRIORITY_ANNOTATION = "javax.annotation.Priority";
    /**
     * 每个类的 @Order 值（或未注释标记）缓存。
     */
    private static final Map<AnnotatedElement, Object> orderCache = new ConcurrentReferenceHashMap<>(64);

    /**
     * 返回指定类型的顺序，如果找不到，则返回指定的默认值。
     * <p>照顾 {@link Order @Order} 和 {@code @javax.annotation.Priority}.
     *
     * @param type 要处理的类型
     * @return 优先级值，或指定的默认顺序（如果找不到）
     * @see #getPriority(Class)
     */
    public static double getOrder(Class<?> type, int defaultOrder) {
        Double order = getOrder(type);
        return (order != null ? order : defaultOrder);
    }

    /**
     * 返回指定类型的顺序，如果找不到，则返回指定的默认值。
     * <p>照顾 {@link Order @Order} 和 {@code @javax.annotation.Priority}.
     *
     * @param type 要处理的类型
     * @return 优先级值，或指定的默认顺序（如果找不到）
     * @see #getPriority(Class)
     */
    public static Double getOrder(Class<?> type, Integer defaultOrder) {
        Double order = getOrder(type);
        return (order != null ? order : defaultOrder);
    }

    /**
     * 返回指定类型的order。
     * <p>照顾 {@link Order @Order} 和 {@code @javax.annotation.Priority}.
     *
     * @param type 要处理的类型
     * @return order值，如果找不到，则为null
     * @see #getPriority(Class)
     */
    public static Double getOrder(Class<?> type) {
        return getOrder((AnnotatedElement) type);
    }

    /**
     * 返回在指定元素上声明的顺序。
     * <p>照顾 {@link Order @Order} 和 {@code @javax.annotation.Priority}.
     *
     * @param element 注释元素（例如类型或方法）
     * @return order值，如果找不到，则为null
     */
    public static Double getOrder(AnnotatedElement element) {
        return getOrderFromAnnotations(element, MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY));
    }

    /**
     * 返回指定注释集合中的顺序。
     * <p>照顾 {@link Order @Order} 和 {@code @javax.annotation.Priority}.
     *
     * @param element     源元素
     * @param annotations 要考虑的注释
     * @return order值，如果找不到，则为null
     */
    static Double getOrderFromAnnotations(AnnotatedElement element, MergedAnnotations annotations) {
        if (!(element instanceof Class)) {
            return findOrder(annotations);
        }
        Object cached = orderCache.get(element);
        if (cached != null) {
            return (cached instanceof Double ? (Double) cached : null);
        }
        Double result = findOrder(annotations);
        orderCache.put(element, result != null ? result : NOT_ANNOTATED);
        return result;
    }

    private static Double findOrder(MergedAnnotations annotations) {
        MergedAnnotation<Order> orderAnnotation = annotations.get(Order.class);
        if (orderAnnotation.isPresent()) {
            return orderAnnotation.getDouble(MergedAnnotation.VALUE);
        }
        MergedAnnotation<?> priorityAnnotation = annotations.get(JAVAX_PRIORITY_ANNOTATION);
        if (priorityAnnotation.isPresent()) {
            return priorityAnnotation.getDouble(MergedAnnotation.VALUE);
        }
        return null;
    }

    /**
     * 返回在指定类型上声明的{@code javax.annotation.Priority}注释的值，如果没有，则返回null。
     *
     * @param type 要处理的类型
     * @return 如果声明了注释，则为优先级值；如果没有，则为null
     */
    public static Integer getPriority(Class<?> type) {
        return MergedAnnotations
                .from(type, SearchStrategy.TYPE_HIERARCHY)
                .get(JAVAX_PRIORITY_ANNOTATION)
                .getValue(MergedAnnotation.VALUE, Integer.class)
                .orElse(null);
    }
}
