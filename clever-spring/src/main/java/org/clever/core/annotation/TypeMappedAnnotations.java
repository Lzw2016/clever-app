package org.clever.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link MergedAnnotations} 的实现类，使用了{@link AnnotationTypeMappings}实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 14:53 <br/>
 */
final class TypeMappedAnnotations implements MergedAnnotations {
    /**
     * 没有注解时可以使用的共享实例
     */
    static final MergedAnnotations NONE = new TypeMappedAnnotations(null, new Annotation[0], RepeatableContainers.none(), AnnotationFilter.ALL);
    /**
     * 源对象
     */
    private final Object source;
    /**
     * 可注解元素实例
     */
    private final AnnotatedElement element;
    /**
     * 查找策略
     */
    private final SearchStrategy searchStrategy;
    /**
     * 注解
     */
    private final Annotation[] annotations;
    /**
     * 支持重复标注多个注解的对象
     */
    private final RepeatableContainers repeatableContainers;
    /**
     * 注解过滤
     */
    private final AnnotationFilter annotationFilter;
    private volatile List<Aggregate> aggregates;

    private TypeMappedAnnotations(AnnotatedElement element,
                                  SearchStrategy searchStrategy,
                                  RepeatableContainers repeatableContainers,
                                  AnnotationFilter annotationFilter) {
        this.source = element;
        this.element = element;
        this.searchStrategy = searchStrategy;
        this.annotations = null;
        this.repeatableContainers = repeatableContainers;
        this.annotationFilter = annotationFilter;
    }

    private TypeMappedAnnotations(Object source,
                                  Annotation[] annotations,
                                  RepeatableContainers repeatableContainers,
                                  AnnotationFilter annotationFilter) {
        this.source = source;
        this.element = null;
        this.searchStrategy = null;
        this.annotations = annotations;
        this.repeatableContainers = repeatableContainers;
        this.annotationFilter = annotationFilter;
    }

    @Override
    public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
        if (this.annotationFilter.matches(annotationType)) {
            return false;
        }
        return Boolean.TRUE.equals(scan(annotationType, IsPresent.get(this.repeatableContainers, this.annotationFilter, false)));
    }

    @Override
    public boolean isPresent(String annotationType) {
        if (this.annotationFilter.matches(annotationType)) {
            return false;
        }
        return Boolean.TRUE.equals(scan(annotationType, IsPresent.get(this.repeatableContainers, this.annotationFilter, false)));
    }

    @Override
    public <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType) {
        if (this.annotationFilter.matches(annotationType)) {
            return false;
        }
        return Boolean.TRUE.equals(scan(annotationType, IsPresent.get(this.repeatableContainers, this.annotationFilter, true)));
    }

    @Override
    public boolean isDirectlyPresent(String annotationType) {
        if (this.annotationFilter.matches(annotationType)) {
            return false;
        }
        return Boolean.TRUE.equals(scan(annotationType, IsPresent.get(this.repeatableContainers, this.annotationFilter, true)));
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType) {
        return get(annotationType, null, null);
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType, Predicate<? super MergedAnnotation<A>> predicate) {
        return get(annotationType, predicate, null);
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType, 
                                                          Predicate<? super MergedAnnotation<A>> predicate, 
                                                          MergedAnnotationSelector<A> selector) {
        if (this.annotationFilter.matches(annotationType)) {
            return MergedAnnotation.missing();
        }
        MergedAnnotation<A> result = scan(annotationType, new MergedAnnotationFinder<>(annotationType, predicate, selector));
        return (result != null ? result : MergedAnnotation.missing());
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
        return get(annotationType, null, null);
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> get(String annotationType, Predicate<? super MergedAnnotation<A>> predicate) {
        return get(annotationType, predicate, null);
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> get(String annotationType, 
                                                          Predicate<? super MergedAnnotation<A>> predicate, 
                                                          MergedAnnotationSelector<A> selector) {
        if (this.annotationFilter.matches(annotationType)) {
            return MergedAnnotation.missing();
        }
        MergedAnnotation<A> result = scan(annotationType, new MergedAnnotationFinder<>(annotationType, predicate, selector));
        return (result != null ? result : MergedAnnotation.missing());
    }

    @Override
    public <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType) {
        if (this.annotationFilter == AnnotationFilter.ALL) {
            return Stream.empty();
        }
        return StreamSupport.stream(spliterator(annotationType), false);
    }

    @Override
    public <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType) {
        if (this.annotationFilter == AnnotationFilter.ALL) {
            return Stream.empty();
        }
        return StreamSupport.stream(spliterator(annotationType), false);
    }

    @Override
    public Stream<MergedAnnotation<Annotation>> stream() {
        if (this.annotationFilter == AnnotationFilter.ALL) {
            return Stream.empty();
        }
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Iterator<MergedAnnotation<Annotation>> iterator() {
        if (this.annotationFilter == AnnotationFilter.ALL) {
            return Collections.emptyIterator();
        }
        return Spliterators.iterator(spliterator());
    }

    @Override
    public Spliterator<MergedAnnotation<Annotation>> spliterator() {
        if (this.annotationFilter == AnnotationFilter.ALL) {
            return Spliterators.emptySpliterator();
        }
        return spliterator(null);
    }

    private <A extends Annotation> Spliterator<MergedAnnotation<A>> spliterator(Object annotationType) {
        return new AggregatesSpliterator<>(annotationType, getAggregates());
    }

    private List<Aggregate> getAggregates() {
        List<Aggregate> aggregates = this.aggregates;
        if (aggregates == null) {
            aggregates = scan(this, new AggregatesCollector());
            if (aggregates == null || aggregates.isEmpty()) {
                aggregates = Collections.emptyList();
            }
            this.aggregates = aggregates;
        }
        return aggregates;
    }

    private <C, R> R scan(C criteria, AnnotationsProcessor<C, R> processor) {
        if (this.annotations != null) {
            R result = processor.doWithAnnotations(criteria, 0, this.source, this.annotations);
            return processor.finish(result);
        }
        if (this.element != null && this.searchStrategy != null) {
            return AnnotationsScanner.scan(criteria, this.element, this.searchStrategy, processor);
        }
        return null;
    }

    static MergedAnnotations from(AnnotatedElement element, 
                                  SearchStrategy searchStrategy, 
                                  RepeatableContainers repeatableContainers, 
                                  AnnotationFilter annotationFilter) {
        if (AnnotationsScanner.isKnownEmpty(element, searchStrategy)) {
            return NONE;
        }
        return new TypeMappedAnnotations(element, searchStrategy, repeatableContainers, annotationFilter);
    }

    static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

        if (annotations.length == 0) {
            return NONE;
        }
        return new TypeMappedAnnotations(source, annotations, repeatableContainers, annotationFilter);
    }

    private static boolean isMappingForType(AnnotationTypeMapping mapping, AnnotationFilter annotationFilter, Object requiredType) {

        Class<? extends Annotation> actualType = mapping.getAnnotationType();
        return (!annotationFilter.matches(actualType) && (requiredType == null || actualType == requiredType || actualType.getName().equals(requiredType)));
    }

    /**
     * {@link AnnotationsProcessor}(注解处理器)，用于检测注解是"直接标注存在"还是"元标注存在"
     */
    private static final class IsPresent implements AnnotationsProcessor<Object, Boolean> {
        /**
         * 共享实例，使我们无需为常见组合创建新处理器
         */
        private static final IsPresent[] SHARED;

        static {
            SHARED = new IsPresent[4];
            SHARED[0] = new IsPresent(RepeatableContainers.none(), AnnotationFilter.PLAIN, true);
            SHARED[1] = new IsPresent(RepeatableContainers.none(), AnnotationFilter.PLAIN, false);
            SHARED[2] = new IsPresent(RepeatableContainers.standardRepeatable(), AnnotationFilter.PLAIN, true);
            SHARED[3] = new IsPresent(RepeatableContainers.standardRepeatable(), AnnotationFilter.PLAIN, false);
        }

        private final RepeatableContainers repeatableContainers;
        private final AnnotationFilter annotationFilter;
        private final boolean directOnly;

        private IsPresent(RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter, boolean directOnly) {
            this.repeatableContainers = repeatableContainers;
            this.annotationFilter = annotationFilter;
            this.directOnly = directOnly;
        }

        @Override
        public Boolean doWithAnnotations(Object requiredType, int aggregateIndex, Object source, Annotation[] annotations) {
            for (Annotation annotation : annotations) {
                if (annotation != null) {
                    Class<? extends Annotation> type = annotation.annotationType();
                    if (type != null && !this.annotationFilter.matches(type)) {
                        if (type == requiredType || type.getName().equals(requiredType)) {
                            return Boolean.TRUE;
                        }
                        Annotation[] repeatedAnnotations = this.repeatableContainers.findRepeatedAnnotations(annotation);
                        if (repeatedAnnotations != null) {
                            Boolean result = doWithAnnotations(requiredType, aggregateIndex, source, repeatedAnnotations);
                            if (result != null) {
                                return result;
                            }
                        }
                        if (!this.directOnly) {
                            AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(type);
                            for (int i = 0; i < mappings.size(); i++) {
                                AnnotationTypeMapping mapping = mappings.get(i);
                                if (isMappingForType(mapping, this.annotationFilter, requiredType)) {
                                    return Boolean.TRUE;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        static IsPresent get(RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter, boolean directOnly) {
            // Use a single shared instance for common combinations
            if (annotationFilter == AnnotationFilter.PLAIN) {
                if (repeatableContainers == RepeatableContainers.none()) {
                    return SHARED[directOnly ? 0 : 1];
                }
                if (repeatableContainers == RepeatableContainers.standardRepeatable()) {
                    return SHARED[directOnly ? 2 : 3];
                }
            }
            return new IsPresent(repeatableContainers, annotationFilter, directOnly);
        }
    }

    /**
     * {@link AnnotationsProcessor}(注解处理器)，查找单个 {@link MergedAnnotation}
     */
    private class MergedAnnotationFinder<A extends Annotation> implements AnnotationsProcessor<Object, MergedAnnotation<A>> {
        private final Object requiredType;
        private final Predicate<? super MergedAnnotation<A>> predicate;
        private final MergedAnnotationSelector<A> selector;
        private MergedAnnotation<A> result;

        MergedAnnotationFinder(Object requiredType, Predicate<? super MergedAnnotation<A>> predicate, MergedAnnotationSelector<A> selector) {
            this.requiredType = requiredType;
            this.predicate = predicate;
            this.selector = (selector != null ? selector : MergedAnnotationSelectors.nearest());
        }

        @Override
        public MergedAnnotation<A> doWithAggregate(Object context, int aggregateIndex) {
            return this.result;
        }

        @Override
        public MergedAnnotation<A> doWithAnnotations(Object type, int aggregateIndex, Object source, Annotation[] annotations) {
            for (Annotation annotation : annotations) {
                if (annotation != null && !annotationFilter.matches(annotation)) {
                    MergedAnnotation<A> result = process(type, aggregateIndex, source, annotation);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }

        private MergedAnnotation<A> process(Object type, int aggregateIndex, Object source, Annotation annotation) {
            Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(annotation);
            if (repeatedAnnotations != null) {
                return doWithAnnotations(type, aggregateIndex, source, repeatedAnnotations);
            }
            AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(annotation.annotationType(), repeatableContainers, annotationFilter);
            for (int i = 0; i < mappings.size(); i++) {
                AnnotationTypeMapping mapping = mappings.get(i);
                if (isMappingForType(mapping, annotationFilter, this.requiredType)) {
                    MergedAnnotation<A> candidate = TypeMappedAnnotation.createIfPossible(mapping, source, annotation, aggregateIndex, IntrospectionFailureLogger.INFO);
                    if (candidate != null && (this.predicate == null || this.predicate.test(candidate))) {
                        if (this.selector.isBestCandidate(candidate)) {
                            return candidate;
                        }
                        updateLastResult(candidate);
                    }
                }
            }
            return null;
        }

        private void updateLastResult(MergedAnnotation<A> candidate) {
            MergedAnnotation<A> lastResult = this.result;
            this.result = (lastResult != null ? this.selector.select(lastResult, candidate) : candidate);
        }

        @Override
        public MergedAnnotation<A> finish(MergedAnnotation<A> result) {
            return (result != null ? result : this.result);
        }
    }

    /**
     * {@link AnnotationsProcessor}(注解处理器)，收集{@link Aggregate}实例
     */
    private class AggregatesCollector implements AnnotationsProcessor<Object, List<Aggregate>> {
        private final List<Aggregate> aggregates = new ArrayList<>();

        @Override
        public List<Aggregate> doWithAnnotations(Object criteria, int aggregateIndex, Object source, Annotation[] annotations) {
            this.aggregates.add(createAggregate(aggregateIndex, source, annotations));
            return null;
        }

        private Aggregate createAggregate(int aggregateIndex, Object source, Annotation[] annotations) {
            List<Annotation> aggregateAnnotations = getAggregateAnnotations(annotations);
            return new Aggregate(aggregateIndex, source, aggregateAnnotations);
        }

        private List<Annotation> getAggregateAnnotations(Annotation[] annotations) {
            List<Annotation> result = new ArrayList<>(annotations.length);
            addAggregateAnnotations(result, annotations);
            return result;
        }

        private void addAggregateAnnotations(List<Annotation> aggregateAnnotations, Annotation[] annotations) {
            for (Annotation annotation : annotations) {
                if (annotation != null && !annotationFilter.matches(annotation)) {
                    Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(annotation);
                    if (repeatedAnnotations != null) {
                        addAggregateAnnotations(aggregateAnnotations, repeatedAnnotations);
                    } else {
                        aggregateAnnotations.add(annotation);
                    }
                }
            }
        }

        @Override
        public List<Aggregate> finish(List<Aggregate> processResult) {
            return this.aggregates;
        }
    }

    private static class Aggregate {
        private final int aggregateIndex;
        private final Object source;
        private final List<Annotation> annotations;
        private final AnnotationTypeMappings[] mappings;

        Aggregate(int aggregateIndex, Object source, List<Annotation> annotations) {
            this.aggregateIndex = aggregateIndex;
            this.source = source;
            this.annotations = annotations;
            this.mappings = new AnnotationTypeMappings[annotations.size()];
            for (int i = 0; i < annotations.size(); i++) {
                this.mappings[i] = AnnotationTypeMappings.forAnnotationType(annotations.get(i).annotationType());
            }
        }

        int size() {
            return this.annotations.size();
        }

        AnnotationTypeMapping getMapping(int annotationIndex, int mappingIndex) {
            AnnotationTypeMappings mappings = getMappings(annotationIndex);
            return (mappingIndex < mappings.size() ? mappings.get(mappingIndex) : null);
        }

        AnnotationTypeMappings getMappings(int annotationIndex) {
            return this.mappings[annotationIndex];
        }

        <A extends Annotation> MergedAnnotation<A> createMergedAnnotationIfPossible(int annotationIndex, int mappingIndex, IntrospectionFailureLogger logger) {
            return TypeMappedAnnotation.createIfPossible(this.mappings[annotationIndex].get(mappingIndex), this.source, this.annotations.get(annotationIndex), this.aggregateIndex, logger);
        }
    }

    /**
     * {@link Spliterator} 用于按距离优先顺序使用聚合中的合并注解
     */
    private class AggregatesSpliterator<A extends Annotation> implements Spliterator<MergedAnnotation<A>> {
        private final Object requiredType;
        private final List<Aggregate> aggregates;
        private int aggregateCursor;
        private int[] mappingCursors;

        AggregatesSpliterator(Object requiredType, List<Aggregate> aggregates) {
            this.requiredType = requiredType;
            this.aggregates = aggregates;
            this.aggregateCursor = 0;
        }

        @Override
        public boolean tryAdvance(Consumer<? super MergedAnnotation<A>> action) {
            while (this.aggregateCursor < this.aggregates.size()) {
                Aggregate aggregate = this.aggregates.get(this.aggregateCursor);
                if (tryAdvance(aggregate, action)) {
                    return true;
                }
                this.aggregateCursor++;
                this.mappingCursors = null;
            }
            return false;
        }

        private boolean tryAdvance(Aggregate aggregate, Consumer<? super MergedAnnotation<A>> action) {
            if (this.mappingCursors == null) {
                this.mappingCursors = new int[aggregate.size()];
            }
            int lowestDistance = Integer.MAX_VALUE;
            int annotationResult = -1;
            for (int annotationIndex = 0; annotationIndex < aggregate.size(); annotationIndex++) {
                AnnotationTypeMapping mapping = getNextSuitableMapping(aggregate, annotationIndex);
                if (mapping != null && mapping.getDistance() < lowestDistance) {
                    annotationResult = annotationIndex;
                    lowestDistance = mapping.getDistance();
                }
                if (lowestDistance == 0) {
                    break;
                }
            }
            if (annotationResult != -1) {
                MergedAnnotation<A> mergedAnnotation = aggregate.createMergedAnnotationIfPossible(
                        annotationResult,
                        this.mappingCursors[annotationResult],
                        this.requiredType != null ? IntrospectionFailureLogger.INFO : IntrospectionFailureLogger.DEBUG
                );
                this.mappingCursors[annotationResult]++;
                if (mergedAnnotation == null) {
                    return tryAdvance(aggregate, action);
                }
                action.accept(mergedAnnotation);
                return true;
            }
            return false;
        }

        private AnnotationTypeMapping getNextSuitableMapping(Aggregate aggregate, int annotationIndex) {
            int[] cursors = this.mappingCursors;
            if (cursors != null) {
                AnnotationTypeMapping mapping;
                do {
                    mapping = aggregate.getMapping(annotationIndex, cursors[annotationIndex]);
                    if (mapping != null && isMappingForType(mapping, annotationFilter, this.requiredType)) {
                        return mapping;
                    }
                    cursors[annotationIndex]++;
                } while (mapping != null);
            }
            return null;
        }

        @Override
        public Spliterator<MergedAnnotation<A>> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            int size = 0;
            for (int aggregateIndex = this.aggregateCursor; aggregateIndex < this.aggregates.size(); aggregateIndex++) {
                Aggregate aggregate = this.aggregates.get(aggregateIndex);
                for (int annotationIndex = 0; annotationIndex < aggregate.size(); annotationIndex++) {
                    AnnotationTypeMappings mappings = aggregate.getMappings(annotationIndex);
                    int numberOfMappings = mappings.size();
                    if (aggregateIndex == this.aggregateCursor && this.mappingCursors != null) {
                        numberOfMappings -= Math.min(this.mappingCursors[annotationIndex], mappings.size());
                    }
                    size += numberOfMappings;
                }
            }
            return size;
        }

        @Override
        public int characteristics() {
            return NONNULL | IMMUTABLE;
        }
    }
}
