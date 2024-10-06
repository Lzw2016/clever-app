package org.clever.web.mvc.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 将请求{@code "multipart/form-data"}数据绑定到参数
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:44 <br/>
 *
 * @see RequestParam
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPart {
    /**
     * {@link #name} 的别名。
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 要绑定到的 {@code "multipart/form-data"} 的名称
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 是否是必须的
     */
    boolean required() default true;
}
