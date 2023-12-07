package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:43 <br/>
 */
@FunctionalInterface
public interface SevenPredicate<A, B, C, D, E, F, G> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c, D d, E e, F f, G g);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default SevenPredicate<A, B, C, D, E, F, G> and(SevenPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f, g) -> test(a, b, c, d, e, f, g) && other.test(a, b, c, d, e, f, g);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default SevenPredicate<A, B, C, D, E, F, G> or(SevenPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f, g) -> test(a, b, c, d, e, f, g) || other.test(a, b, c, d, e, f, g);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default SevenPredicate<A, B, C, D, E, F, G> negate() {
        return (a, b, c, d, e, f, g) -> !test(a, b, c, d, e, f, g);
    }
}
