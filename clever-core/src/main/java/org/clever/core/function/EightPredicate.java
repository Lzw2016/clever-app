package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:43 <br/>
 */
@FunctionalInterface
public interface EightPredicate<A, B, C, D, E, F, G, H> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c, D d, E e, F f, G g, H h);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default EightPredicate<A, B, C, D, E, F, G, H> and(EightPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f, g, h) -> test(a, b, c, d, e, f, g, h) && other.test(a, b, c, d, e, f, g, h);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default EightPredicate<A, B, C, D, E, F, G, H> or(EightPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f, g, h) -> test(a, b, c, d, e, f, g, h) || other.test(a, b, c, d, e, f, g, h);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default EightPredicate<A, B, C, D, E, F, G, H> negate() {
        return (a, b, c, d, e, f, g, h) -> !test(a, b, c, d, e, f, g, h);
    }
}
