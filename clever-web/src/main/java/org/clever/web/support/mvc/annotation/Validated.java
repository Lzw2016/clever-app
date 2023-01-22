package org.clever.web.support.mvc.annotation;

import java.lang.annotation.*;

/**
 * mvc参数验证，支持 JSR-303 验证规范
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/20 22:30 <br/>
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {
    /**
     * 指定一个或多个验证组以应用于由此注释启动的验证步骤。
     * <p>JSR-303 将验证组定义为自定义注释，应用程序声明这些注释的唯一目的是将它们用作类型安全的组参数
     */
    Class<?>[] value() default {};
}
