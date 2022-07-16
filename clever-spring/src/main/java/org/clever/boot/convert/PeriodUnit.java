package org.clever.boot.convert;

import java.lang.annotation.*;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * 可用于更改转换{@link Period}时使用的默认单位的注释。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:48 <br/>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PeriodUnit {
    /**
     * 如果未指定，则使用Period单位。
     *
     * @return 周期单位
     */
    ChronoUnit value();
}
