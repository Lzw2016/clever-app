package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:15 <br/>
 */
@FunctionalInterface
public interface OneConsumer<A> {
    void call(A a);
}
