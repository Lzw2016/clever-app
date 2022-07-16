package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.converter.ConverterRegistry;

/**
 * 可配置的类型转换服务，支持管理(注册和移除)转换器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:03 <br/>
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {
}
