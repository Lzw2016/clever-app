package org.clever.core.convert.converter;

import org.clever.util.Assert;

/**
 * 将源类型 {@code S} 转换为目标类型 {@code T} (1:1的转换器)<br/>
 * 该接口的实现是线程安全的，可以共享<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:01 <br/>
 */
@FunctionalInterface
public interface Converter<S, T> {
    /**
     * 将 {@code S} 类型的源对象转换为 {@code T} 类型的目标对象
     *
     * @param source 要转换的源对象，它必须是 {@code S} 的实例(从不为null)
     * @return {@code T} 的实例必须为空(可能为null)
     * @throws IllegalArgumentException 如果源无法转换为所需的目标类型
     */
    T convert(S source);

    /**
     * 构造一个组合转换器，首先将此转换器应用于其输入，然后将后一个转换器应用于结果<br/>
     * 转换过程: S -> this -> T -> after -> U
     */
    default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
        Assert.notNull(after, "After Converter must not be null");
        return (S s) -> {
            T initialResult = convert(s);
            return (initialResult != null ? after.convert(initialResult) : null);
        };
    }
}
