package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:44 <br/>
 */
@FunctionalInterface
public interface TenPredicate<A, B, C, D, E, F, G, H, I, J> {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default TenPredicate<A, B, C, D, E, F, G, H, I, J> and(TenPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H, ? super I, ? super J> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f, g, h, i, j) -> test(a, b, c, d, e, f, g, h, i, j) && other.test(a, b, c, d, e, f, g, h, i, j);
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default TenPredicate<A, B, C, D, E, F, G, H, I, J> or(TenPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H, ? super I, ? super J> other) {
        Objects.requireNonNull(other);
        return (a, b, c, d, e, f, g, h, i, j) -> test(a, b, c, d, e, f, g, h, i, j) || other.test(a, b, c, d, e, f, g, h, i, j);
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default TenPredicate<A, B, C, D, E, F, G, H, I, J> negate() {
        return (a, b, c, d, e, f, g, h, i, j) -> !test(a, b, c, d, e, f, g, h, i, j);
    }
}
