package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:36 <br/>
 */
@FunctionalInterface
public interface ThreeFunction<R, A, B, C> {
    R call(A a, B b, C c);
}
