package org.clever.core.annotation;

import org.clever.core.annotation.MergedAnnotation.Adapt;
import org.clever.core.annotation.MergedAnnotations.SearchStrategy;
import org.clever.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于在{@link AnnotatedElement }上查找注解、元注解和可重复注解的通用实用类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:06 <br/>
 */
public abstract class AnnotatedElementUtils {
    /**
     * 为给定的注解构建一个适应的{@link AnnotatedElement}，通常用于{@link AnnotatedElementUtils}上的其他方法
     */
    public static AnnotatedElement forAnnotations(Annotation... annotations) {
        return new AnnotatedElementForAnnotations(annotations);
    }

    /**
     * 获取提供的{@link AnnotatedElement}上(指定{@code annotationType}的)注解上存在的所有元注解类型的完全限定类名
     */
    public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return getMetaAnnotationTypes(element, element.getAnnotation(annotationType));
    }

    /**
     * 获取提供的{@link AnnotatedElement}上(指定{@code annotationName}的)注解上存在的所有元注解类型的完全限定类名
     */
    public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return getMetaAnnotationTypes(element, annotation);
            }
        }
        return Collections.emptySet();
    }

    private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Annotation annotation) {
        if (annotation == null) {
            return Collections.emptySet();
        }
        return getAnnotations(annotation.annotationType()).stream()
                .map(mergedAnnotation -> mergedAnnotation.getType().getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 确定提供的{@link AnnotatedElement}是否使用复合注解进行注解，该复合注解是使用指定{@code annotationType}的注解进行元注解的
     */
    public static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return getAnnotations(element).stream(annotationType).anyMatch(MergedAnnotation::isMetaPresent);
    }

    /**
     * 确定提供的{@link AnnotatedElement}是否使用复合注解进行注解，该复合注解是使用指定{@code annotationName}的注解进行元注解的
     */
    public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
        return getAnnotations(element).stream(annotationName).anyMatch(MergedAnnotation::isMetaPresent);
    }

    /**
     * 判断{@code annotationType}的注解是否存在于{@link AnnotatedElement}上，或位于指定element上方的注解层次结构中
     *
     * @param element        带注解的element
     * @param annotationType 要查找的注解类型
     */
    public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) || AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
            return element.isAnnotationPresent(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return getAnnotations(element).isPresent(annotationType);
    }

    /**
     * 判断{@code annotationName}的注解是否存在于{@link AnnotatedElement}上，或位于指定element上方的注解层次结构中
     */
    public static boolean isAnnotated(AnnotatedElement element, String annotationName) {
        return getAnnotations(element).isPresent(annotationName);
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次中指定{@code annotationType}的第一个注解，
     * 并将该注解的属性与注解层次较低级别中注解的匹配属性合并
     * 在单个注解和注解层次结构中都完全支持{@link  AliasFor @AliasFor}语义。
     * 此方法委托给{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}
     */
    public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        MergedAnnotation<?> mergedAnnotation = getAnnotations(element).get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
        return getAnnotationAttributes(mergedAnnotation, false, false);
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次中指定{@code annotationName}的第一个注解，
     * 并将该注解的属性与注解层次较低级别中注解的匹配属性合并
     * 在单个注解和注解层次结构中都完全支持{@link  AliasFor @AliasFor}语义。
     * 此方法委托给{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}
     */
    public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, String annotationName) {
        return getMergedAnnotationAttributes(element, annotationName, false, false);
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次中指定{@code annotationName}的第一个注解，
     * 并将该注解的属性与注解层次较低级别中注解的匹配属性合并。
     * 注解层次结构中较低级别的属性覆盖较高级别的同名属性，并且在单个注解和注解层次结构中都完全支持@AliasFor语义。
     * 与getAllAnnotationAttributes不同，一旦找到指定annotationName的第一个注解，此方法使用的搜索算法将停止搜索注解层次。因此，将忽略指定annotationName的其他注解。
     *
     * @param element                AnnotatedElement
     * @param annotationName         annotationName
     * @param classValuesAsString    是否将将类引用转换为字符串
     * @param nestedAnnotationsAsMap 是将嵌套注解实例转换为AnnotationAttributes，还是将其保留为注解实例
     */
    public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
                                                                     String annotationName,
                                                                     boolean classValuesAsString,
                                                                     boolean nestedAnnotationsAsMap) {
        MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
                .get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
        return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
    }

    /**
     * 获取所提供element上方注解层次中指定{@code annotationType}的第一个注解，<br/>
     * 将该注解的属性与注解层次较低级别中注解的匹配属性合并，<br/>
     * 然后将结果合成回指定{@code annotationType}的注解，<br/>
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义<br/>
     *
     * @param element        带注解的元素element
     * @param annotationType 要查找的注解类型
     * @return 未找到返回 null
     */
    public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) || AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
            return element.getDeclaredAnnotation(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return getAnnotations(element).get(
                annotationType,
                null,
                MergedAnnotationSelectors.firstDirectlyDeclared()
        ).synthesize(MergedAnnotation::isPresent).orElse(null);
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次结构中指定{@code annotationType}的所有注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回指定{@code annotationType}的注解。
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     */
    public static <A extends Annotation> Set<A> getAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
        return getAnnotations(element).stream(annotationType).collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次结构中指定注解类型的所有注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回相应{@code annotationType}的注解。
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     */
    public static Set<Annotation> getAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
        return getAnnotations(element).stream()
                .filter(MergedAnnotationPredicates.typeIn(annotationTypes))
                .collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次结构中指定{@code annotationType}的所有可重复注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回指定{@code annotationType}的注解。
     * 保存可重复注解的容器类型将通过注解进行查找。可重复。
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     */
    public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(AnnotatedElement element, Class<A> annotationType) {
        return getMergedRepeatableAnnotations(element, annotationType, null);
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次结构中指定{@code annotationType}的所有可重复注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回指定{@code annotationType}的注解。
     * 保存可重复注解的容器类型将通过注解进行查找。可重复。
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     *
     * @param element        AnnotatedElement
     * @param annotationType annotationType
     * @param containerType  保存注解的容器的类型；如果应通过注解查找容器类型，则可能为null
     */
    public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(AnnotatedElement element,
                                                                               Class<A> annotationType,
                                                                               Class<? extends Annotation> containerType) {
        return getRepeatableAnnotations(element, containerType, annotationType)
                .stream(annotationType)
                .collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次结构中指定{@code annotationName}的所有注解的注解属性，并将结果存储在MultiValueMap中
     * 注意：与{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}不同，此方法不支持属性重写
     */
    public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element, String annotationName) {
        return getAllAnnotationAttributes(element, annotationName, false, false);
    }

    /**
     * 获取所提供{@link AnnotatedElement}上方注解层次结构中指定{@code annotationName}的所有注解的注解属性，并将结果存储在MultiValueMap中
     * 注意：与{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}不同，此方法不支持属性重写
     *
     * @param element                AnnotatedElement
     * @param annotationName         annotationName
     * @param classValuesAsString    是否将将类引用转换为字符串
     * @param nestedAnnotationsAsMap 是将嵌套注解实例转换为AnnotationAttributes，还是将其保留为注解实例
     */
    public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
                                                                           String annotationName,
                                                                           final boolean classValuesAsString,
                                                                           final boolean nestedAnnotationsAsMap) {
        Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
        return getAnnotations(element).stream(annotationName)
                .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
                .map(MergedAnnotation::withNonMergedAttributes)
                .collect(MergedAnnotationCollectors.toMultiValueMap(AnnotatedElementUtils::nullIfEmpty, adaptations));
    }

    /**
     * 确定指定{@code annotationType}的注解在提供的{@link AnnotatedElement}上或在指定{@link AnnotatedElement}上方的注解层次结构中是否可用。
     * 如果此方法返回true，则{@link #findMergedAnnotationAttributes}将返回非null值
     */
    public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) ||
                AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
            return element.isAnnotationPresent(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return findAnnotations(element).isPresent(annotationType);
    }

    /**
     * 在提供的{@link AnnotatedElement}上方的注解层次中查找指定{@code annotationType}的第一个注解，
     * 并将该注解的属性与注解层次较低级别中注解的匹配属性合并。
     * 注解层次结构中较低级别的属性覆盖较高级别的同名属性，并且在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义。
     * 与{@link #getAllAnnotationAttributes}不同，一旦找到指定{@code annotationType}的第一个注解，
     * 此方法使用的搜索算法将停止搜索注解层次。因此，将忽略指定{@code annotationType}的其他注解
     *
     * @param element                AnnotatedElement
     * @param annotationType         annotationType
     * @param classValuesAsString    是否将将类引用转换为字符串
     * @param nestedAnnotationsAsMap 是将嵌套注解实例转换为AnnotationAttributes，还是将其保留为注解实例
     */
    public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
                                                                      Class<? extends Annotation> annotationType,
                                                                      boolean classValuesAsString,
                                                                      boolean nestedAnnotationsAsMap) {
        MergedAnnotation<?> mergedAnnotation = findAnnotations(element).get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
        return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
    }

    /**
     * 在提供的{@link AnnotatedElement}上方的注解层次中查找指定{@code annotationName}的第一个注解，
     * 并将该注解的属性与注解层次较低级别中注解的匹配属性合并。
     * 注解层次结构中较低级别的属性覆盖较高级别的同名属性，并且在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义。
     * 与{@link #getAllAnnotationAttributes}不同，一旦找到指定{@code annotationName}的第一个注解，
     * 此方法使用的搜索算法将停止搜索注解层次。因此，将忽略指定{@code annotationName}的其他注解
     *
     * @param element                AnnotatedElement
     * @param annotationName         annotationName
     * @param classValuesAsString    是否将将类引用转换为字符串
     * @param nestedAnnotationsAsMap 是将嵌套注解实例转换为AnnotationAttributes，还是将其保留为注解实例
     */
    public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
                                                                      String annotationName,
                                                                      boolean classValuesAsString,
                                                                      boolean nestedAnnotationsAsMap) {
        MergedAnnotation<?> mergedAnnotation = findAnnotations(element).get(
                annotationName,
                null,
                MergedAnnotationSelectors.firstDirectlyDeclared()
        );
        return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
    }

    /**
     * 在提供的{@link AnnotatedElement}上方的注解层次中查找指定注解类型的第一个注解，
     * 将该注解的属性与注解层次较低级别中注解的匹配属性合并，然后将结果合成回指定注解类型的注解。
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     */
    public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) ||
                AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
            return element.getDeclaredAnnotation(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return findAnnotations(element)
                .get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
                .synthesize(MergedAnnotation::isPresent).orElse(null);
    }

    /**
     * 在所提供{@link AnnotatedElement}上方的注解层次结构中查找指定{@code annotationType}的所有注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回指定{@code annotationType}的注解
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     */
    public static <A extends Annotation> Set<A> findAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
        return findAnnotations(element).stream(annotationType)
                .sorted(highAggregateIndexesFirst())
                .collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    /**
     * 在所提供{@link AnnotatedElement}上方的注解层次结构中查找指定{@code annotationType}的所有注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回相应{@code annotationType}注解
     * 在单个注解和注解层次结构中都完全支持{@link AliasFor @AliasFor}语义
     */
    public static Set<Annotation> findAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
        return findAnnotations(element).stream()
                .filter(MergedAnnotationPredicates.typeIn(annotationTypes))
                .sorted(highAggregateIndexesFirst())
                .collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    /**
     * 在所提供{@link AnnotatedElement}上方的注解层次结构中查找指定{@code annotationType}的所有可重复注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回指定{@code annotationType}的注解。
     * 保存可重复注解的容器类型将通过注解进行查找{@link java.lang.annotation.Repeatable}
     */
    public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
                                                                                Class<A> annotationType) {
        return findMergedRepeatableAnnotations(element, annotationType, null);
    }

    /**
     * 在所提供{@link AnnotatedElement}上方的注解层次结构中查找指定{@code annotationType}的所有可重复注解；
     * 对于找到的每个注解，将该注解的属性与注解层次较低级别中注解的匹配属性合并，并将结果合成回指定{@code annotationType}的注解。
     * 保存可重复注解的容器类型将通过注解进行查找{@link java.lang.annotation.Repeatable}
     *
     * @param containerType 保存注解的容器的类型；如果应通过注解查找容器类型，则可能为null
     */
    public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
                                                                                Class<A> annotationType,
                                                                                Class<? extends Annotation> containerType) {
        return findRepeatableAnnotations(element, containerType, annotationType)
                .stream(annotationType)
                .sorted(highAggregateIndexesFirst())
                .collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    private static MergedAnnotations getAnnotations(AnnotatedElement element) {
        return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none());
    }

    private static MergedAnnotations getRepeatableAnnotations(AnnotatedElement element,
                                                              Class<? extends Annotation> containerType,
                                                              Class<? extends Annotation> annotationType) {
        RepeatableContainers repeatableContainers = RepeatableContainers.of(annotationType, containerType);
        return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, repeatableContainers);
    }

    private static MergedAnnotations findAnnotations(AnnotatedElement element) {
        return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());
    }

    private static MergedAnnotations findRepeatableAnnotations(AnnotatedElement element,
                                                               Class<? extends Annotation> containerType,
                                                               Class<? extends Annotation> annotationType) {
        RepeatableContainers repeatableContainers = RepeatableContainers.of(annotationType, containerType);
        return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, repeatableContainers);
    }

    private static MultiValueMap<String, Object> nullIfEmpty(MultiValueMap<String, Object> map) {
        return (map.isEmpty() ? null : map);
    }

    private static <A extends Annotation> Comparator<MergedAnnotation<A>> highAggregateIndexesFirst() {
        return Comparator.<MergedAnnotation<A>>comparingInt(MergedAnnotation::getAggregateIndex).reversed();
    }

    private static AnnotationAttributes getAnnotationAttributes(MergedAnnotation<?> annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
        if (!annotation.isPresent()) {
            return null;
        }
        return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, nestedAnnotationsAsMap));
    }

    /**
     * 包装后的{@link AnnotatedElement}包含特定注解
     */
    private static class AnnotatedElementForAnnotations implements AnnotatedElement {

        private final Annotation[] annotations;

        AnnotatedElementForAnnotations(Annotation... annotations) {
            this.annotations = annotations;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (Annotation annotation : this.annotations) {
                if (annotation.annotationType() == annotationClass) {
                    return (T) annotation;
                }
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return this.annotations.clone();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return this.annotations.clone();
        }
    }
}
