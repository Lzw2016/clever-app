package org.clever.core.validator.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 状态校验 String 类型(不校验null数据)
 * 作者： lzw<br/>
 * 创建时间：2018-11-09 21:40 <br/>
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = StrStatusValidator.class)
public @interface StrStatus {
    String message() default "状态校验失败";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 有效的状态值集合
     */
    String[] value() default {};
}
