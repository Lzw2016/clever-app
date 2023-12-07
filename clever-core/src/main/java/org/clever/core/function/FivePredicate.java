package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:43 <br/>
 */
@FunctionalInterface
public interface FivePredicate<A, B, C, D, E> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c, D d, E e);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default FivePredicate<A, B, C, D, E> and(FivePredicate<? super A, ? super B, ? super C, ? super D, ? super E> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e) -> test(a, b, c, d, e) && other.test(a, b, c, d, e);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default FivePredicate<A, B, C, D, E> or(FivePredicate<? super A, ? super B, ? super C, ? super D, ? super E> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e) -> test(a, b, c, d, e) || other.test(a, b, c, d, e);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default FivePredicate<A, B, C, D, E> negate() {
        return (a, b, c, d, e) -> !test(a, b, c, d, e);
    }
}
