package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:35 <br/>
 */
@FunctionalInterface
public interface FiveFunction<R, A, B, C, D, E> {
    R call(A a, B b, C c, D d, E e);
}
