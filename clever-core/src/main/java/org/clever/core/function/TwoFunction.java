package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:37 <br/>
 */
@FunctionalInterface
public interface TwoFunction<R, A, B> {
    R call(A a, B b);
}
