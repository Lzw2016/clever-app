package org.clever.format;

import org.clever.core.convert.converter.Converter;

/**
 * 通过{@link FormatterRegistry} SPI向FormattingConversionService注册{@link Formatter Formatters}和{@link Converter Converters}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:00 <br/>
 */
public interface FormatterRegistrar {
    /**
     * 通过FormatterRegistry SPI向FormattingConversionService注册格式化程序和转换器
     */
    void registerFormatters(FormatterRegistry registry);
}
