package org.clever.boot.context.properties.bind;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 返回 {@link Binder} 绑定操作结果的容器对象。可能包含成功绑定的对象或空结果。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:14 <br/>
 *
 * @param <T> 结果类型
 */
public final class BindResult<T> {
    private static final BindResult<?> UNBOUND = new BindResult<>(null);

    private final T value;

    private BindResult(T value) {
        this.value = value;
    }

    /**
     * 返回绑定的对象，如果没有绑定值，则抛出 {@link NoSuchElementException}。
     *
     * @return 界限值 (从不为 null)
     * @throws NoSuchElementException 如果没有绑定值
     * @see #isBound()
     */
    public T get() throws NoSuchElementException {
        if (this.value == null) {
            throw new NoSuchElementException("No value bound");
        }
        return this.value;
    }

    /**
     * 如果结果已绑定，则返回true。
     *
     * @return 如果结果已绑定
     */
    public boolean isBound() {
        return (this.value != null);
    }

    /**
     * 使用绑定值调用指定的使用者，如果没有绑定值，则不执行任何操作。
     *
     * @param consumer 如果值已绑定，则要执行的块
     */
    public void ifBound(Consumer<? super T> consumer) {
        Assert.notNull(consumer, "Consumer must not be null");
        if (this.value != null) {
            consumer.accept(this.value);
        }
    }

    /**
     * 将提供的映射函数应用于绑定值，如果没有绑定值，则返回更新的未绑定结果。
     *
     * @param <U>    映射函数结果的类型
     * @param mapper 应用于绑定值的映射函数。如果未绑定任何值，则不会调用映射器。
     * @return BindResult，描述将映射函数应用于此BindResult的值的结果。
     */
    public <U> BindResult<U> map(Function<? super T, ? extends U> mapper) {
        Assert.notNull(mapper, "Mapper must not be null");
        return of((this.value != null) ? mapper.apply(this.value) : null);
    }

    /**
     * 返回已绑定的对象，如果未绑定任何值，则返回其他。
     *
     * @param other 如果没有绑定值，则返回的值（可以为null）
     * @return 值（如果已绑定），否则为其他值
     */
    public T orElse(T other) {
        return (this.value != null) ? this.value : other;
    }

    /**
     * 返回绑定的对象，或者如果没有绑定值，则返回调用其他对象的结果。
     *
     * @param other 如果没有绑定值，则返回值的 {@link Supplier}
     * @return 值（如果已绑定），否则为提供的其他值
     */
    public T orElseGet(Supplier<? extends T> other) {
        return (this.value != null) ? this.value : other.get();
    }

    /**
     * 返回已绑定的对象，如果未绑定任何值，则引发由提供的供应商创建的异常。
     *
     * @param <X>               要引发的异常的类型
     * @param exceptionSupplier 将返回要抛出的异常的供应商
     * @return 现值
     * @throws X 如果没有价值
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (this.value == null) {
            throw exceptionSupplier.get();
        }
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(this.value, ((BindResult<?>) obj).value);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.value);
    }

    @SuppressWarnings("unchecked")
    static <T> BindResult<T> of(T value) {
        if (value == null) {
            return (BindResult<T>) UNBOUND;
        }
        return new BindResult<>(value);
    }
}
