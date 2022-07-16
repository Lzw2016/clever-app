package org.clever.format.support;

import org.clever.context.i18n.LocaleContextHolder;
import org.clever.core.GenericTypeResolver;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.core.convert.support.GenericConversionService;
import org.clever.format.*;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个{@link org.clever.core.convert.ConversionService}的实现，同时实现{@link FormatterRegistry}接口<br/>
 * 支持转换过程中的数据格式化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:29 <br/>
 */
public class FormattingConversionService extends GenericConversionService implements FormatterRegistry {
    /**
     * 用于Printer格式化的缓存
     */
    private final Map<AnnotationConverterKey, GenericConverter> cachedPrinters = new ConcurrentHashMap<>(64);
    /**
     * 用于Parser格式化的缓存
     */
    private final Map<AnnotationConverterKey, GenericConverter> cachedParsers = new ConcurrentHashMap<>(64);
    
    @Override
    public void addPrinter(Printer<?> printer) {
        Class<?> fieldType = getFieldType(printer, Printer.class);
        addConverter(new PrinterConverter(fieldType, printer, this));
    }

    @Override
    public void addParser(Parser<?> parser) {
        Class<?> fieldType = getFieldType(parser, Parser.class);
        addConverter(new ParserConverter(fieldType, parser, this));
    }

    @Override
    public void addFormatter(Formatter<?> formatter) {
        addFormatterForFieldType(getFieldType(formatter), formatter);
    }

    @Override
    public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
        addConverter(new PrinterConverter(fieldType, formatter, this));
        addConverter(new ParserConverter(fieldType, formatter, this));
    }

    @Override
    public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
        addConverter(new PrinterConverter(fieldType, printer, this));
        addConverter(new ParserConverter(fieldType, parser, this));
    }

    @Override
    public void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory) {
        Class<? extends Annotation> annotationType = getAnnotationType(annotationFormatterFactory);
        Set<Class<?>> fieldTypes = annotationFormatterFactory.getFieldTypes();
        for (Class<?> fieldType : fieldTypes) {
            addConverter(new AnnotationPrinterConverter(annotationType, annotationFormatterFactory, fieldType));
            addConverter(new AnnotationParserConverter(annotationType, annotationFormatterFactory, fieldType));
        }
    }

    /**
     * 获取 {@code Formatter<?>} 的泛型类型
     */
    static Class<?> getFieldType(Formatter<?> formatter) {
        return getFieldType(formatter, Formatter.class);
    }

    /**
     * 获取{@code instance}对象对应泛型类型{@code genericInterface}的泛型类型参数
     */
    private static <T> Class<?> getFieldType(T instance, Class<T> genericInterface) {
        Class<?> fieldType = GenericTypeResolver.resolveTypeArgument(instance.getClass(), genericInterface);
        Assert.notNull(
                fieldType,
                () -> "Unable to extract the parameterized field type from " +
                        ClassUtils.getShortName(genericInterface) +
                        " [" + instance.getClass().getName() + "]; " +
                        "does the class parameterize the <T> generic type?"
        );
        return fieldType;
    }

    /**
     * 获取 {@code AnnotationFormatterFactory<? extends Annotation>} 的泛型类型
     */
    @SuppressWarnings("unchecked")
    static Class<? extends Annotation> getAnnotationType(AnnotationFormatterFactory<? extends Annotation> factory) {
        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) GenericTypeResolver.resolveTypeArgument(factory.getClass(), AnnotationFormatterFactory.class);
        if (annotationType == null) {
            throw new IllegalArgumentException(
                    "Unable to extract parameterized Annotation type argument from " +
                            "AnnotationFormatterFactory [" + factory.getClass().getName() + "]; " +
                            "does the factory parameterize the <A extends Annotation> generic type?"
            );
        }
        return annotationType;
    }

    /**
     * 把Printer包装成一个GenericConverter，能够把fieldType类型转换为String
     */
    private static class PrinterConverter implements GenericConverter {
        /**
         * Printer需要格式化的类型
         */
        private final Class<?> fieldType;
        /**
         * {@link #printer}类型的泛型类型TypeDescriptor
         */
        private final TypeDescriptor printerObjectType;
        /**
         * 被包装的原始Printer
         */
        @SuppressWarnings("rawtypes")
        private final Printer printer;
        /**
         * 类型转换服务，用于将源对象转换成{@link #printerObjectType}类型
         */
        private final ConversionService conversionService;

        public PrinterConverter(Class<?> fieldType, Printer<?> printer, ConversionService conversionService) {
            this.fieldType = fieldType;
            this.printerObjectType = TypeDescriptor.valueOf(resolvePrinterObjectType(printer));
            this.printer = printer;
            this.conversionService = conversionService;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (!sourceType.isAssignableTo(this.printerObjectType)) {
                source = this.conversionService.convert(source, sourceType, this.printerObjectType);
            }
            if (source == null) {
                return "";
            }
            return this.printer.print(source, LocaleContextHolder.getLocale());
        }

        /**
         * 获取 {@code Printer<?>} 类型的泛型类型
         */
        private Class<?> resolvePrinterObjectType(Printer<?> printer) {
            return GenericTypeResolver.resolveTypeArgument(printer.getClass(), Printer.class);
        }

        @Override
        public String toString() {
            return (this.fieldType.getName() + " -> " + String.class.getName() + " : " + this.printer);
        }
    }

    /**
     * 把Parser包装成一个GenericConverter，能够把String转换为fieldType类型
     */
    private static class ParserConverter implements GenericConverter {
        /**
         * Parser需要格式化解析的类型
         */
        private final Class<?> fieldType;
        /**
         * 被包装的原始Parser
         */
        private final Parser<?> parser;
        /**
         * 类型转换服务，用于将Parser格式化后的对象转换成targetType类型
         */
        private final ConversionService conversionService;

        public ParserConverter(Class<?> fieldType, Parser<?> parser, ConversionService conversionService) {
            this.fieldType = fieldType;
            this.parser = parser;
            this.conversionService = conversionService;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            String text = (String) source;
            if (!StringUtils.hasText(text)) {
                return null;
            }
            Object result;
            try {
                result = this.parser.parse(text, LocaleContextHolder.getLocale());
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
            }
            TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
            if (!resultType.isAssignableTo(targetType)) {
                result = this.conversionService.convert(result, resultType, targetType);
            }
            return result;
        }

        @Override
        public String toString() {
            return String.class.getName() + " -> " + this.fieldType.getName() + ": " + this.parser;
        }
    }

    /**
     * 把AnnotationFormatterFactory包装成一个ConditionalGenericConverter<br/>
     * 基于Printer的格式化转换器，能够把fieldType类型转换为String
     */
    private class AnnotationPrinterConverter implements ConditionalGenericConverter {
        /**
         * 支持的Annotation类型
         */
        private final Class<? extends Annotation> annotationType;
        /**
         * 被包装的AnnotationFormatterFactory原始对象
         */
        @SuppressWarnings("rawtypes")
        private final AnnotationFormatterFactory annotationFormatterFactory;
        /**
         * 支持的格式化对象类型
         */
        private final Class<?> fieldType;

        public AnnotationPrinterConverter(Class<? extends Annotation> annotationType, AnnotationFormatterFactory<?> annotationFormatterFactory, Class<?> fieldType) {
            this.annotationType = annotationType;
            this.annotationFormatterFactory = annotationFormatterFactory;
            this.fieldType = fieldType;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return sourceType.hasAnnotation(this.annotationType);
        }

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            Annotation ann = sourceType.getAnnotation(this.annotationType);
            if (ann == null) {
                throw new IllegalStateException("Expected [" + this.annotationType.getName() + "] to be present on " + sourceType);
            }
            AnnotationConverterKey converterKey = new AnnotationConverterKey(ann, sourceType.getObjectType());
            GenericConverter converter = cachedPrinters.get(converterKey);
            if (converter == null) {
                Printer<?> printer = this.annotationFormatterFactory.getPrinter(converterKey.getAnnotation(), converterKey.getFieldType());
                converter = new PrinterConverter(this.fieldType, printer, FormattingConversionService.this);
                cachedPrinters.put(converterKey, converter);
            }
            return converter.convert(source, sourceType, targetType);
        }

        @Override
        public String toString() {
            return "@" + this.annotationType.getName() + " " + this.fieldType.getName() + " -> " + String.class.getName() + ": " + this.annotationFormatterFactory;
        }
    }

    /**
     * 把AnnotationFormatterFactory包装成一个ConditionalGenericConverter<br/>
     * 基于ParserC的格式化转换器，能够把String转换为fieldType类型
     */
    private class AnnotationParserConverter implements ConditionalGenericConverter {
        /**
         * 支持的Annotation类型
         */
        private final Class<? extends Annotation> annotationType;
        /**
         * 被包装的AnnotationFormatterFactory原始对象
         */
        @SuppressWarnings("rawtypes")
        private final AnnotationFormatterFactory annotationFormatterFactory;
        /**
         * 支持格式化解析的对象类型
         */
        private final Class<?> fieldType;

        public AnnotationParserConverter(Class<? extends Annotation> annotationType, AnnotationFormatterFactory<?> annotationFormatterFactory, Class<?> fieldType) {
            this.annotationType = annotationType;
            this.annotationFormatterFactory = annotationFormatterFactory;
            this.fieldType = fieldType;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return targetType.hasAnnotation(this.annotationType);
        }

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            Annotation ann = targetType.getAnnotation(this.annotationType);
            if (ann == null) {
                throw new IllegalStateException("Expected [" + this.annotationType.getName() + "] to be present on " + targetType);
            }
            AnnotationConverterKey converterKey = new AnnotationConverterKey(ann, targetType.getObjectType());
            GenericConverter converter = cachedParsers.get(converterKey);
            if (converter == null) {
                Parser<?> parser = this.annotationFormatterFactory.getParser(converterKey.getAnnotation(), converterKey.getFieldType());
                converter = new ParserConverter(this.fieldType, parser, FormattingConversionService.this);
                cachedParsers.put(converterKey, converter);
            }
            return converter.convert(source, sourceType, targetType);
        }

        @Override
        public String toString() {
            return String.class.getName() + " -> @" + this.annotationType.getName() + " " + this.fieldType.getName() + ": " + this.annotationFormatterFactory;
        }
    }

    /**
     * 格式化器的缓存key
     */
    private static class AnnotationConverterKey {
        private final Annotation annotation;
        private final Class<?> fieldType;

        public AnnotationConverterKey(Annotation annotation, Class<?> fieldType) {
            this.annotation = annotation;
            this.fieldType = fieldType;
        }

        public Annotation getAnnotation() {
            return this.annotation;
        }

        public Class<?> getFieldType() {
            return this.fieldType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationConverterKey)) {
                return false;
            }
            AnnotationConverterKey otherKey = (AnnotationConverterKey) other;
            return (this.fieldType == otherKey.fieldType && this.annotation.equals(otherKey.annotation));
        }

        @Override
        public int hashCode() {
            return (this.fieldType.hashCode() * 29 + this.annotation.hashCode());
        }
    }
}
