package org.clever.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解，只能在请求入口方法上使用<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 11:49 <br/>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {
    /**
     * 必须拥有指定全部角色
     */
    String[] roles() default {};

    /**
     * 必须拥有指定全部权限
     */
    String[] permissions() default {};

    /**
     * 必须拥有指定任意角色
     */
    String[] anyRoles() default {};

    /**
     * 必须拥有指定任意权限
     */
    String[] anyPermissions() default {};

    /**
     * 必须满足指定表达式(OGNL表达式语法)
     */
    String expr() default "";
}
