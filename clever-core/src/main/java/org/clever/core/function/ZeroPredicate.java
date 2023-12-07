package org.clever.core.function;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/07 11:32 <br/>
 */
@FunctionalInterface
public interface ZeroPredicate {
    /**
     * 匹配 Predicate 逻辑
     *
     * @return 匹配成功 true，匹配失败 false
     */
    boolean test();

    /**
     * 用 and 连接两个 Predicate，返回新的 Predicate
     */
    default ZeroPredicate and(ZeroPredicate other) {
        Objects.requireNonNull(other);
        return () -> test() && other.test();
    }

    /**
     * 用 or 连接两个 Predicate，返回新的 Predicate
     */
    default ZeroPredicate or(ZeroPredicate other) {
        Objects.requireNonNull(other);
        return () -> test() || other.test();
    }

    /**
     * 在给定的 Predicate 前取反(!Predicate)，返回新的 Predicate
     */
    default ZeroPredicate negate() {
        return () -> !test();
    }
}
