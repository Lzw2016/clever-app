package org.clever.data.jdbc.mybatis.annotations;

import java.lang.annotation.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/10 16:08 <br/>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
    /**
     * 参数名称
     */
    String value();
}
