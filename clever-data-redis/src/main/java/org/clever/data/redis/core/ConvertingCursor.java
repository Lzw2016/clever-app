package org.clever.data.redis.core;

import org.clever.core.convert.converter.Converter;
import org.clever.util.Assert;

/**
 * {@link ConvertingCursor} 包装给定的光标，并在返回项目之前将给定的 {@link Converter} 应用于项目。
 * 这允许轻松地执行所需的转换，而底层实现仍然可以使用其原生类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:50 <br/>
 */
public class ConvertingCursor<S, T> implements Cursor<T> {
    private Cursor<S> delegate;
    private final Converter<S, T> converter;

    /**
     * @param cursor    Cursor 不得为 {@literal null}
     * @param converter Converter 不得为 {@literal null}
     */
    public ConvertingCursor(Cursor<S> cursor, Converter<S, T> converter) {
        Assert.notNull(cursor, "Cursor delegate must not be 'null'.");
        Assert.notNull(cursor, "Converter must not be 'null'.");
        this.delegate = cursor;
        this.converter = converter;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        return converter.convert(delegate.next());
    }

    @Override
    public void remove() {
        delegate.remove();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public long getCursorId() {
        return delegate.getCursorId();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public Cursor<T> open() {
        this.delegate = delegate.open();
        return this;
    }

    @Override
    public long getPosition() {
        return delegate.getPosition();
    }
}
