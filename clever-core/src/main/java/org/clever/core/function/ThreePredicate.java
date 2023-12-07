package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:43 <br/>
 */
@FunctionalInterface
public interface ThreePredicate<A, B, C> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default ThreePredicate<A, B, C> and(ThreePredicate<? super A, ? super B, ? super C> other) {
        Objects.requireNonNull(other);
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default ThreePredicate<A, B, C> or(ThreePredicate<? super A, ? super B, ? super C> other) {
        Objects.requireNonNull(other);
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default ThreePredicate<A, B, C> negate() {
        return (a, b, c) -> !test(a, b, c);
    }
}
