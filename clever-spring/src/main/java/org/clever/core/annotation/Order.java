package org.clever.core.annotation;

import org.clever.core.Ordered;

import java.lang.annotation.*;

/**
 * {@code @Order} 定义顺序。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:33 <br/>
 *
 * @see org.clever.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @see OrderUtils
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {
    /**
     * order 值
     * <p>默认值为 {@link Ordered#LOWEST_PRECEDENCE}.
     *
     * @see Ordered#getOrder()
     */
    int value() default Ordered.LOWEST_PRECEDENCE;
}
