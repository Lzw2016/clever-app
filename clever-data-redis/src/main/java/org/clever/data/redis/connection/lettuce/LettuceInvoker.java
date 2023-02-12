package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import org.clever.core.convert.converter.Converter;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.util.Assert;

import java.util.*;
import java.util.function.Supplier;

/**
 * 用于 {@link RedisClusterAsyncCommands lettuce methods} 的功能调用实用程序。
 * 通常用于将方法调用表示为方法引用，并通过 {@code just} 或 {@code from} 方法之一传递方法参数。
 * <p>
 * {@code just} 方法记录方法调用并立即评估方法结果。
 * {@code from} 方法允许编写函数管道以使用 {@link Converter} 转换结果。
 * <p>
 * 使用示例：
 * <pre>{@code
 * LettuceInvoker invoker = …;
 * Long result = invoker.just(RedisGeoAsyncCommands::geoadd, key, point.getX(), point.getY(), member);
 * List<byte[]> result = invoker.fromMany(RedisGeoAsyncCommands::geohash, key, members).toList(it -> it.getValueOrElse(null));
 * }</pre>
 * <p>
 * 来自 {@link RedisFuture} 的实际转换被委托给 {@link Synchronizer}，后者可以等待完成或沿着 {@link Converter} 记录未来以进行进一步处理。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:34 <br/>
 */
class LettuceInvoker {
    private final RedisClusterAsyncCommands<byte[], byte[]> connection;
    private final Synchronizer synchronizer;

    LettuceInvoker(RedisClusterAsyncCommands<byte[], byte[]> connection, Synchronizer synchronizer) {
        this.connection = connection;
        this.synchronizer = synchronizer;
    }

    /**
     * 调用 {@link ConnectionFunction0} 并返回其结果
     *
     * @param function 不得为 {@literal null}
     */
    <R> R just(ConnectionFunction0<R> function) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return synchronizer.invoke(() -> function.apply(connection), Converters.identityConverter(), () -> null);
    }

    /**
     * 调用 {@link ConnectionFunction1} 并返回其结果
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     */
    <R, T1> R just(ConnectionFunction1<T1, R> function, T1 t1) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return synchronizer.invoke(() -> function.apply(connection, t1));
    }

    /**
     * 调用 {@link ConnectionFunction2} 并返回其结果
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     */
    <R, T1, T2> R just(ConnectionFunction2<T1, T2, R> function, T1 t1, T2 t2) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return synchronizer.invoke(() -> function.apply(connection, t1, t2));
    }

    /**
     * 调用 {@link ConnectionFunction3} 并返回其结果
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     */
    <R, T1, T2, T3> R just(ConnectionFunction3<T1, T2, T3, R> function, T1 t1, T2 t2, T3 t3) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return synchronizer.invoke(() -> function.apply(connection, t1, t2, t3));
    }

    /**
     * 调用 {@link ConnectionFunction4} 并返回其结果
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     * @param t4       第四个参数
     */
    <R, T1, T2, T3, T4> R just(ConnectionFunction4<T1, T2, T3, T4, R> function, T1 t1, T2 t2, T3 t3, T4 t4) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return synchronizer.invoke(() -> function.apply(connection, t1, t2, t3, t4));
    }

    /**
     * 调用 {@link ConnectionFunction5} 并返回其结果
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     * @param t4       第四个参数
     * @param t5       第五个参数
     */
    @SuppressWarnings("UnusedReturnValue")
    <R, T1, T2, T3, T4, T5> R just(ConnectionFunction5<T1, T2, T3, T4, T5, R> function, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return synchronizer.invoke(() -> function.apply(connection, t1, t2, t3, t4, t5));
    }

    /**
     * 从 {@link ConnectionFunction0} 编写调用管道，并返回 {@link SingleInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     */
    <R> SingleInvocationSpec<R> from(ConnectionFunction0<R> function) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return new DefaultSingleInvocationSpec<>(() -> function.apply(connection), synchronizer);
    }

    /**
     * 从 {@link ConnectionFunction1} 编写调用管道，并返回 {@link SingleInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     */
    <R, T1> SingleInvocationSpec<R> from(ConnectionFunction1<T1, R> function, T1 t1) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return from(it -> function.apply(it, t1));
    }

    /**
     * 从 {@link ConnectionFunction2} 编写调用管道，并返回 {@link SingleInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     */
    <R, T1, T2> SingleInvocationSpec<R> from(ConnectionFunction2<T1, T2, R> function, T1 t1, T2 t2) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return from(it -> function.apply(it, t1, t2));
    }

    /**
     * 从 {@link ConnectionFunction3} 编写调用管道，并返回 {@link SingleInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     */
    <R, T1, T2, T3> SingleInvocationSpec<R> from(ConnectionFunction3<T1, T2, T3, R> function, T1 t1, T2 t2, T3 t3) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return from(it -> function.apply(it, t1, t2, t3));
    }

    /**
     * 从 {@link ConnectionFunction4} 编写调用管道，并返回 {@link SingleInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     * @param t4       第四个参数
     */
    <R, T1, T2, T3, T4> SingleInvocationSpec<R> from(ConnectionFunction4<T1, T2, T3, T4, R> function, T1 t1, T2 t2, T3 t3, T4 t4) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return from(it -> function.apply(it, t1, t2, t3, t4));
    }

    /**
     * 从 {@link ConnectionFunction5} 编写调用管道，并返回 {@link SingleInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     * @param t4       第四个参数
     * @param t5       第五个参数
     */
    <R, T1, T2, T3, T4, T5> SingleInvocationSpec<R> from(ConnectionFunction5<T1, T2, T3, T4, T5, R> function, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return from(it -> function.apply(it, t1, t2, t3, t4, t5));
    }

    /**
     * 从 {@link ConnectionFunction0} 编写一个调用管道，该管道返回类似 {@link Collection} 的结果，并返回 {@link ManyInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     */
    <R extends Collection<E>, E> ManyInvocationSpec<E> fromMany(ConnectionFunction0<R> function) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return new DefaultManyInvocationSpec<>(() -> function.apply(connection), synchronizer);
    }

    /**
     * 从 {@link ConnectionFunction1} 编写一个调用管道，该管道返回类似 {@link Collection} 的结果，并返回 {@link ManyInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       first argument.
     */
    <R extends Collection<E>, E, T1> ManyInvocationSpec<E> fromMany(ConnectionFunction1<T1, R> function, T1 t1) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return fromMany(it -> function.apply(it, t1));
    }

    /**
     * 从 {@link ConnectionFunction2} 编写一个调用管道，该管道返回类似 {@link Collection} 的结果，并返回 {@link ManyInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     */
    <R extends Collection<E>, E, T1, T2> ManyInvocationSpec<E> fromMany(ConnectionFunction2<T1, T2, R> function, T1 t1, T2 t2) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return fromMany(it -> function.apply(it, t1, t2));
    }

    /**
     * 从 {@link ConnectionFunction3} 编写一个调用管道，该管道返回类似 {@link Collection} 的结果，并返回 {@link ManyInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     */
    <R extends Collection<E>, E, T1, T2, T3> ManyInvocationSpec<E> fromMany(ConnectionFunction3<T1, T2, T3, R> function, T1 t1, T2 t2, T3 t3) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return fromMany(it -> function.apply(it, t1, t2, t3));
    }

    /**
     * 从 {@link ConnectionFunction4} 编写一个调用管道，该管道返回类似 {@link Collection} 的结果，并返回 {@link ManyInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     * @param t4       第四个参数
     */
    <R extends Collection<E>, E, T1, T2, T3, T4> ManyInvocationSpec<E> fromMany(ConnectionFunction4<T1, T2, T3, T4, R> function, T1 t1, T2 t2, T3 t3, T4 t4) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return fromMany(it -> function.apply(it, t1, t2, t3, t4));
    }

    /**
     * 从 {@link ConnectionFunction5} 编写一个调用管道，该管道返回类似 {@link Collection} 的结果，并返回 {@link ManyInvocationSpec} 以进行进一步组合
     *
     * @param function 不得为 {@literal null}
     * @param t1       第一个参数
     * @param t2       第二个参数
     * @param t3       第三个参数
     * @param t4       第四个参数
     * @param t5       第五个参数
     */
    <R extends Collection<E>, E, T1, T2, T3, T4, T5> ManyInvocationSpec<E> fromMany(ConnectionFunction5<T1, T2, T3, T4, T5, R> function, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        Assert.notNull(function, "ConnectionFunction must not be null!");
        return fromMany(it -> function.apply(it, t1, t2, t3, t4, t5));
    }

    /**
     * 表示调用 Pipelined 中的一个元素，允许通过应用 {@link Converter} 来使用结果
     */
    interface SingleInvocationSpec<S> {
        /**
         * 通过调用 {@code ConnectionFunction} 并在应用 {@link Converter} 后返回结果来具体化管道
         *
         * @param converter 不得为 {@literal null}
         * @param <T>       目标类型
         * @return 转换后的结果，可以是 {@literal null}。
         */
        <T> T get(Converter<S, T> converter);

        /**
         * 通过调用 {@code ConnectionFunction} 并在应用 {@link Converter} 后返回结果来具体化管道，或者返回 {@literal nullDefault} 值（如果不存在）
         *
         * @param converter   不得为 {@literal null}
         * @param nullDefault 可以是 {@literal null}。
         * @param <T>         目标类型
         * @return 转换后的结果，可以是 {@literal null}。
         */
        default <T> T orElse(Converter<S, T> converter, T nullDefault) {
            return getOrElse(converter, () -> nullDefault);
        }

        /**
         * 通过调用 {@code ConnectionFunction} 并在应用 {@link Converter} 后返回结果来具体化管道，或者返回 {@literal nullDefault} 值（如果不存在）
         *
         * @param converter   不得为 {@literal null}
         * @param nullDefault 不得为 {@literal null}
         * @param <T>         目标类型
         * @return 转换后的结果， 可以是 {@literal null}。
         */
        <T> T getOrElse(Converter<S, T> converter, Supplier<T> nullDefault);
    }

    /**
     * 表示返回类似 {@link Collection} 结果的方法的调用 Pipelined 中的一个元素，允许通过应用 {@link Converter} 来使用结果
     */
    interface ManyInvocationSpec<S> {
        /**
         * 通过调用 {@code ConnectionFunction} 并返回结果来具体化管道
         *
         * @return 结果为 {@link List}
         */
        default List<S> toList() {
            return toList(Converters.identityConverter());
        }

        /**
         * 通过调用 {@code ConnectionFunction} 并在应用 {@link Converter} 后返回结果来具体化管道
         *
         * @param converter 不得为 {@literal null}
         * @param <T>       目标类型
         * @return 转换后的 {@link List}
         */
        <T> List<T> toList(Converter<S, T> converter);

        /**
         * 通过调用 {@code ConnectionFunction} 并返回结果来具体化管道
         *
         * @return 结果为 {@link Set}
         */
        default Set<S> toSet() {
            return toSet(Converters.identityConverter());
        }

        /**
         * 通过调用 {@code ConnectionFunction} 并在应用 {@link Converter} 后返回结果来具体化管道
         *
         * @param converter 不得为 {@literal null}
         * @param <T>       目标类型
         * @return 转换后的 {@link Set}
         */
        <T> Set<T> toSet(Converter<S, T> converter);
    }

    /**
     * 一个接受 {@link RedisClusterAsyncCommands} 的函数，参数为 0
     */
    @FunctionalInterface
    interface ConnectionFunction0<R> {
        /**
         * 将此函数应用于参数并返回 {@link RedisFuture}
         *
         * @param connection 正在使用的连接。从不 {@literal null}
         */
        RedisFuture<R> apply(RedisClusterAsyncCommands<byte[], byte[]> connection);
    }

    /**
     * 一个接受 {@link RedisClusterAsyncCommands} 的函数，参数为 1
     */
    @FunctionalInterface
    interface ConnectionFunction1<T1, R> {
        /**
         * 将此函数应用于参数并返回 {@link RedisFuture}
         *
         * @param connection 正在使用的连接。从不 {@literal null}
         * @param t1         第一个参数
         */
        RedisFuture<R> apply(RedisClusterAsyncCommands<byte[], byte[]> connection, T1 t1);
    }

    /**
     * 一个接受 {@link RedisClusterAsyncCommands} 的函数，具有 2 个参数
     */
    @FunctionalInterface
    interface ConnectionFunction2<T1, T2, R> {
        /**
         * 将此函数应用于参数并返回 {@link RedisFuture}
         *
         * @param connection 正在使用的连接。从不 {@literal null}
         * @param t1         第一个参数
         * @param t2         第二个参数
         */
        RedisFuture<R> apply(RedisClusterAsyncCommands<byte[], byte[]> connection, T1 t1, T2 t2);
    }

    /**
     * 一个接受 {@link RedisClusterAsyncCommands} 的函数，带有 3 个参数
     */
    @FunctionalInterface
    interface ConnectionFunction3<T1, T2, T3, R> {
        /**
         * 将此函数应用于参数并返回 {@link RedisFuture}
         *
         * @param connection 正在使用的连接。从不 {@literal null}
         * @param t1         第一个参数
         * @param t2         第二个参数
         * @param t3         第三个参数
         */
        RedisFuture<R> apply(RedisClusterAsyncCommands<byte[], byte[]> connection, T1 t1, T2 t2, T3 t3);
    }

    /**
     * 一个接受 {@link RedisClusterAsyncCommands} 的函数，具有 4 个参数
     */
    @FunctionalInterface
    interface ConnectionFunction4<T1, T2, T3, T4, R> {
        /**
         * 将此函数应用于参数并返回 {@link RedisFuture}
         *
         * @param connection 正在使用的连接。从不 {@literal null}
         * @param t1         第一个参数
         * @param t2         第二个参数
         * @param t3         第三个参数
         * @param t4         第四个参数
         */
        RedisFuture<R> apply(RedisClusterAsyncCommands<byte[], byte[]> connection, T1 t1, T2 t2, T3 t3, T4 t4);
    }

    /**
     * 一个接受 {@link RedisClusterAsyncCommands} 的函数，具有 5 个参数
     */
    @FunctionalInterface
    interface ConnectionFunction5<T1, T2, T3, T4, T5, R> {
        /**
         * 将此函数应用于参数并返回 {@link RedisFuture}
         *
         * @param connection 正在使用的连接。从不为 {@literal null}
         * @param t1         第一个参数
         * @param t2         第二个参数
         * @param t3         第三个参数
         * @param t4         第四个参数
         * @param t5         第五个参数
         */
        RedisFuture<R> apply(RedisClusterAsyncCommands<byte[], byte[]> connection, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

    static class DefaultSingleInvocationSpec<S> implements SingleInvocationSpec<S> {
        private final Supplier<RedisFuture<S>> parent;
        private final Synchronizer synchronizer;

        public DefaultSingleInvocationSpec(Supplier<RedisFuture<S>> parent, Synchronizer synchronizer) {
            this.parent = parent;
            this.synchronizer = synchronizer;
        }

        @Override
        public <T> T get(Converter<S, T> converter) {
            Assert.notNull(converter, "Converter must not be null");
            return synchronizer.invoke(parent, converter, () -> null);
        }

        @Override
        public <T> T getOrElse(Converter<S, T> converter, Supplier<T> nullDefault) {
            Assert.notNull(converter, "Converter must not be null!");
            return synchronizer.invoke(parent, converter, nullDefault);
        }
    }

    static class DefaultManyInvocationSpec<S> implements ManyInvocationSpec<S> {
        private final Supplier<RedisFuture<Collection<S>>> parent;
        private final Synchronizer synchronizer;

        @SuppressWarnings({"unchecked", "rawtypes"})
        public DefaultManyInvocationSpec(Supplier<RedisFuture<? extends Collection<S>>> parent, Synchronizer synchronizer) {
            this.parent = (Supplier) parent;
            this.synchronizer = synchronizer;
        }

        @Override
        public <T> List<T> toList(Converter<S, T> converter) {
            Assert.notNull(converter, "Converter must not be null!");
            return synchronizer.invoke(parent, source -> {
                if (source.isEmpty()) {
                    return Collections.emptyList();
                }
                List<T> result = new ArrayList<>(source.size());
                for (S s : source) {
                    result.add(converter.convert(s));
                }
                return result;
            }, Collections::emptyList);
        }

        @Override
        public <T> Set<T> toSet(Converter<S, T> converter) {
            Assert.notNull(converter, "Converter must not be null!");
            return synchronizer.invoke(parent, source -> {
                if (source.isEmpty()) {
                    return Collections.emptySet();
                }
                Set<T> result = new LinkedHashSet<>(source.size());
                for (S s : source) {
                    result.add(converter.convert(s));
                }
                return result;
            }, Collections::emptySet);
        }
    }

    /**
     * 用于定义同步函数以评估 {@link RedisFuture} 的接口
     */
    @FunctionalInterface
    interface Synchronizer {
        @SuppressWarnings({"unchecked", "rawtypes"})
        default <I, T> T invoke(Supplier<RedisFuture<I>> futureSupplier) {
            return (T) doInvoke((Supplier) futureSupplier, Converters.identityConverter(), () -> null);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        default <I, T> T invoke(Supplier<RedisFuture<I>> futureSupplier, Converter<I, T> converter, Supplier<T> nullDefault) {
            return (T) doInvoke((Supplier) futureSupplier, (Converter<Object, Object>) converter, (Supplier<Object>) nullDefault);
        }

        Object doInvoke(Supplier<RedisFuture<Object>> futureSupplier, Converter<Object, Object> converter, Supplier<Object> nullDefault);
    }
}
