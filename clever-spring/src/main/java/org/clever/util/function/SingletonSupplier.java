package org.clever.util.function;

import org.clever.util.Assert;

import java.util.function.Supplier;

/**
 * 一种{@link java.util.function.Supplier}修饰符，用于缓存单例结果，
 * 并使其可从{@link #get()}（可空）和{@link #obtain()}（空安全）获得。
 * <p>{@code SingletonSupplier}可以通过工厂方法构建，也可以通过提供默认供应商作为回退的构造函数构建。
 * 这对于方法引用提供者特别有用，对于返回null并缓存结果的方法，返回默认提供者。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:04 <br/>
 *
 * @param <T> 该供应商提供的结果类型
 */
public class SingletonSupplier<T> implements Supplier<T> {
    private final Supplier<? extends T> instanceSupplier;
    private final Supplier<? extends T> defaultSupplier;
    private volatile T singletonInstance;

    /**
     * 使用给定的单例实例构建{@code SingletonSupplier}，并为实例为null的情况构建默认供应商。
     *
     * @param instance        singleton实例 (potentially {@code null})
     * @param defaultSupplier 作为后备方案的默认供应商
     */
    public SingletonSupplier(T instance, Supplier<? extends T> defaultSupplier) {
        this.instanceSupplier = null;
        this.defaultSupplier = defaultSupplier;
        this.singletonInstance = instance;
    }

    /**
     * 在实例为null的情况下，使用给定实例供应商和默认供应商构建{@code SingletonSupplier}
     *
     * @param instanceSupplier 直接实例供应商
     * @param defaultSupplier  作为后备方案的默认供应商
     */
    public SingletonSupplier(Supplier<? extends T> instanceSupplier, Supplier<? extends T> defaultSupplier) {
        this.instanceSupplier = instanceSupplier;
        this.defaultSupplier = defaultSupplier;
    }

    private SingletonSupplier(Supplier<? extends T> supplier) {
        this.instanceSupplier = supplier;
        this.defaultSupplier = null;
    }

    private SingletonSupplier(T singletonInstance) {
        this.instanceSupplier = null;
        this.defaultSupplier = null;
        this.singletonInstance = singletonInstance;
    }

    /**
     * 获取此供应商的共享单例实例。
     *
     * @return 单例实例（如果没有，则为null）
     */
    @Override
    public T get() {
        T instance = this.singletonInstance;
        if (instance == null) {
            synchronized (this) {
                instance = this.singletonInstance;
                if (instance == null) {
                    if (this.instanceSupplier != null) {
                        instance = this.instanceSupplier.get();
                    }
                    if (instance == null && this.defaultSupplier != null) {
                        instance = this.defaultSupplier.get();
                    }
                    this.singletonInstance = instance;
                }
            }
        }
        return instance;
    }

    /**
     * 获取此供应商的共享singleton实例
     *
     * @return singleton实例 (从不为 null)
     * @throws IllegalStateException 如果没有实例
     */
    public T obtain() {
        T instance = get();
        Assert.state(instance != null, "No instance from Supplier");
        return instance;
    }

    /**
     * 使用给定的singleton实例构建{@code SingletonSupplier}
     *
     * @param instance singleton实例(从不为 null)
     * @return 单一供应商(从不为 null)
     */
    public static <T> SingletonSupplier<T> of(T instance) {
        return new SingletonSupplier<>(instance);
    }

    /**
     * 使用给定的singleton实例构建{@code SingletonSupplier}
     *
     * @param instance singleton实例 (potentially {@code null})
     * @return 单例供应商，如果实例为null，则为null
     */
    public static <T> SingletonSupplier<T> ofNullable(T instance) {
        return (instance != null ? new SingletonSupplier<>(instance) : null);
    }

    /**
     * 使用给定的供应商构建{@code SingletonSupplier}
     *
     * @param supplier 实例供应商 (从不为 null)
     * @return 单一供应商 (从不为 null)
     */
    public static <T> SingletonSupplier<T> of(Supplier<T> supplier) {
        return new SingletonSupplier<>(supplier);
    }

    /**
     * 使用给定的供应商构建{@code SingletonSupplier}
     *
     * @param supplier 实例供应商(potentially {@code null})
     * @return 单一供应商，如果实例供应商为null，则为null
     */
    public static <T> SingletonSupplier<T> ofNullable(Supplier<T> supplier) {
        return (supplier != null ? new SingletonSupplier<>(supplier) : null);
    }
}
