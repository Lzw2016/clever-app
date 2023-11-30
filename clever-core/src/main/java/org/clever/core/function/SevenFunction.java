package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:34 <br/>
 */
@FunctionalInterface
public interface SevenFunction<R, A, B, C, D, E, F, G> {
    R call(A a, B b, C c, D d, E e, F f, G g);
}
