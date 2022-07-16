package org.clever.boot.convert;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.core.convert.converter.ConverterRegistry;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.core.convert.converter.GenericConverter.ConvertiblePair;
import org.clever.core.convert.support.ConfigurableConversionService;
import org.clever.core.convert.support.DefaultConversionService;
import org.clever.format.*;
import org.clever.format.support.DefaultFormattingConversionService;
import org.clever.format.support.FormattingConversionService;
import org.clever.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * {@link FormattingConversionService}的一种特殊化，默认配置为适用于大多数引导应用程序的转换器和格式化程序。
 * <p>
 * 设计用于直接实例化，但也暴露了静态 {@link #addApplicationConverters} 和 {@link #addApplicationFormatters(FormatterRegistry)} 用于对注册表实例进行特别使用的实用方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:27 <br/>
 */
public class ApplicationConversionService extends FormattingConversionService {
    private static volatile ApplicationConversionService sharedInstance;

    private final boolean unmodifiable;

    public ApplicationConversionService() {
        this(null);
    }

    public ApplicationConversionService(StringValueResolver embeddedValueResolver) {
        this(embeddedValueResolver, false);
    }

    private ApplicationConversionService(StringValueResolver embeddedValueResolver, boolean unmodifiable) {
        configure(this, embeddedValueResolver);
        this.unmodifiable = unmodifiable;
    }

    @Override
    public void addPrinter(Printer<?> printer) {
        assertModifiable();
        super.addPrinter(printer);
    }

    @Override
    public void addParser(Parser<?> parser) {
        assertModifiable();
        super.addParser(parser);
    }

    @Override
    public void addFormatter(Formatter<?> formatter) {
        assertModifiable();
        super.addFormatter(formatter);
    }

    @Override
    public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
        assertModifiable();
        super.addFormatterForFieldType(fieldType, formatter);
    }

    @Override
    public void addConverter(Converter<?, ?> converter) {
        assertModifiable();
        super.addConverter(converter);
    }

    @Override
    public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
        assertModifiable();
        super.addFormatterForFieldType(fieldType, printer, parser);
    }

    @Override
    public void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory) {
        assertModifiable();
        super.addFormatterForFieldAnnotation(annotationFormatterFactory);
    }

    @Override
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter) {
        assertModifiable();
        super.addConverter(sourceType, targetType, converter);
    }

    @Override
    public void addConverter(GenericConverter converter) {
        assertModifiable();
        super.addConverter(converter);
    }

    @Override
    public void addConverterFactory(ConverterFactory<?, ?> factory) {
        assertModifiable();
        super.addConverterFactory(factory);
    }

    @Override
    public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
        assertModifiable();
        super.removeConvertible(sourceType, targetType);
    }

    private void assertModifiable() {
        if (this.unmodifiable) {
            throw new UnsupportedOperationException("This ApplicationConversionService cannot be modified");
        }
    }

    /**
     * Return {@code true} if objects of {@code sourceType} can be converted to the
     * {@code targetType} and the converter has {@code Object.class} as a supported source  type.
     *
     * @param sourceType 要测试的源类型
     * @param targetType 要测试的目标类型
     * @return 如果转换通过 {@code ObjectTo...} 转换器
     */
    public boolean isConvertViaObjectSourceType(TypeDescriptor sourceType, TypeDescriptor targetType) {
        GenericConverter converter = getConverter(sourceType, targetType);
        Set<ConvertiblePair> pairs = (converter != null) ? converter.getConvertibleTypes() : null;
        if (pairs != null) {
            for (ConvertiblePair pair : pairs) {
                if (Object.class.equals(pair.getSourceType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回一个共享的默认应用程序{@code ConversionService}实例，在需要时懒洋洋地构建它。
     * <p>
     * 注意：这个方法实际上返回了一个{@link ApplicationConversionService}实例。然而，为了二进制兼容性，保留了 {@code ConversionService} 签名
     *
     * @return 共享 {@code ApplicationConversionService} 实例（从不为null）
     */
    public static ConversionService getSharedInstance() {
        ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
        if (sharedInstance == null) {
            synchronized (ApplicationConversionService.class) {
                sharedInstance = ApplicationConversionService.sharedInstance;
                if (sharedInstance == null) {
                    sharedInstance = new ApplicationConversionService(null, true);
                    ApplicationConversionService.sharedInstance = sharedInstance;
                }
            }
        }
        return sharedInstance;
    }

    /**
     * 使用适用于大多数引导应用程序的格式化程序和转换器配置给定的{@link FormatterRegistry}。
     *
     * @param registry 要添加到的转换器注册表（也必须可转换为ConversionService，例如作为 {@link ConfigurableConversionService})
     * @throws ClassCastException 如果给定的 FormatterRegistry 无法转换为 ConversionService
     */
    public static void configure(FormatterRegistry registry, StringValueResolver stringValueResolver) {
        DefaultConversionService.addDefaultConverters(registry);
        DefaultFormattingConversionService.addDefaultFormatters(registry, stringValueResolver);
        addApplicationFormatters(registry);
        addApplicationConverters(registry);
    }

    /**
     * 添加对大多数引导应用程序有用的转换器。
     *
     * @param registry 要添加到的转换器注册表（也必须可转换为ConversionService，例如作为 {@link ConfigurableConversionService})
     * @throws ClassCastException 如果给定的 ConverterRegistry 无法转换为 ConversionService
     */
    public static void addApplicationConverters(ConverterRegistry registry) {
        addDelimitedStringConverters(registry);
        registry.addConverter(new StringToDurationConverter());
        registry.addConverter(new DurationToStringConverter());
        registry.addConverter(new NumberToDurationConverter());
        registry.addConverter(new DurationToNumberConverter());
        registry.addConverter(new StringToPeriodConverter());
        registry.addConverter(new PeriodToStringConverter());
        registry.addConverter(new NumberToPeriodConverter());
        registry.addConverter(new StringToDataSizeConverter());
        registry.addConverter(new NumberToDataSizeConverter());
        registry.addConverter(new StringToFileConverter());
        registry.addConverter(new InputStreamSourceToByteArrayConverter());
        registry.addConverterFactory(new LenientStringToEnumConverterFactory());
        registry.addConverterFactory(new LenientBooleanToEnumConverterFactory());
        if (registry instanceof ConversionService) {
            addApplicationConverters(registry, (ConversionService) registry);
        }
    }

    private static void addApplicationConverters(ConverterRegistry registry, ConversionService conversionService) {
        registry.addConverter(new CharSequenceToObjectConverter(conversionService));
    }

    /**
     * 添加转换器以支持分隔字符串。
     *
     * @param registry 要添加到的转换器注册表（也必须可转换为ConversionService，例如作为 {@link ConfigurableConversionService})
     * @throws ClassCastException 如果给定的 ConverterRegistry 无法转换为 ConversionService
     */
    public static void addDelimitedStringConverters(ConverterRegistry registry) {
        ConversionService service = (ConversionService) registry;
        registry.addConverter(new ArrayToDelimitedStringConverter(service));
        registry.addConverter(new CollectionToDelimitedStringConverter(service));
        registry.addConverter(new DelimitedStringToArrayConverter(service));
        registry.addConverter(new DelimitedStringToCollectionConverter(service));
    }

    /**
     * 添加对大多数引导应用程序有用的格式化程序。
     *
     * @param registry 注册默认格式化程序的服务
     */
    public static void addApplicationFormatters(FormatterRegistry registry) {
        registry.addFormatter(new CharArrayFormatter());
        registry.addFormatter(new InetAddressFormatter());
        registry.addFormatter(new IsoOffsetFormatter());
    }
}
