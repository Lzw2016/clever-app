package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.core.ResolvableType;
import org.clever.core.style.ToStringCreator;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

/**
 * 可由 {@link Binder} 绑定的源。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:04 <br/>
 *
 * @param <T> 源类型
 * @see Bindable#of(Class)
 * @see Bindable#of(ResolvableType)
 */
public final class Bindable<T> {
    private static final Annotation[] NO_ANNOTATIONS = {};
    private static final EnumSet<BindRestriction> NO_BIND_RESTRICTIONS = EnumSet.noneOf(BindRestriction.class);

    private final ResolvableType type;
    private final ResolvableType boxedType;
    private final Supplier<T> value;
    private final Annotation[] annotations;
    private final EnumSet<BindRestriction> bindRestrictions;

    private Bindable(ResolvableType type,
                     ResolvableType boxedType,
                     Supplier<T> value,
                     Annotation[] annotations,
                     EnumSet<BindRestriction> bindRestrictions) {
        this.type = type;
        this.boxedType = boxedType;
        this.value = value;
        this.annotations = annotations;
        this.bindRestrictions = bindRestrictions;
    }

    /**
     * 返回要绑定的项的类型。
     *
     * @return 正在绑定的类型
     */
    public ResolvableType getType() {
        return this.type;
    }

    /**
     * 返回要绑定的项目的装箱类型。
     *
     * @return 要绑定的项目的装箱类型
     */
    public ResolvableType getBoxedType() {
        return this.boxedType;
    }

    /**
     * 返回提供对象值的供应商或null
     *
     * @return 值或null
     */
    public Supplier<T> getValue() {
        return this.value;
    }

    /**
     * 返回任何可能影响绑定的关联注释
     *
     * @return 相关注释
     */
    public Annotation[] getAnnotations() {
        return this.annotations;
    }

    /**
     * 返回可能影响绑定的单个关联注释。
     *
     * @param <A>  注释类型
     * @param type 注解类型
     * @return 关联的注释或null
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        for (Annotation annotation : this.annotations) {
            if (type.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    /**
     * 如果已添加指定的绑定限制，则返回true。
     *
     * @param bindRestriction 要检查的绑定限制
     * @return 如果已添加绑定限制
     */
    public boolean hasBindRestriction(BindRestriction bindRestriction) {
        return this.bindRestrictions.contains(bindRestriction);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Bindable<?> other = (Bindable<?>) obj;
        boolean result;
        result = nullSafeEquals(this.type.resolve(), other.type.resolve());
        result = result && nullSafeEquals(this.annotations, other.annotations);
        result = result && nullSafeEquals(this.bindRestrictions, other.bindRestrictions);
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
        result = prime * result + ObjectUtils.nullSafeHashCode(this.annotations);
        result = prime * result + ObjectUtils.nullSafeHashCode(this.bindRestrictions);
        return result;
    }

    @Override
    public String toString() {
        ToStringCreator creator = new ToStringCreator(this);
        creator.append("type", this.type);
        creator.append("value", (this.value != null) ? "provided" : "none");
        creator.append("annotations", this.annotations);
        return creator.toString();
    }

    private boolean nullSafeEquals(Object o1, Object o2) {
        return ObjectUtils.nullSafeEquals(o1, o2);
    }

    /**
     * 使用指定的注释创建更新的 {@link Bindable} 实例
     *
     * @param annotations 注释
     * @return 更新的 {@link Bindable}
     */
    public Bindable<T> withAnnotations(Annotation... annotations) {
        return new Bindable<>(
                this.type,
                this.boxedType,
                this.value,
                (annotations != null) ? annotations : NO_ANNOTATIONS, NO_BIND_RESTRICTIONS
        );
    }

    /**
     * 使用现有值创建更新的 {@link Bindable} 实例。
     *
     * @param existingValue 现有价值
     * @return 更新的 {@link Bindable}
     */
    public Bindable<T> withExistingValue(T existingValue) {
        Assert.isTrue(
                existingValue == null || this.type.isArray() || this.boxedType.resolve().isInstance(existingValue),
                () -> "ExistingValue must be an instance of " + this.type
        );
        Supplier<T> value = (existingValue != null) ? () -> existingValue : null;
        return new Bindable<>(this.type, this.boxedType, value, this.annotations, this.bindRestrictions);
    }

    /**
     * 与价值供应商创建更新的 {@link Bindable} 实例。
     *
     * @param suppliedValue 价值的供应商
     * @return 更新的 {@link Bindable}
     */
    public Bindable<T> withSuppliedValue(Supplier<T> suppliedValue) {
        return new Bindable<>(this.type, this.boxedType, suppliedValue, this.annotations, this.bindRestrictions);
    }

    /**
     * Create an updated {@link Bindable} instance with additional bind restrictions.
     *
     * @param additionalRestrictions 适用的任何其他限制
     * @return 更新的 {@link Bindable}
     */
    public Bindable<T> withBindRestrictions(BindRestriction... additionalRestrictions) {
        EnumSet<BindRestriction> bindRestrictions = EnumSet.copyOf(this.bindRestrictions);
        bindRestrictions.addAll(Arrays.asList(additionalRestrictions));
        return new Bindable<>(this.type, this.boxedType, this.value, this.annotations, bindRestrictions);
    }

    /**
     * 创建指定实例类型的新 {@link Bindable}，现有值等于该实例。
     *
     * @param <T>      源类型
     * @param instance 实例（不能为null）
     * @return {@link Bindable} 实例
     * @see #of(ResolvableType)
     * @see #withExistingValue(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> Bindable<T> ofInstance(T instance) {
        Assert.notNull(instance, "Instance must not be null");
        Class<T> type = (Class<T>) instance.getClass();
        return of(type).withExistingValue(instance);
    }

    /**
     * 创建指定类型的新 {@link Bindable}
     *
     * @param <T>  源类型
     * @param type 类型（不能为null）
     * @return {@link Bindable} 实例
     * @see #of(ResolvableType)
     */
    public static <T> Bindable<T> of(Class<T> type) {
        Assert.notNull(type, "Type must not be null");
        return of(ResolvableType.forClass(type));
    }

    /**
     * 创建指定元素类型的新 {@link Bindable} {@link List}
     *
     * @param <E>         元素类型
     * @param elementType 列表元素类型
     * @return {@link Bindable} 实例
     */
    public static <E> Bindable<List<E>> listOf(Class<E> elementType) {
        return of(ResolvableType.forClassWithGenerics(List.class, elementType));
    }

    /**
     * 创建指定键和值类型的新 {@link Bindable} {@link Set}
     *
     * @param <E>         元素类型
     * @param elementType 集合元素类型
     * @return {@link Bindable} 实例
     */
    public static <E> Bindable<Set<E>> setOf(Class<E> elementType) {
        return of(ResolvableType.forClassWithGenerics(Set.class, elementType));
    }

    /**
     * 创建指定键和值类型的新 {@link Bindable} {@link Map}
     *
     * @param <K>       密钥类型
     * @param <V>       值类型
     * @param keyType   映射键类型
     * @param valueType 映射值类型
     * @return {@link Bindable} 实例
     */
    public static <K, V> Bindable<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
        return of(ResolvableType.forClassWithGenerics(Map.class, keyType, valueType));
    }

    /**
     * 创建指定类型的新 {@link Bindable}
     *
     * @param <T>  源类型
     * @param type 类型（不能为null）
     * @return {@link Bindable} 实例
     * @see #of(Class)
     */
    public static <T> Bindable<T> of(ResolvableType type) {
        Assert.notNull(type, "Type must not be null");
        ResolvableType boxedType = box(type);
        return new Bindable<>(type, boxedType, null, NO_ANNOTATIONS, NO_BIND_RESTRICTIONS);
    }

    private static ResolvableType box(ResolvableType type) {
        Class<?> resolved = type.resolve();
        if (resolved != null && resolved.isPrimitive()) {
            Object array = Array.newInstance(resolved, 1);
            Class<?> wrapperType = Array.get(array, 0).getClass();
            return ResolvableType.forClass(wrapperType);
        }
        if (resolved != null && resolved.isArray()) {
            return ResolvableType.forArrayComponent(box(type.getComponentType()));
        }
        return type;
    }

    /**
     * 绑定值时可以应用的限制。
     */
    public enum BindRestriction {
        /**
         * 不要绑定直接 {@link ConfigurationProperty} 匹配项。
         */
        NO_DIRECT_PROPERTY
    }
}
