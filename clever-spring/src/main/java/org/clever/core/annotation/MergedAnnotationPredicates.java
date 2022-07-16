package org.clever.core.annotation;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 为{@link MergedAnnotation MergedAnnotations}提供各种测试操作的Predicates实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 14:00 <br/>
 */
public abstract class MergedAnnotationPredicates {
    private MergedAnnotationPredicates() {
    }

    /**
     * 如果合并注解类型的名称包含在指定的数组中，则创建一个计算结果为true的新{@link Predicate}
     *
     * @param <A>       注解类型
     * @param typeNames 应匹配的类型名称
     */
    public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(String... typeNames) {
        return annotation -> ObjectUtils.containsElement(typeNames, annotation.getType().getName());
    }

    /**
     * 如果合并注解类型的名称包含在指定的数组中，则创建一个计算结果为true的新{@link Predicate}
     *
     * @param <A>   注解类型
     * @param types 应匹配的类型
     */
    public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(Class<?>... types) {
        return annotation -> ObjectUtils.containsElement(types, annotation.getType());
    }

    /**
     * 如果合并的注解类型包含在指定的集合中，则创建一个计算结果为true的新{@link Predicate}
     *
     * @param <A>   注解类型
     * @param types 应匹配的类型
     */
    public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(Collection<?> types) {
        return annotation -> types.stream().map(type -> type instanceof Class ? ((Class<?>) type).getName() : type.toString())
                .anyMatch(typeName -> typeName.equals(annotation.getType().getName()));
    }

    /**
     * 创建一个新的有状态的一次性{@link Predicate}，该{@link Predicate}只匹配提取值的第一次运行。<br/>
     * 例如，{@code MergedAnnotationPredicates.firstRunOf(MergedAnnotation::distance)}将匹配第一个注解以及具有相同距离的任何后续管路。<br/>
     * 注意：此{@link Predicate}仅与第一次运行匹配。一旦提取的值发生变化，{@link Predicate}总是返回false。<br/>
     * 例如，如果有一组距离为{@code [1, 1, 2, 1]}的注解，则只有前两个注解会匹配
     *
     * @param valueExtractor 用于提取要检查的值的函数
     */
    public static <A extends Annotation> Predicate<MergedAnnotation<A>> firstRunOf(Function<? super MergedAnnotation<A>, ?> valueExtractor) {
        return new FirstRunOfPredicate<>(valueExtractor);
    }

    /**
     * 创建一个新的有状态的一次性{@link Predicate}，该{@link Predicate}匹配基于提取的键的唯一注解。<br/>
     * 例如，{@code MergedAnnotationPredicates.unique(MergedAnnotation::getType)}将在第一次遇到唯一类型时匹配
     *
     * @param keyExtractor 用于提取用于测试唯一性的密钥的函数
     */
    public static <A extends Annotation, K> Predicate<MergedAnnotation<A>> unique(Function<? super MergedAnnotation<A>, K> keyExtractor) {
        return new UniquePredicate<>(keyExtractor);
    }

    /**
     * {@link Predicate} 用于的实现 {@link MergedAnnotationPredicates#firstRunOf(Function)}.
     */
    private static class FirstRunOfPredicate<A extends Annotation> implements Predicate<MergedAnnotation<A>> {
        private final Function<? super MergedAnnotation<A>, ?> valueExtractor;
        private boolean hasLastValue;
        private Object lastValue;

        FirstRunOfPredicate(Function<? super MergedAnnotation<A>, ?> valueExtractor) {
            Assert.notNull(valueExtractor, "Value extractor must not be null");
            this.valueExtractor = valueExtractor;
        }

        @Override
        public boolean test(MergedAnnotation<A> annotation) {
            if (!this.hasLastValue) {
                this.hasLastValue = true;
                this.lastValue = this.valueExtractor.apply(annotation);
            }
            Object value = this.valueExtractor.apply(annotation);
            return ObjectUtils.nullSafeEquals(value, this.lastValue);
        }
    }

    /**
     * {@link Predicate} 用于的实现 {@link MergedAnnotationPredicates#unique(Function)}.
     */
    private static class UniquePredicate<A extends Annotation, K> implements Predicate<MergedAnnotation<A>> {
        private final Function<? super MergedAnnotation<A>, K> keyExtractor;
        private final Set<K> seen = new HashSet<>();

        UniquePredicate(Function<? super MergedAnnotation<A>, K> keyExtractor) {
            Assert.notNull(keyExtractor, "Key extractor must not be null");
            this.keyExtractor = keyExtractor;
        }

        @Override
        public boolean test(MergedAnnotation<A> annotation) {
            K key = this.keyExtractor.apply(annotation);
            return this.seen.add(key);
        }
    }
}
