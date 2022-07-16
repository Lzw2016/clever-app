package org.clever.core.annotation;

import java.lang.annotation.*;

/**
 * AliasFor 注解用于定义注解的属性别名
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:41 <br/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFor {
    /**
     * 属性的别名，与{@link #attribute()}相同
     */
    @AliasFor("attribute") String value() default "";

    /**
     * 属性的别名，与{@link #value()}相同
     */
    @AliasFor("value") String attribute() default "";

    /**
     * 声明别名属性的Annotation
     */
    Class<? extends Annotation> annotation() default Annotation.class;
}
