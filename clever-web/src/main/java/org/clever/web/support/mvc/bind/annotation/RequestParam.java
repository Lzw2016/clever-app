package org.clever.web.support.mvc.bind.annotation;

import org.clever.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 将请求parameter数据绑定到参数
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:36 <br/>
 *
 * @see RequestHeader
 * @see CookieValue
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    /**
     * {@link #name} 的别名。
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 要绑定到的 parameter 的名称
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 是否是必须的
     */
    boolean required() default true;

    /**
     * 默认值
     */
    String defaultValue() default ValueConstants.DEFAULT_NONE;
}
