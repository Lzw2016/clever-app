package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:42 <br/>
 */
@FunctionalInterface
public interface TwoPredicate<A, B> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default TwoPredicate<A, B> and(TwoPredicate<? super A, ? super B> other) {
        Objects.requireNonNull(other);
        return (a, b) -> test(a, b) && other.test(a, b);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default TwoPredicate<A, B> or(TwoPredicate<? super A, ? super B> other) {
        Objects.requireNonNull(other);
        return (a, b) -> test(a, b) || other.test(a, b);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default TwoPredicate<A, B> negate() {
        return (a, b) -> !test(a, b);
    }
}
