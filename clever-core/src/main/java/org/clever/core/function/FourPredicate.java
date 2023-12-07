package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:43 <br/>
 */
@FunctionalInterface
public interface FourPredicate<A, B, C, D> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c, D d);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default FourPredicate<A, B, C, D> and(FourPredicate<? super A, ? super B, ? super C, ? super D> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d) -> test(a, b, c, d) && other.test(a, b, c, d);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default FourPredicate<A, B, C, D> or(FourPredicate<? super A, ? super B, ? super C, ? super D> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d) -> test(a, b, c, d) || other.test(a, b, c, d);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default FourPredicate<A, B, C, D> negate() {
        return (a, b, c, d) -> !test(a, b, c, d);
    }
}
