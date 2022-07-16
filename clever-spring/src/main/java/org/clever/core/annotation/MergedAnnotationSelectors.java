package org.clever.core.annotation;

import java.lang.annotation.Annotation;

/**
 * {@link MergedAnnotationSelector}实现，为{@link MergedAnnotation}实例提供各种选项
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 14:57 <br/>
 */
public abstract class MergedAnnotationSelectors {
    private static final MergedAnnotationSelector<?> NEAREST = new Nearest();
    private static final MergedAnnotationSelector<?> FIRST_DIRECTLY_DECLARED = new FirstDirectlyDeclared();

    private MergedAnnotationSelectors() {
    }

    /**
     * 选择最近的注解，即距离最小的注解
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> MergedAnnotationSelector<A> nearest() {
        return (MergedAnnotationSelector<A>) NEAREST;
    }

    /**
     * 尽可能选择第一个直接声明的注解。如果未声明直接注解，则选择最近的注解
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> MergedAnnotationSelector<A> firstDirectlyDeclared() {
        return (MergedAnnotationSelector<A>) FIRST_DIRECTLY_DECLARED;
    }

    /**
     * {@link MergedAnnotationSelector} 选择最近的注解
     */
    private static class Nearest implements MergedAnnotationSelector<Annotation> {
        @Override
        public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
            return annotation.getDistance() == 0;
        }

        @Override
        public MergedAnnotation<Annotation> select(MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {
            if (candidate.getDistance() < existing.getDistance()) {
                return candidate;
            }
            return existing;
        }
    }

    /**
     * {@link MergedAnnotationSelector} 选择第一个直接注解
     */
    private static class FirstDirectlyDeclared implements MergedAnnotationSelector<Annotation> {
        @Override
        public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
            return annotation.getDistance() == 0;
        }

        @Override
        public MergedAnnotation<Annotation> select(MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {
            if (existing.getDistance() > 0 && candidate.getDistance() == 0) {
                return candidate;
            }
            return existing;
        }
    }
}
