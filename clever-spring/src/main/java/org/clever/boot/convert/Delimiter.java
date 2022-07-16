package org.clever.boot.convert;

import java.lang.annotation.*;

/**
 * 声明应使用指定的分隔符将字段或方法参数转换为集合。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:00 <br/>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface Delimiter {
    /**
     * 一个分隔符值，用于指示不需要分隔符，结果应为包含整个字符串的单个元素。
     */
    String NONE = "";

    /**
     * 如果整个内容应视为单个元素，则使用或{@code NONE}的分隔符。
     *
     * @return 分隔符
     */
    String value();
}
