package org.clever.data.convert;

import org.clever.core.convert.converter.GenericConverter;
import org.clever.core.convert.converter.GenericConverter.ConvertiblePair;
import org.clever.util.Assert;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * 使用 Java 8 lambda 轻松设置 {@link GenericConverter} 实例的 API，主要以双向方式，以便轻松注册为数据映射子系统的自定义类型转换器。
 * 注册从读取或写入转换器的定义开始，然后可以完成。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 13:27 <br/>
 *
 * @see #reading(Class, Class, Function)
 * @see #writing(Class, Class, Function)
 */
public interface ConverterBuilder {
    /**
     * 创建一个新的 {@link ReadingConverterBuilder} 以生成一个转换器，以将给定源（存储类型）的值读取到给定目标（域类型）中
     *
     * @param source   不能为 {@literal null}
     * @param target   不能为 {@literal null}
     * @param function 不能为 {@literal null}
     */
    static <S, T> ReadingConverterBuilder<S, T> reading(Class<S> source, Class<T> target, Function<? super S, ? extends T> function) {
        Assert.notNull(source, "Source type must not be null");
        Assert.notNull(target, "Target type must not be null");
        Assert.notNull(function, "Conversion function must not be null");
        return new DefaultConverterBuilder<>(new ConvertiblePair(source, target), Optional.empty(), Optional.of(function));
    }

    /**
     * 创建一个新的 {@link WritingConverterBuilder} 以生成一个转换器，以将给定源（域类型）的值写入给定目标（存储类型）
     *
     * @param source   不能为 {@literal null}
     * @param target   不能为 {@literal null}
     * @param function 不能为 {@literal null}
     */
    static <S, T> WritingConverterBuilder<S, T> writing(Class<S> source, Class<T> target, Function<? super S, ? extends T> function) {
        Assert.notNull(source, "Source type must not be null");
        Assert.notNull(target, "Target type must not be null");
        Assert.notNull(function, "Conversion function must not be null");
        return new DefaultConverterBuilder<>(new ConvertiblePair(target, source), Optional.of(function), Optional.empty());
    }

    /**
     * 返回要为当前 {@link ConverterBuilder} 注册的所有 {@link GenericConverter} 实例
     */
    Set<GenericConverter> getConverters();

    /**
     * 公开写入转换器
     */
    interface WritingConverterAware {
        /**
         * 返回已创建的写入转换器
         */
        GenericConverter getWritingConverter();
    }

    /**
     * 公开读取转换器
     */
    interface ReadingConverterAware {
        /**
         * 返回已创建的读取转换器
         */
        GenericConverter getReadingConverter();
    }

    /**
     * 表示 {@link ConverterAware} 的中间设置步骤的接口，首先定义读取转换器
     */
    interface ReadingConverterBuilder<T, S> extends ConverterBuilder, ReadingConverterAware {
        /**
         * 通过注册给定的 {@link Function} 来创建新的 @link{ ConverterAware}
         *
         * @param function 不能为 {@literal null}
         */
        ConverterAware andWriting(Function<? super S, ? extends T> function);
    }

    /**
     * 表示 {@link ConverterAware} 的中间设置步骤的接口，首先定义写入转换器。
     */
    interface WritingConverterBuilder<S, T> extends ConverterBuilder, WritingConverterAware {
        /**
         * 通过注册给定的 {@link Function} 来创建新的 @link{ ConverterAware}。
         *
         * @param function 不能为 {@literal null}
         */
        ConverterAware andReading(Function<? super T, ? extends S> function);
    }

    /**
     * 一个 {@link ConverterBuilder} 知道读取和写入转换器
     */
    interface ConverterAware extends ConverterBuilder, ReadingConverterAware, WritingConverterAware {
    }
}
