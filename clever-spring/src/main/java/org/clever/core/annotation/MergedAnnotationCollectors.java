package org.clever.core.annotation;

import org.clever.core.annotation.MergedAnnotation.Adapt;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;

/**
 * 为{@link MergedAnnotation}实例提供各种与集合相关的简化操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:04 <br/>
 */
public abstract class MergedAnnotationCollectors {
    private static final Collector.Characteristics[] NO_CHARACTERISTICS = {};
    private static final Collector.Characteristics[] IDENTITY_FINISH_CHARACTERISTICS = {Collector.Characteristics.IDENTITY_FINISH};

    private MergedAnnotationCollectors() {
    }

    /**
     * 创建一个{@link LinkedHashSet}收集器，调用{@link MergedAnnotation#synthesize()}创建合成注解，生成合成注解Set集合
     */
    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
        return Collector.of(LinkedHashSet::new, (set, annotation) -> set.add(annotation.synthesize()), MergedAnnotationCollectors::combiner);
    }

    /**
     * 创建{@link Annotation}数组收集器，调用{@link MergedAnnotation#synthesize()}创建合成注解
     *
     * @see #toAnnotationArray(IntFunction)
     */
    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Annotation[]> toAnnotationArray() {
        return toAnnotationArray(Annotation[]::new);
    }

    /**
     * 创建{@link Annotation}数组收集器，调用{@link MergedAnnotation#synthesize()}创建合成注解
     *
     * @param <A>       注解类型
     * @param <R>       返回数组类型
     * @param generator 根据集合长度和元素类型创建一个新数组的函数
     * @see #toAnnotationArray
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    public static <R extends Annotation, A extends R> Collector<MergedAnnotation<A>, ?, R[]> toAnnotationArray(IntFunction<R[]> generator) {
        return Collector.of(
                ArrayList::new,
                (list, annotation) -> list.add(annotation.synthesize()),
                MergedAnnotationCollectors::combiner,
                list -> list.toArray(generator.apply(list.size()))
        );
    }

    /**
     * 创建{@link MultiValueMap}收集器，调用{@link MergedAnnotation#asMap(Adapt...)}创建注解属性值{@link MultiValueMap}
     *
     * @see #toMultiValueMap(Function, Adapt...)
     */
    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(Adapt... adaptations) {
        return toMultiValueMap(Function.identity(), adaptations);
    }

    /**
     * 创建{@link MultiValueMap}收集器，调用{@link MergedAnnotation#asMap(Adapt...)}创建注解属性值{@link MultiValueMap}
     *
     * @param <A>         注解类型
     * @param finisher    创建{@link MultiValueMap}的finisher函数
     * @param adaptations 注解属性值转换方式
     * @see #toMultiValueMap(Adapt...)
     */
    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
            Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher,
            Adapt... adaptations) {
        Collector.Characteristics[] characteristics = (isSameInstance(finisher, Function.identity()) ? IDENTITY_FINISH_CHARACTERISTICS : NO_CHARACTERISTICS);
        return Collector.of(
                LinkedMultiValueMap::new,
                (map, annotation) -> annotation.asMap(adaptations).forEach(map::add),
                MergedAnnotationCollectors::combiner,
                finisher,
                characteristics
        );
    }

    private static boolean isSameInstance(Object instance, Object candidate) {
        return instance == candidate;
    }

    /**
     * {@link  Collection}收集器的组合器 {@link Collector#combiner()}
     */
    private static <E, C extends Collection<E>> C combiner(C collection, C additions) {
        collection.addAll(additions);
        return collection;
    }

    /**
     * {@link  MultiValueMap}收集器的组合器 {@link Collector#combiner()}
     */
    private static <K, V> MultiValueMap<K, V> combiner(MultiValueMap<K, V> map, MultiValueMap<K, V> additions) {
        map.addAll(additions);
        return map;
    }
}
