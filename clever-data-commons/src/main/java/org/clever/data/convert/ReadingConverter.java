package org.clever.data.convert;

import org.clever.core.convert.converter.Converter;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * 注释以阐明 {@link Converter} 作为读取转换器的预期用途，以防转换类型离开
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:04 <br/>
 */
@Target(TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadingConverter {
}
