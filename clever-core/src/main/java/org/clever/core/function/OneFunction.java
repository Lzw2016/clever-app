package org.clever.core.function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/30 23:37 <br/>
 */
@FunctionalInterface
public interface OneFunction<R, A> {
    R call(A a);
}
