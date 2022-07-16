package org.clever.core.annotation;

import org.clever.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 提供单个源注释类型的 {@link AnnotationTypeMapping} 信息。
 * 对所有元注释执行递归的宽度优先爬网，以最终提供映射根 {@link Annotation} 属性的快速方法。
 *
 * <p>
 * 支持基于约定的元注释合并以及隐式和显式 {@link AliasFor @AliasFor} 别名。还提供了有关镜像属性的信息。
 *
 * <p>该类被设计为缓存，以便只需要搜索一次元注释，而不管它们实际使用了多少次。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:47 <br/>
 */
final class AnnotationTypeMappings {
    private static final IntrospectionFailureLogger failureLogger = IntrospectionFailureLogger.DEBUG;
    private static final Map<AnnotationFilter, Cache> standardRepeatablesCache = new ConcurrentReferenceHashMap<>();
    private static final Map<AnnotationFilter, Cache> noRepeatablesCache = new ConcurrentReferenceHashMap<>();

    private final RepeatableContainers repeatableContainers;
    private final AnnotationFilter filter;
    private final List<AnnotationTypeMapping> mappings;

    private AnnotationTypeMappings(RepeatableContainers repeatableContainers, AnnotationFilter filter, Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        this.repeatableContainers = repeatableContainers;
        this.filter = filter;
        this.mappings = new ArrayList<>();
        addAllMappings(annotationType, visitedAnnotationTypes);
        this.mappings.forEach(AnnotationTypeMapping::afterAllMappingsSet);
    }

    private void addAllMappings(Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        Deque<AnnotationTypeMapping> queue = new ArrayDeque<>();
        addIfPossible(queue, null, annotationType, null, visitedAnnotationTypes);
        while (!queue.isEmpty()) {
            AnnotationTypeMapping mapping = queue.removeFirst();
            this.mappings.add(mapping);
            addMetaAnnotationsToQueue(queue, mapping);
        }
    }

    private void addMetaAnnotationsToQueue(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping source) {
        Annotation[] metaAnnotations = AnnotationsScanner.getDeclaredAnnotations(source.getAnnotationType(), false);
        for (Annotation metaAnnotation : metaAnnotations) {
            if (!isMappable(source, metaAnnotation)) {
                continue;
            }
            Annotation[] repeatedAnnotations = this.repeatableContainers.findRepeatedAnnotations(metaAnnotation);
            if (repeatedAnnotations != null) {
                for (Annotation repeatedAnnotation : repeatedAnnotations) {
                    if (!isMappable(source, repeatedAnnotation)) {
                        continue;
                    }
                    addIfPossible(queue, source, repeatedAnnotation);
                }
            } else {
                addIfPossible(queue, source, metaAnnotation);
            }
        }
    }

    private void addIfPossible(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping source, Annotation ann) {
        addIfPossible(queue, source, ann.annotationType(), ann, new HashSet<>());
    }

    private void addIfPossible(Deque<AnnotationTypeMapping> queue,
                               AnnotationTypeMapping source,
                               Class<? extends Annotation> annotationType,
                               Annotation ann,
                               Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        try {
            queue.addLast(new AnnotationTypeMapping(source, annotationType, ann, visitedAnnotationTypes));
        } catch (Exception ex) {
            AnnotationUtils.rethrowAnnotationConfigurationException(ex);
            if (failureLogger.isEnabled()) {
                failureLogger.log("Failed to introspect meta-annotation " + annotationType.getName(), (source != null ? source.getAnnotationType() : null), ex);
            }
        }
    }

    private boolean isMappable(AnnotationTypeMapping source, Annotation metaAnnotation) {
        return (metaAnnotation != null
                && !this.filter.matches(metaAnnotation)
                && !AnnotationFilter.PLAIN.matches(source.getAnnotationType())
                && !isAlreadyMapped(source, metaAnnotation));
    }

    private boolean isAlreadyMapped(AnnotationTypeMapping source, Annotation metaAnnotation) {
        Class<? extends Annotation> annotationType = metaAnnotation.annotationType();
        AnnotationTypeMapping mapping = source;
        while (mapping != null) {
            if (mapping.getAnnotationType() == annotationType) {
                return true;
            }
            mapping = mapping.getSource();
        }
        return false;
    }

    /**
     * 获取包含的映射的总数。
     *
     * @return 映射的总数
     */
    int size() {
        return this.mappings.size();
    }

    /**
     * 从该实例获取单个映射。
     * <p>索引0将始终返回根映射；较高的索引将返回元注释映射。
     *
     * @param index 要返回的索引
     * @return {@link AnnotationTypeMapping}
     * @throws IndexOutOfBoundsException 如果索引超出范围 (index < 0 || index >= size())
     */
    AnnotationTypeMapping get(int index) {
        return this.mappings.get(index);
    }

    /**
     * 为指定的注释类型创建注释类型映射 {@link AnnotationTypeMappings}
     *
     * @param annotationType 源注释类型
     * @return 注释类型的类型映射
     */
    static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType) {
        return forAnnotationType(annotationType, new HashSet<>());
    }

    /**
     * 为指定的注释类型创建注释类型映射 {@link AnnotationTypeMappings}
     *
     * @param annotationType         源注释类型
     * @param visitedAnnotationTypes 我们已经访问过的注释集；用于避免某些JVM语言（如Kotlin）支持的递归注释的无限递归
     * @return 注释类型的类型映射
     */
    static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        return forAnnotationType(annotationType, RepeatableContainers.standardRepeatable(), AnnotationFilter.PLAIN, visitedAnnotationTypes);
    }

    /**
     * 为指定的注释类型创建注释类型映射 {@link AnnotationTypeMappings}
     *
     * @param annotationType       源注释类型
     * @param repeatableContainers 元注释可能使用的可重复容器
     * @param annotationFilter     用于限制考虑哪些注释的注释过滤器
     * @return 注释类型的类型映射
     */
    static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType,
                                                    RepeatableContainers repeatableContainers,
                                                    AnnotationFilter annotationFilter) {
        return forAnnotationType(annotationType, repeatableContainers, annotationFilter, new HashSet<>());
    }

    /**
     * 为指定的注释类型创建注释类型映射 {@link AnnotationTypeMappings}
     *
     * @param annotationType         源注释类型
     * @param repeatableContainers   元注释可能使用的可重复容器
     * @param annotationFilter       用于限制考虑哪些注释的注释过滤器
     * @param visitedAnnotationTypes 我们已经访问过的注释集；用于避免某些JVM语言（如Kotlin）支持的递归注释的无限递归
     * @return 注释类型的类型映射
     */
    private static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType,
                                                            RepeatableContainers repeatableContainers,
                                                            AnnotationFilter annotationFilter,
                                                            Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        if (repeatableContainers == RepeatableContainers.standardRepeatable()) {
            return standardRepeatablesCache.computeIfAbsent(annotationFilter, key -> new Cache(repeatableContainers, key)).get(annotationType, visitedAnnotationTypes);
        }
        if (repeatableContainers == RepeatableContainers.none()) {
            return noRepeatablesCache.computeIfAbsent(annotationFilter, key -> new Cache(repeatableContainers, key)).get(annotationType, visitedAnnotationTypes);
        }
        return new AnnotationTypeMappings(repeatableContainers, annotationFilter, annotationType, visitedAnnotationTypes);
    }

    static void clearCache() {
        standardRepeatablesCache.clear();
        noRepeatablesCache.clear();
    }

    /**
     * 根据创建的缓存 {@link AnnotationFilter}.
     */
    private static class Cache {
        private final RepeatableContainers repeatableContainers;
        private final AnnotationFilter filter;
        private final Map<Class<? extends Annotation>, AnnotationTypeMappings> mappings;

        /**
         * 使用指定的筛选器创建缓存实例。
         *
         * @param filter 注释过滤器
         */
        Cache(RepeatableContainers repeatableContainers, AnnotationFilter filter) {
            this.repeatableContainers = repeatableContainers;
            this.filter = filter;
            this.mappings = new ConcurrentReferenceHashMap<>();
        }

        /**
         * 获取或创建指定注释类型的 {@link AnnotationTypeMappings}
         *
         * @param annotationType         注释类型
         * @param visitedAnnotationTypes 我们已经访问过的注释集；用于避免某些JVM语言（如Kotlin）支持的递归注释的无限递归
         * @return a new or existing {@link AnnotationTypeMappings} instance
         */
        AnnotationTypeMappings get(Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
            return this.mappings.computeIfAbsent(annotationType, key -> createMappings(key, visitedAnnotationTypes));
        }

        private AnnotationTypeMappings createMappings(Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
            return new AnnotationTypeMappings(this.repeatableContainers, this.filter, annotationType, visitedAnnotationTypes);
        }
    }
}

