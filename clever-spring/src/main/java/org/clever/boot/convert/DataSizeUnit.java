package org.clever.boot.convert;

import org.clever.util.unit.DataSize;
import org.clever.util.unit.DataUnit;

import java.lang.annotation.*;

/**
 * 可用于更改转换{@link DataSize}时使用的默认单位的注释。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:51 <br/>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSizeUnit {
    DataUnit value();
}
