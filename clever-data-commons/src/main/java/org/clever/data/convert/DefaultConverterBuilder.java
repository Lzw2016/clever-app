package org.clever.data.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.core.convert.converter.GenericConverter.ConvertiblePair;
import org.clever.data.convert.ConverterBuilder.ConverterAware;
import org.clever.data.convert.ConverterBuilder.ReadingConverterBuilder;
import org.clever.data.convert.ConverterBuilder.WritingConverterBuilder;
import org.clever.data.util.Optionals;
import org.clever.util.ObjectUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 构建器，用于使用 Lambdas 为数据类型映射轻松设置（双向）{@link Converter} 实例。
 * 在 {@link ConverterBuilder} 上使用工厂方法来创建此类的实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 13:31 <br/>
 *
 * @see ConverterBuilder#writing(Class, Class, Function)
 * @see ConverterBuilder#reading(Class, Class, Function)
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class DefaultConverterBuilder<S, T> implements ConverterAware, ReadingConverterBuilder<T, S>, WritingConverterBuilder<S, T> {
    private final ConvertiblePair convertiblePair;
    private final Optional<Function<? super S, ? extends T>> writing;
    private final Optional<Function<? super T, ? extends S>> reading;

    DefaultConverterBuilder(ConvertiblePair convertiblePair,
                            Optional<Function<? super S, ? extends T>> writing,
                            Optional<Function<? super T, ? extends S>> reading) {
        this.convertiblePair = convertiblePair;
        this.writing = writing;
        this.reading = reading;
    }

    @Override
    public ConverterAware andReading(Function<? super T, ? extends S> function) {
        return withReading(Optional.of(function));
    }

    @Override
    public ConverterAware andWriting(Function<? super S, ? extends T> function) {
        return withWriting(Optional.of(function));
    }

    @Override
    public GenericConverter getReadingConverter() {
        return getOptionalReadingConverter().orElseThrow(() -> new IllegalStateException("No reading converter specified"));
    }

    @Override
    public GenericConverter getWritingConverter() {
        return getOptionalWritingConverter().orElseThrow(() -> new IllegalStateException("No writing converter specified"));
    }

    @Override
    public Set<GenericConverter> getConverters() {
        return Optionals.toStream(getOptionalReadingConverter(), getOptionalWritingConverter()).collect(Collectors.toSet());
    }

    private Optional<GenericConverter> getOptionalReadingConverter() {
        return reading.map(it -> new ConfigurableGenericConverter.Reading<>(convertiblePair, it));
    }

    private Optional<GenericConverter> getOptionalWritingConverter() {
        return writing.map(it -> new ConfigurableGenericConverter.Writing<>(invertedPair(), it));
    }

    private ConvertiblePair invertedPair() {
        return new ConvertiblePair(convertiblePair.getTargetType(), convertiblePair.getSourceType());
    }

    DefaultConverterBuilder<S, T> withWriting(Optional<Function<? super S, ? extends T>> writing) {
        return this.writing == writing ? this : new DefaultConverterBuilder<S, T>(this.convertiblePair, writing, this.reading);
    }

    DefaultConverterBuilder<S, T> withReading(Optional<Function<? super T, ? extends S>> reading) {
        return this.reading == reading ? this : new DefaultConverterBuilder<S, T>(this.convertiblePair, this.writing, reading);
    }

    private static class ConfigurableGenericConverter<S, T> implements GenericConverter {
        private final ConvertiblePair convertiblePair;
        private final Function<? super S, ? extends T> function;

        public ConfigurableGenericConverter(ConvertiblePair convertiblePair, Function<? super S, ? extends T> function) {
            this.convertiblePair = convertiblePair;
            this.function = function;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return function.apply((S) source);
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(convertiblePair);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConfigurableGenericConverter)) {
                return false;
            }
            ConfigurableGenericConverter<?, ?> that = (ConfigurableGenericConverter<?, ?>) o;
            if (!ObjectUtils.nullSafeEquals(convertiblePair, that.convertiblePair)) {
                return false;
            }
            return ObjectUtils.nullSafeEquals(function, that.function);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(convertiblePair);
            result = 31 * result + ObjectUtils.nullSafeHashCode(function);
            return result;
        }

        @WritingConverter
        private static class Writing<S, T> extends ConfigurableGenericConverter<S, T> {
            Writing(ConvertiblePair convertiblePair, Function<? super S, ? extends T> function) {
                super(convertiblePair, function);
            }
        }

        @ReadingConverter
        private static class Reading<S, T> extends ConfigurableGenericConverter<S, T> {
            Reading(ConvertiblePair convertiblePair, Function<? super S, ? extends T> function) {
                super(convertiblePair, function);
            }
        }
    }
}
