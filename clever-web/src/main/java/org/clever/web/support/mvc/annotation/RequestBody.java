package org.clever.web.support.mvc.annotation;

import java.lang.annotation.*;

/**
 * 将请求body数据绑定到参数
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:44 <br/>
 *
 * @see RequestHeader
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
    /**
     * 是否是必须的
     */
    boolean required() default true;
}
