package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:23 <br/>
 */
@FunctionalInterface
public interface NineConsumer<A, B, C, D, E, F, G, H, I> {
    void call(A a, B b, C c, D d, E e, F f, G g, H h, I i);
}
