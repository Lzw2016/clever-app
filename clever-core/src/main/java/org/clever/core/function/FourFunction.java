package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:36 <br/>
 */
@FunctionalInterface
public interface FourFunction<R, A, B, C, D> {
    R call(A a, B b, C c, D d);
}
