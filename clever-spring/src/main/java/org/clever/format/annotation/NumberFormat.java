package org.clever.format.annotation;

import java.lang.annotation.*;

/**
 * 声明字段或方法参数的格式应为数字<br/>
 * 可以应用于任何JDK {@code Number}类型，如{@code Double}、{@code Long}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:13 <br/>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface NumberFormat {
    /**
     * 设置模式。默认为{@link Style#DEFAULT}
     */
    Style style() default Style.DEFAULT;

    /**
     * 自定义格式
     */
    String pattern() default "";

    /**
     * 常用数字格式样式
     */
    enum Style {
        /**
         * 默认格式：对于货币类型(例如{@code javax.money.MonetaryAmount)})，通常为“number”，但也可能为“currency”
         */
        DEFAULT,
        /**
         * 当前区域设置的通用数字格式
         */
        NUMBER,
        /**
         * 当前区域设置的百分比格式
         */
        PERCENT,
        /**
         * 当前区域设置的货币格式
         */
        CURRENCY
    }
}
