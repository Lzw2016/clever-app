package org.clever.core.env;

import org.clever.core.convert.support.ConfigurableConversionService;

/**
 * 配置接口将由大多数(如果不是所有){@link PropertyResolver}类型实现。
 * 提供用于访问和自定义将属性值从一种类型转换为另一种类型时使用的转换服务的工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:12 <br/>
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {
    /**
     * 返回一个 {@link ConfigurableConversionService} 在属性上执行类型转换时使用。
     * <p>返回的转换服务的可配置性质允许方便地添加和删除各个转换器实例：
     * <pre>{@code
     * ConfigurableConversionService cs = env.getConversionService();
     * cs.addConverter(new FooConverter());
     * }</pre>
     *
     * @see PropertyResolver#getProperty(String, Class)
     * @see org.clever.core.convert.converter.ConverterRegistry#addConverter
     */
    ConfigurableConversionService getConversionService();

    /**
     * 设置 {@link ConfigurableConversionService} 在属性上执行类型转换时使用。
     *
     * @see PropertyResolver#getProperty(String, Class)
     * @see #getConversionService()
     * @see org.clever.core.convert.converter.ConverterRegistry#addConverter
     */
    void setConversionService(ConfigurableConversionService conversionService);

    /**
     * 设置由此解析程序替换的占位符必须以开头的前缀
     */
    void setPlaceholderPrefix(String placeholderPrefix);

    /**
     * 设置此解析程序替换的占位符必须以其结尾的后缀
     */
    void setPlaceholderSuffix(String placeholderSuffix);

    /**
     * 指定此解析程序替换的占位符与其关联的默认值之间的分隔字符, 或null如果没有此类特殊字符，则应将其处理为值分隔符。
     */
    void setValueSeparator(String valueSeparator);

    /**
     * 设置当遇到嵌套在给定属性值中的无法解析占位符时，是否引发异常。
     * false值表示严格解析，即将引发异常。
     * true值表示应在未解析的${...}中传递无法解析的嵌套占位符类型{@link #getProperty(String)}及其变体的实现必须检查此处设置的值，
     * 以确定当属性值包含无法解析的占位符时的正确行为
     */
    void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

    /**
     * 指定必须存在哪些属性，由{@link #validateRequiredProperties()}验证
     */
    void setRequiredProperties(String... requiredProperties);

    /**
     * 验证{@link #setRequiredProperties}指定的每个属性是否存在并解析为非null值
     *
     * @throws MissingRequiredPropertiesException 如果所需的任何属性无法解决
     */
    void validateRequiredProperties() throws MissingRequiredPropertiesException;
}
