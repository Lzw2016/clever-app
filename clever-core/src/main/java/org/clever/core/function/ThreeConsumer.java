package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:22 <br/>
 */
@FunctionalInterface
public interface ThreeConsumer<A, B, C> {
    void call(A a, B b, C c);
}
