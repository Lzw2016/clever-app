package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:43 <br/>
 */
@FunctionalInterface
public interface SixPredicate<A, B, C, D, E, F> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c, D d, E e, F f);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default SixPredicate<A, B, C, D, E, F> and(SixPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f) -> test(a, b, c, d, e, f) && other.test(a, b, c, d, e, f);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default SixPredicate<A, B, C, D, E, F> or(SixPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f) -> test(a, b, c, d, e, f) || other.test(a, b, c, d, e, f);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default SixPredicate<A, B, C, D, E, F> negate() {
        return (a, b, c, d, e, f) -> !test(a, b, c, d, e, f);
    }
}
