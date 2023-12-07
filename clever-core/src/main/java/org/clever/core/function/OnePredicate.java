package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:42 <br/>
 */
@FunctionalInterface
public interface OnePredicate<A> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default OnePredicate<A> and(OnePredicate<? super A> other) {
        Objects.requireNonNull(other);
        return (a) -> test(a) && other.test(a);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default OnePredicate<A> or(OnePredicate<? super A> other) {
        Objects.requireNonNull(other);
        return (a) -> test(a) || other.test(a);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default OnePredicate<A> negate() {
        return (a) -> !test(a);
    }
}
