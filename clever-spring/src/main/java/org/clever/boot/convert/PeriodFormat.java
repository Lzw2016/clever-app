package org.clever.boot.convert;

import java.lang.annotation.*;
import java.time.Period;

/**
 * 可用于指示转换{@link Period}时使用的格式的注释。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:49 <br/>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PeriodFormat {
    /**
     * {@link Period} 格式样式
     */
    PeriodStyle value();
}
