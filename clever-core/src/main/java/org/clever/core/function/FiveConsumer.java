package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:22 <br/>
 */
@FunctionalInterface
public interface FiveConsumer<A, B, C, D, E> {
    void call(A a, B b, C c, D d, E e);
}
