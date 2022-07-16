package org.clever.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 用于封装来源于多个不同的注解的属性，表示一个注解视图
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:22 <br/>
 */
public interface MergedAnnotation<A extends Annotation> {
    /**
     * 带有单个属性的注解的属性名称
     */
    String VALUE = "value";

    /**
     * 获取实际注解类型
     */
    Class<A> getType();

    /**
     * 注解是否存在存在
     */
    boolean isPresent();

    /**
     * 注解是否被直接标注存在
     */
    boolean isDirectlyPresent();

    /**
     * 注解是否被组合注解标注存在
     */
    boolean isMetaPresent();

    /**
     * 获取与此注解用作元注解相关的距离<br/>
     * 被直接标注存在的距离为0，
     * 被元注解标注的距离为1，
     * 被元注解的元注解标注的距离为2，...
     * 未被标注返回-1
     */
    int getDistance();

    /**
     * 获取包含此注解的聚合集合的索引
     */
    int getAggregateIndex();

    /**
     * 获取最终声明根注解的源，如果源未知，则获取null
     */
    Object getSource();

    /**
     * 获取元注解的源，如果注解不存在，则为null<br/>
     * 元源是使用此注解进行元注解的注解
     *
     * @see #getRoot()
     */
    MergedAnnotation<?> getMetaSource();

    /**
     * 获取根注解，即直接在源上声明的距离为0的注解
     *
     * @see #getMetaSource()
     */
    MergedAnnotation<?> getRoot();

    /**
     * 从该注解到根，获取注解层次结构中注解类型的完整列表<br/>
     * 提供了唯一标识合并注解实例的有用方法
     *
     * @see MergedAnnotationPredicates#unique(Function)
     * @see #getRoot()
     * @see #getMetaSource()
     */
    List<Class<? extends Annotation>> getMetaTypes();

    /**
     * 判断注解的属性值是否没有默认值
     */
    boolean hasNonDefaultValue(String attributeName);

    /**
     * 判断注解的属性值是否存在默认值
     */
    boolean hasDefaultValue(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(byte类型)
     */
    byte getByte(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(byte[]类型)
     */
    byte[] getByteArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(boolean类型)
     */
    boolean getBoolean(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(boolean[]类型)
     */
    boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(char类型)
     */
    char getChar(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(char[]类型)
     */
    char[] getCharArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(short类型)
     */
    short getShort(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(short[]类型)
     */
    short[] getShortArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(int类型)
     */
    int getInt(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(int[]类型)
     */
    int[] getIntArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(long类型)
     */
    long getLong(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(long[]类型)
     */
    long[] getLongArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(double类型)
     */
    double getDouble(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(double[]类型)
     */
    double[] getDoubleArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(float类型)
     */
    float getFloat(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(float[]类型)
     */
    float[] getFloatArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(String类型)
     */
    String getString(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(String[]类型)
     */
    String[] getStringArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(Class类型)
     */
    Class<?> getClass(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(Class[]类型)
     */
    Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

    /**
     * 获取注解的属性值(Enum类型)
     */
    <E extends Enum<E>> E getEnum(String attributeName, Class<E> type) throws NoSuchElementException;

    /**
     * 获取注解的属性值(Enum[]类型)
     */
    <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) throws NoSuchElementException;

    /**
     * 获取注解的属性值(MergedAnnotation类型)
     */
    <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type) throws NoSuchElementException;

    /**
     * 获取注解的属性值(MergedAnnotation[]类型)
     */
    <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type) throws NoSuchElementException;

    /**
     * 获取注解的属性值(Optional类型)
     */
    Optional<Object> getValue(String attributeName);

    /**
     * 获取注解的属性值(Optional类型)
     */
    <T> Optional<T> getValue(String attributeName, Class<T> type);

    /**
     * 获取注解的属性默认值(Optional类型)
     */
    Optional<Object> getDefaultValue(String attributeName);

    /**
     * 获取注解的属性默认值(Optional类型)
     */
    <T> Optional<T> getDefaultValue(String attributeName, Class<T> type);

    /**
     * 创建注解的新视图，删除了默认值的所有属性
     *
     * @see #filterAttributes(Predicate)
     */
    MergedAnnotation<A> filterDefaultValues();

    /**
     * 创建注解的新视图，根据注解属性名称自定义过滤
     *
     * @see #filterDefaultValues()
     * @see MergedAnnotationPredicates
     */
    MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

    /**
     * 创建注解的新视图，以显示未合并的属性值
     */
    MergedAnnotation<A> withNonMergedAttributes();

    /**
     * 获取AnnotationAttributes(注解属性值)对象
     */
    AnnotationAttributes asAnnotationAttributes(Adapt... adaptations);

    /**
     * 获取注解属性值的不可变{@code Map<属性名, 属性值>}
     */
    Map<String, Object> asMap(Adapt... adaptations);

    /**
     * 获取一个包含所有注解属性的{@code Map<属性名, 属性值>}
     */
    <T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations);

    /**
     * 创建此合并注解的类型安全合成版本，可直接在代码中使<br/>
     * 结果是使用JDK代理合成的，因此在第一次调用时可能会产生计算开销
     */
    A synthesize() throws NoSuchElementException;

    /**
     * 创建此合并注解的类型安全合成版本，可直接在代码中使<br/>
     * 结果是使用JDK代理合成的，因此在第一次调用时可能会产生计算开销
     *
     * @param condition 用于测试注解是否可以合成
     * @see MergedAnnotationPredicates
     */
    Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException;

    /**
     * 创建表示缺失注解(即不存在的注解)的合并注解
     */
    static <A extends Annotation> MergedAnnotation<A> missing() {
        return MissingMergedAnnotation.getInstance();
    }

    /**
     * 根据注解类型创建新的{@link MergedAnnotation}实例
     */
    static <A extends Annotation> MergedAnnotation<A> from(A annotation) {
        return from(null, annotation);
    }

    /**
     * 创建新的{@link MergedAnnotation}实例
     */
    static <A extends Annotation> MergedAnnotation<A> from(Object source, A annotation) {
        return TypeMappedAnnotation.from(source, annotation);
    }

    /**
     * 创建新的{@link MergedAnnotation}实例<br/>
     * 生成的注解将没有任何属性值，但仍可用于查询默认值
     */
    static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType) {
        return of(null, annotationType, null);
    }

    /**
     * 使用Map提供的属性值和注解类型创建新的{@link MergedAnnotation}实例
     *
     * @see #of(AnnotatedElement, Class, Map)
     */
    static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType, Map<String, ?> attributes) {
        return of(null, annotationType, attributes);
    }

    /**
     * 使用Map提供的属性值和注解类型创建新的{@link MergedAnnotation}实例
     */
    static <A extends Annotation> MergedAnnotation<A> of(AnnotatedElement source, Class<A> annotationType, Map<String, ?> attributes) {
        return of(null, source, annotationType, attributes);
    }

    /**
     * 使用Map提供的属性值和注解类型创建新的{@link MergedAnnotation}实例
     */
    static <A extends Annotation> MergedAnnotation<A> of(ClassLoader classLoader, Object source, Class<A> annotationType, Map<String, ?> attributes) {
        return TypeMappedAnnotation.of(classLoader, source, annotationType, attributes);
    }

    /**
     * 注解属性值转换方式
     */
    enum Adapt {
        /**
         * class和class数组属性转换成字符串
         */
        CLASS_TO_STRING,
        /**
         * 嵌套的annotation和annotation数组转换为Map
         * 使嵌套注释或注释数组适应贴图，而不是合成值
         */
        ANNOTATION_TO_MAP;

        protected final boolean isIn(Adapt... adaptations) {
            for (Adapt candidate : adaptations) {
                if (candidate == this) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 工厂方法用来创建 Adapt[]
         *
         * @param classToString    是否包含 {@link Adapt#CLASS_TO_STRING}
         * @param annotationsToMap 是否包含 {@link Adapt#ANNOTATION_TO_MAP}
         */
        public static Adapt[] values(boolean classToString, boolean annotationsToMap) {
            EnumSet<Adapt> result = EnumSet.noneOf(Adapt.class);
            addIfTrue(result, Adapt.CLASS_TO_STRING, classToString);
            addIfTrue(result, Adapt.ANNOTATION_TO_MAP, annotationsToMap);
            return result.toArray(new Adapt[0]);
        }

        private static <T> void addIfTrue(Set<T> result, T value, boolean test) {
            if (test) {
                result.add(value);
            }
        }
    }
}
