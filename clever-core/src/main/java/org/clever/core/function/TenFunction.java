package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:32 <br/>
 */
@FunctionalInterface
public interface TenFunction<R, A, B, C, D, E, F, G, H, I, J> {
    R call(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
}
