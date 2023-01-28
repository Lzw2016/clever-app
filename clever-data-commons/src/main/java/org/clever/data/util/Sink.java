package org.clever.data.util;

import java.util.function.Consumer;

/**
 * 一个简单的 {@link Consumer}，它捕获传递给它的实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:27 <br/>
 */
class Sink<T> implements Consumer<T> {
    private T value;

    /**
     * 返回捕获的值
     */
    public T getValue() {
        return value;
    }

    @Override
    public void accept(T t) {
        this.value = t;
    }
}
