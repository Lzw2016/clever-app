package org.clever.format.annotation;

import java.lang.annotation.*;

/**
 * 声明字段或方法参数的格式应为日期或时间
 * <p>
 * 支持按样式模式、ISO日期时间模式或自定义格式模式字符串进行格式设置。
 * 可以应用于{@link java.util.Date}，{@link java.util.Calendar}，{@link Long}(毫秒时间戳)以及JSR-310 {@code java.time}类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:02 <br/>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DateTimeFormat {
    /**
     * 对于短日期、短时间，默认为"SS"。
     * 如果希望按照默认样式以外的通用样式格式化字段或方法参数，请设置此属性
     *
     * @see #fallbackPatterns
     */
    String style() default "SS";

    /**
     * 用于设置字段或方法参数格式的ISO模式。
     * 支持的ISO模式在DateTimeFormat中定义{@link ISO}枚举。
     * 默认为{@link ISO#NONE}，表示应忽略此属性。
     * 如果希望按照ISO格式格式化字段或方法参数，请设置此属性
     *
     * @see #fallbackPatterns
     */
    ISO iso() default ISO.NONE;

    /**
     * 设置时间格式
     *
     * @see #fallbackPatterns
     */
    String pattern() default "";

    /**
     * 当patterns、iso、style失败时，用作回退的自定义时间格式
     * <pre>{@code
     * @DateTimeFormat(iso = ISO.DATE, fallbackPatterns = { "M/d/yy", "dd.MM.yyyy" })
     * }</pre>
     */
    String[] fallbackPatterns() default {};

    /**
     * 通用ISO日期时间格式模式
     */
    enum ISO {

        /**
         * 最常见的ISO日期格式 {@code yyyy-MM-dd}: "2000-10-31"
         */
        DATE,

        /**
         * 最常见的ISO日期格式 {@code HH:mm:ss.SSSXXX}: "01:30:00.000-05:00".
         */
        TIME,

        /**
         * 最常见的ISO日期时间格式 {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX}: "2000-10-31T01:30:00.000-05:00"
         */
        DATE_TIME,

        /**
         * 指示不应应用基于ISO的格式模式
         */
        NONE
    }
}
