package org.clever.boot.convert;

import java.lang.annotation.*;
import java.time.Duration;

/**
 * 可用于指示转换{@link Duration}时使用的格式的注释。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:39 <br/>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DurationFormat {
    /**
     * 持续时间格式样式。
     */
    DurationStyle value();
}
