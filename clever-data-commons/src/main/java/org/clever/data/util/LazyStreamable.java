package org.clever.data.util;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@link Streamable} 的惰性实现从给定的 {@link Supplier} 获取 {@link Stream}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:24 <br/>
 */
final class LazyStreamable<T> implements Streamable<T> {
    private final Supplier<? extends Stream<T>> stream;

    private LazyStreamable(Supplier<? extends Stream<T>> stream) {
        this.stream = stream;
    }

    public static <T> LazyStreamable<T> of(Supplier<? extends Stream<T>> stream) {
        return new LazyStreamable<T>(stream);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Stream<T> stream() {
        return stream.get();
    }

    public Supplier<? extends Stream<T>> getStream() {
        return this.stream;
    }

    @Override
    public String toString() {
        return "LazyStreamable(stream=" + this.getStream() + ")";
    }
}
