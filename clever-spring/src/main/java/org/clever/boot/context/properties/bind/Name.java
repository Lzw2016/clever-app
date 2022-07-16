package org.clever.boot.context.properties.bind;

import java.lang.annotation.*;

/**
 * 绑定到不可变属性时可用于指定名称的注释。当绑定到与保留语言关键字冲突的名称时，可能需要此注释。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:11 <br/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Documented
public @interface Name {
    /**
     * 用于绑定的属性的名称。
     *
     * @return 属性名称
     */
    String value();
}
