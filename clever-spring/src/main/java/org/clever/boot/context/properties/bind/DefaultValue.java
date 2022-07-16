package org.clever.boot.context.properties.bind;

import java.lang.annotation.*;

/**
 * 可用于在绑定到不可变属性时指定默认值的注释。
 * 此注释还可以与嵌套属性一起使用，以指示应始终绑定值（而不是绑定null）。
 * 只有在 {@link Binder} 使用的特性源中未找到特性时，才会使用此注释中的值。
 * 例如，如果在绑定到@ConfigurationProperties时 {@link org.clever.core.env.Environment} 中存在该属性，
 * 则即使该属性值为空，也不会使用该属性的默认值。
 * <p>
 * 注意：此注释不支持属性占位符解析，值必须为常量。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:13 <br/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Documented
public @interface DefaultValue {
    /**
     * 属性的默认值。可以是集合或基于数组的属性的值数组。
     *
     * @return 属性的默认值。
     */
    String[] value() default {};
}
