package org.clever.core.annotation;

import org.clever.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 提供访问MergedAnnotation集合的方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:20 <br/>
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {
    /**
     * 注解是否存在存在
     */
    <A extends Annotation> boolean isPresent(Class<A> annotationType);

    /**
     * 注解是否存在存在
     */
    boolean isPresent(String annotationType);

    /**
     * 注解是否被直接标注存在
     */
    <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

    /**
     * 注解是否被直接标注存在
     */
    boolean isDirectlyPresent(String annotationType);

    /**
     * 获取指定类型的最近匹配注解或元注解
     *
     * @param annotationType 注解类型
     */
    <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

    /**
     * 获取指定类型的最近匹配注解或元注解
     *
     * @param annotationType 注解类型
     * @param predicate      用于自定义过滤注解
     * @see MergedAnnotationPredicates
     */
    <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType, Predicate<? super MergedAnnotation<A>> predicate);

    /**
     * 获取指定类型的匹配注解或元注解
     *
     * @param annotationType 注解类型
     * @param predicate      用于自定义过滤注解
     * @param selector       自定义注解选择器
     * @see MergedAnnotationPredicates
     * @see MergedAnnotationSelectors
     */
    <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType, Predicate<? super MergedAnnotation<A>> predicate, MergedAnnotationSelector<A> selector);

    /**
     * 获取指定类型的最近匹配注解或元注解
     *
     * @param annotationType 注解类型名
     */
    <A extends Annotation> MergedAnnotation<A> get(String annotationType);

    /**
     * 获取指定类型的最近匹配注解或元注解
     *
     * @param annotationType 注解类型名
     * @param predicate      用于自定义过滤注解
     * @see MergedAnnotationPredicates
     */
    <A extends Annotation> MergedAnnotation<A> get(String annotationType, Predicate<? super MergedAnnotation<A>> predicate);

    /**
     * 获取指定类型的最近匹配注解或元注解
     *
     * @param annotationType 注解类型名
     * @param predicate      用于自定义过滤注解
     * @param selector       自定义注解选择器
     * @see MergedAnnotationPredicates
     * @see MergedAnnotationSelectors
     */
    <A extends Annotation> MergedAnnotation<A> get(String annotationType, Predicate<? super MergedAnnotation<A>> predicate, MergedAnnotationSelector<A> selector);

    /**
     * 流式传输与指定类型匹配的所有注解和元注解。
     *
     * @param annotationType 注解类型
     */
    <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

    /**
     * 流式传输与指定类型匹配的所有注解和元注解
     *
     * @param annotationType 注解类型名
     */
    <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

    /**
     * 流式传输此集合中包含的所有注解和元注解
     */
    Stream<MergedAnnotation<Annotation>> stream();

    /**
     * 创建一个新的{@link MergedAnnotations}实例，使用{@link SearchStrategy#DIRECT}策略
     */
    static MergedAnnotations from(AnnotatedElement element) {
        return from(element, SearchStrategy.DIRECT);
    }

    /**
     * 创建一个新的{@link MergedAnnotations}实例
     *
     * @param element        注解源
     * @param searchStrategy 搜索策略
     */
    static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
        return from(element, searchStrategy, RepeatableContainers.standardRepeatable());
    }

    /**
     * 创建一个新的{@link MergedAnnotations}实例
     *
     * @param element              注解源
     * @param searchStrategy       搜索策略
     * @param repeatableContainers 重复标注多个注解的支持对象
     */
    static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy, RepeatableContainers repeatableContainers) {
        return from(element, searchStrategy, repeatableContainers, AnnotationFilter.PLAIN);
    }

    /**
     * 创建一个新的{@link MergedAnnotations}实例
     *
     * @param element              注解源
     * @param searchStrategy       搜索策略
     * @param repeatableContainers 重复标注多个注解的支持对象
     * @param annotationFilter     注解过滤器
     */
    static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy, RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
        Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
        Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
        return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
    }

    /**
     * 从指定的注解创建新的{@link MergedAnnotations}实例
     *
     * @see #from(Object, Annotation...)
     */
    static MergedAnnotations from(Annotation... annotations) {
        return from(annotations, annotations);
    }

    /**
     * 从指定的注解创建新的{@link MergedAnnotations}实例
     *
     * @param source      注解源
     * @param annotations 包含的注解类型
     * @see #from(Annotation...)
     * @see #from(AnnotatedElement)
     */
    static MergedAnnotations from(Object source, Annotation... annotations) {
        return from(source, annotations, RepeatableContainers.standardRepeatable());
    }

    /**
     * 从指定的注解创建新的{@link MergedAnnotations}实例
     *
     * @param source               注解源
     * @param annotations          包含的注解类型
     * @param repeatableContainers 重复标注多个注解的支持对象
     */
    static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers) {
        return from(source, annotations, repeatableContainers, AnnotationFilter.PLAIN);
    }

    /**
     * 从指定的注解创建新的{@link MergedAnnotations}实例
     *
     * @param source               注解源
     * @param annotations          包含的注解类型
     * @param repeatableContainers 重复标注多个注解的支持对象
     * @param annotationFilter     注解过滤器
     */
    static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
        Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
        Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
        return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
    }

    /**
     * 从指定的直接呈现注解集合创建新的{@link MergedAnnotations}实例<br/>
     * 此方法允许从不一定使用反射加载的注解创建MergedAnnotations实例。提供的注解必须全部直接存在，且聚合索引必须为0<br/>
     * 生成的MergedAnnotations实例将包含指定的注解和任何可以使用反射读取的元注解
     *
     * @param annotations 包含的注解类型
     * @see MergedAnnotation#of(ClassLoader, Object, Class, java.util.Map)
     */
    static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
        return MergedAnnotationsCollection.of(annotations);
    }

    /**
     * 注解搜索策略
     */
    enum SearchStrategy {
        /**
         * 只查找直接声明的注解，不考虑{@link Inherited @Inherited}注解，也不搜索超类或实现的接口
         */
        DIRECT,
        /**
         * 查找所有直接声明的注解以及任何{@link Inherited @Inherited}超类注解<br/>
         * 这种策略只有在与类类型一起使用时才真正有用，因为所有其他注解元素都会忽略{@link Inherited @Inherited}注解<br/>
         * 此策略不会搜索已实现的接口
         */
        INHERITED_ANNOTATIONS,
        /**
         * 查找所有直接声明的注解和超类注解<br/>
         * 这种策略与继承的注解类似，只是注解不需要用{@link Inherited @Inherited}进行元注解<br/>
         * 此策略不会搜索已实现的接口
         */
        SUPERCLASS,
        /**
         * 对整个类型层次结构执行完整搜索，包括超类和实现的接口<br/>
         * 超类注解不需要用{@link Inherited @Inherited}进行元注解
         */
        TYPE_HIERARCHY,
        /**
         * 对源和任何封闭类执行整个类型层次结构的完整搜索<br/>
         * 该策略与{@link #TYPE_HIERARCHY}类似，只是也会搜索封闭类<br/>
         * 超类和封闭类注解不需要使用{@link Inherited @Inherited}进行元注解<br/>
         * 搜索方法源时，此策略与{@link #TYPE_HIERARCHY}相同<br/>
         * 警告：无论源类型是内部类、静态嵌套类还是嵌套接口，此策略都会在封闭类上递归搜索任何源类型的注解。因此，它可能会找到比预期更多的注解。
         */
        TYPE_HIERARCHY_AND_ENCLOSING_CLASSES
    }
}
