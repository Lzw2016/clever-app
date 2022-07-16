package org.clever.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 用于在两个{@link MergedAnnotation}实例之间进行选择的策略接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 14:57 <br/>
 */
@FunctionalInterface
public interface MergedAnnotationSelector<A extends Annotation> {
    /**
     * 确定现有注解是否已知为最佳候选注解，并且可能会跳过任何后续选择
     */
    default boolean isBestCandidate(MergedAnnotation<A> annotation) {
        return false;
    }

    /**
     * 在已存在的注解和候选注解中选择最合适的注解
     *
     * @param existing  已存在的现有注解
     * @param candidate 候选注解
     */
    MergedAnnotation<A> select(MergedAnnotation<A> existing, MergedAnnotation<A> candidate);
}
