package org.clever.core;

import org.clever.core.SerializableTypeWrapper.FieldTypeProvider;
import org.clever.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import org.clever.core.SerializableTypeWrapper.TypeProvider;
import org.clever.util.*;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * 1.封装Java Type，提供对其父类型、接口和泛型参数化类型的访问。<br/>
 * 2.Java Type体系中类型的包括：原始类型(Class)、参数化类型(ParameterizedType)、泛型数组类型(GenericArrayType)、泛型类型变量(TypeVariable)、基本类型(Class)。<br/>
 * 3.一个ResolvableType实例可以从字段、方法参数、方法返回类型、类中获得
 * <pre>
 *   1.{@link #forField(Field)}
 *   2.{@link #forField(Field)}
 *   3.{@link #forMethodParameter(Method, int)}
 *   4.{@link #forMethodReturnType(Method)}
 *   5.{@link #forClass(Class) class}
 * </pre>
 * 4.本类的大多数方法都会返回ResolvableType类型便于连续调用。<br/>
 * <pre>{@code
 * private HashMap<Integer, List<String>> testMap;
 * public void example() throws NoSuchFieldException {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("testMap"));
 *     t.getSuperType();          // AbstractMap<Integer, List<String>>
 *     t.asMap();                 // Map<Integer, List<String>>
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1);           // List<String>
 *     t.resolveGeneric(1, 0);    // String
 * }
 * }</pre>
 * 作者：lizw <br/>
 * 创建时间：2022/04/18 17:35 <br/>
 */
public class ResolvableType implements Serializable {
    /**
     * 没有可用值时返回的{@code ResolvableType.NONE}优先于null使用，因此可以安全地链接多个方法调用
     */
    public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);
    private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];
    private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache = new ConcurrentReferenceHashMap<>(256);

    /**
     * 底层的Java Type
     */
    private final Type type;
    /**
     * 提供类型的对象
     */
    private final TypeProvider typeProvider;
    /**
     * 要使用的{@code VariableResolver}，如果没有可用的解析器，则为{@code null}
     */
    private final VariableResolver variableResolver;
    /**
     * 数组的组件类型，如果应推导该类型，则为{@code null}
     */
    private final ResolvableType componentType;
    private final Integer hash;
    private Class<?> resolved;
    private volatile ResolvableType superType;
    private volatile ResolvableType[] interfaces;
    private volatile ResolvableType[] generics;

    /**
     * 专用构造函数用于创建用于缓存密钥目的的新{@link ResolvableType}，无需预先解析
     */
    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.hash = calculateHashCode();
        this.resolved = null;
    }

    /**
     * 专用构造函数，用于创建用于缓存值目的的新{@link ResolvableType}，具有预先解析和预先计算的hash
     */
    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver, Integer hash) {
        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.hash = hash;
        this.resolved = resolveClass();
    }

    /**
     * 专用构造函数用于为未缓存的目的创建新的{@link ResolvableType}，具有预先解析但延迟计算的hash
     */
    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver, ResolvableType componentType) {
        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = componentType;
        this.hash = null;
        this.resolved = resolveClass();
    }

    /**
     * 用于在类的基础上创建新的{@link ResolvableType}的私有构造函数<br/>
     * 避免所有instanceof检查，以创建直接的类包装
     */
    private ResolvableType(Class<?> clazz) {
        this.resolved = (clazz != null ? clazz : Object.class);
        this.type = this.resolved;
        this.typeProvider = null;
        this.variableResolver = null;
        this.componentType = null;
        this.hash = null;
    }

    /**
     * 底层的Java Type
     */
    public Type getType() {
        return SerializableTypeWrapper.unwrap(this.type);
    }

    /**
     * 返回原始的Class对象，不存在返回 null
     */
    public Class<?> getRawClass() {
        if (this.type == this.resolved) {
            return this.resolved;
        }
        Type rawType = this.type;
        if (rawType instanceof ParameterizedType) {
            rawType = ((ParameterizedType) rawType).getRawType();
        }
        return (rawType instanceof Class ? (Class<?>) rawType : null);
    }

    /**
     * 返回可解析类型的基础源
     */
    public Object getSource() {
        Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
        return (source != null ? source : this.type);
    }

    /**
     * 将此类型作为已解析的类返回，如果无法解析特定类，则返回{@link java.lang.Object}
     *
     * @see #getRawClass()
     * @see #resolve(Class)
     */
    public Class<?> toClass() {
        return resolve(Object.class);
    }

    /**
     * 确定给定对象是否是此{@code ResolvableType}的实例
     *
     * @see #isAssignableFrom(Class)
     */
    public boolean isInstance(Object obj) {
        return (obj != null && isAssignableFrom(obj.getClass()));
    }

    /**
     * 确定此{@code ResolvableType}是否可从指定的其他类型分配
     *
     * @see #isAssignableFrom(ResolvableType)
     */
    public boolean isAssignableFrom(Class<?> other) {
        return isAssignableFrom(forClass(other), null);
    }

    /**
     * 确定此{@code ResolvableType}是否可从指定的其他类型分配<br/>
     * 尝试遵循与Java编译器相同的规则，考虑解析的类是否可以从给定的类型分配，以及所有泛型是否都可以分配
     */
    public boolean isAssignableFrom(ResolvableType other) {
        return isAssignableFrom(other, null);
    }

    private boolean isAssignableFrom(ResolvableType other, Map<Type, Type> matchedBefore) {
        Assert.notNull(other, "ResolvableType must not be null");
        // If we cannot resolve types, we are not assignable
        if (this == NONE || other == NONE) {
            return false;
        }
        // Deal with array by delegating to the component type
        if (isArray()) {
            return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
        }
        if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
            return true;
        }
        // Deal with wildcard bounds
        WildcardBounds ourBounds = WildcardBounds.get(this);
        WildcardBounds typeBounds = WildcardBounds.get(other);
        // In the form X is assignable to <? extends Number>
        if (typeBounds != null) {
            return (ourBounds != null && ourBounds.isSameKind(typeBounds) && ourBounds.isAssignableFrom(typeBounds.getBounds()));
        }
        // In the form <? extends Number> is assignable to X...
        if (ourBounds != null) {
            return ourBounds.isAssignableFrom(other);
        }
        // Main assignability check about to follow
        boolean exactMatch = (matchedBefore != null);  // We're checking nested generic variables now...
        boolean checkGenerics = true;
        Class<?> ourResolved = null;
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    ourResolved = resolved.resolve();
                }
            }
            if (ourResolved == null) {
                // Try variable resolution against target type
                if (other.variableResolver != null) {
                    ResolvableType resolved = other.variableResolver.resolveVariable(variable);
                    if (resolved != null) {
                        ourResolved = resolved.resolve();
                        checkGenerics = false;
                    }
                }
            }
            if (ourResolved == null) {
                // Unresolved type variable, potentially nested -> never insist on exact match
                exactMatch = false;
            }
        }
        if (ourResolved == null) {
            ourResolved = resolve(Object.class);
        }
        Class<?> otherResolved = other.toClass();
        // We need an exact type match for generics
        // List<CharSequence> is not assignable from List<String>
        if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
            return false;
        }
        if (checkGenerics) {
            // Recursively check each generic
            ResolvableType[] ourGenerics = getGenerics();
            ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
            if (ourGenerics.length != typeGenerics.length) {
                return false;
            }
            if (matchedBefore == null) {
                matchedBefore = new IdentityHashMap<>(1);
            }
            matchedBefore.put(this.type, other.type);
            for (int i = 0; i < ourGenerics.length; i++) {
                if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 如果此类型解析为表示数组的类，则返回true
     *
     * @see #getComponentType()
     */
    public boolean isArray() {
        if (this == NONE) {
            return false;
        }
        return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) || this.type instanceof GenericArrayType || resolveType().isArray());
    }

    /**
     * 返回表示数组元素类型的ResolvableType，如果此类型不表示数组，则返回{@link #NONE}
     *
     * @see #isArray()
     */
    public ResolvableType getComponentType() {
        if (this == NONE) {
            return NONE;
        }
        if (this.componentType != null) {
            return this.componentType;
        }
        if (this.type instanceof Class) {
            Class<?> componentType = ((Class<?>) this.type).getComponentType();
            return forType(componentType, this.variableResolver);
        }
        if (this.type instanceof GenericArrayType) {
            return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
        }
        return resolveType().getComponentType();
    }

    /**
     * 转换类型为集合类型，如果此类型未实现或扩展{@link Collection}，则返回{@link #NONE}
     *
     * @see #as(Class)
     * @see #asMap()
     */
    public ResolvableType asCollection() {
        return as(Collection.class);
    }

    /**
     * 转换类型为{@link Map}类型，如果此类型未实现或扩展{@link Map}，则返回{@link #NONE}
     *
     * @see #as(Class)
     * @see #asCollection()
     */
    public ResolvableType asMap() {
        return as(Map.class);
    }

    /**
     * 转换类型，搜索超类型和接口层次结构以查找匹配项，如果此类型未实现或扩展指定的类，则返回{@link #NONE}
     *
     * @see #asCollection()
     * @see #asMap()
     * @see #getSuperType()
     * @see #getInterfaces()
     */
    public ResolvableType as(Class<?> type) {
        if (this == NONE) {
            return NONE;
        }
        Class<?> resolved = resolve();
        if (resolved == null || resolved == type) {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaces()) {
            ResolvableType interfaceAsType = interfaceType.as(type);
            if (interfaceAsType != NONE) {
                return interfaceAsType;
            }
        }
        return getSuperType().as(type);
    }

    /**
     * 返回表示此类型的直接超类型的{@link ResolvableType} <br/>
     * 如果没有可用的超类型，此方法将返回{@link #NONE} <br/>
     * 注意：生成的{@link ResolvableType}实例可能无法序列化
     *
     * @see #getInterfaces()
     */
    public ResolvableType getSuperType() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return NONE;
        }
        try {
            Type superclass = resolved.getGenericSuperclass();
            if (superclass == null) {
                return NONE;
            }
            ResolvableType superType = this.superType;
            if (superType == null) {
                superType = forType(superclass, this);
                this.superType = superType;
            }
            return superType;
        } catch (TypeNotPresentException ex) {
            // Ignore non-present types in generic signature
            return NONE;
        }
    }

    /**
     * 返回表示此类型实现的直接接口的{@link ResolvableType}数组。如果此类型未实现任何接口，则返回空数组<br/>
     * 注意：生成的{@link ResolvableType}实例可能无法序列化
     *
     * @see #getSuperType()
     */
    public ResolvableType[] getInterfaces() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return EMPTY_TYPES_ARRAY;
        }
        ResolvableType[] interfaces = this.interfaces;
        if (interfaces == null) {
            Type[] genericIfcs = resolved.getGenericInterfaces();
            interfaces = new ResolvableType[genericIfcs.length];
            for (int i = 0; i < genericIfcs.length; i++) {
                interfaces[i] = forType(genericIfcs[i], this);
            }
            this.interfaces = interfaces;
        }
        return interfaces;
    }

    /**
     * 如果此类型包含泛型参数，则返回true
     *
     * @see #getGeneric(int...)
     * @see #getGenerics()
     */
    public boolean hasGenerics() {
        return (getGenerics().length > 0);
    }

    /**
     * 如果此类型仅包含无法解析的泛型，即不替换其声明的任何类型变量，则返回true
     */
    boolean isEntirelyUnresolvable() {
        if (this == NONE) {
            return false;
        }
        ResolvableType[] generics = getGenerics();
        for (ResolvableType generic : generics) {
            if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 确定基础类型是否有任何不可解析的泛型：要么通过类型本身上的不可解析类型变量，要么通过以原始方式实现泛型接口，即不替换该接口的类型变量。
     * 只有在这两种情况下，结果才是true
     */
    public boolean hasUnresolvableGenerics() {
        if (this == NONE) {
            return false;
        }
        ResolvableType[] generics = getGenerics();
        for (ResolvableType generic : generics) {
            if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
                return true;
            }
        }
        Class<?> resolved = resolve();
        if (resolved != null) {
            try {
                for (Type genericInterface : resolved.getGenericInterfaces()) {
                    if (genericInterface instanceof Class) {
                        if (forClass((Class<?>) genericInterface).hasGenerics()) {
                            return true;
                        }
                    }
                }
            } catch (TypeNotPresentException ex) {
                // Ignore non-present types in generic signature
            }
            return getSuperType().hasUnresolvableGenerics();
        }
        return false;
    }

    /**
     * 确定基础类型是否是无法通过关联变量解析程序解析的类型变量
     */
    private boolean isUnresolvableTypeVariable() {
        if (this.type instanceof TypeVariable) {
            if (this.variableResolver == null) {
                return true;
            }
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            ResolvableType resolved = this.variableResolver.resolveVariable(variable);
            return resolved == null || resolved.isUnresolvableTypeVariable();
        }
        return false;
    }

    /**
     * 确定基础类型是否表示没有特定边界的通配符(即{@code ? extends Object})
     */
    private boolean isWildcardWithoutBounds() {
        if (this.type instanceof WildcardType) {
            WildcardType wt = (WildcardType) this.type;
            if (wt.getLowerBounds().length == 0) {
                Type[] upperBounds = wt.getUpperBounds();
                return upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0]);
            }
        }
        return false;
    }

    /**
     * 返回指定嵌套级别的{@link ResolvableType}，有关详细信息，请参见{@link #getNested(int, Map)}
     *
     * @param nestingLevel 嵌套级别
     */
    public ResolvableType getNested(int nestingLevel) {
        return getNested(nestingLevel, null);
    }

    /**
     * 返回指定嵌套级别的{@link ResolvableType}<br/>
     * 嵌套级别是指：应返回的特定泛型参数。<br/>
     * 嵌套级别为：1表示此类型；2表示第一个嵌套泛型；3第二个嵌套泛型；等等<br/>
     * {@code typeIndexesPerLevel} Map可用于引用给定级别的特定泛型。<br/>
     * 如果类型不包含泛型，则将考虑超类型层次结构。
     *
     * @param nestingLevel        所需的嵌套级别，1:当前类型，2:第一个嵌套泛型，3:第二个嵌套泛型，依此类推
     * @param typeIndexesPerLevel 包含给定嵌套级别的通用索引的Map (可能为 {@code null})
     */
    public ResolvableType getNested(int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel) {
        ResolvableType result = this;
        for (int i = 2; i <= nestingLevel; i++) {
            if (result.isArray()) {
                result = result.getComponentType();
            } else {
                // Handle derived types
                while (result != ResolvableType.NONE && !result.hasGenerics()) {
                    result = result.getSuperType();
                }
                Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
                index = (index == null ? result.getGenerics().length - 1 : index);
                result = result.getGeneric(index);
            }
        }
        return result;
    }

    /**
     * 返回表示给定索引的泛型参数的{@link ResolvableType}。索引是基于零的；
     * 例如，给定类型{@code Map<Integer, List<String>>}，{@code getGeneric(0)}将访问Integer。
     * 可以通过指定多个索引来访问嵌套泛型；
     * 例如，{@code getGeneric(1, 0)} 将访问嵌套List中的String。
     * 为方便起见，如果未指定索引，则返回第一个泛型。
     * 如果指定索引中没有可用的泛型，则不会返回任何泛型
     *
     * @see #hasGenerics()
     * @see #getGenerics()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType getGeneric(int... indexes) {
        ResolvableType[] generics = getGenerics();
        if (indexes == null || indexes.length == 0) {
            return (generics.length == 0 ? NONE : generics[0]);
        }
        ResolvableType generic = this;
        for (int index : indexes) {
            generics = generic.getGenerics();
            if (index < 0 || index >= generics.length) {
                return NONE;
            }
            generic = generics[index];
        }
        return generic;
    }

    /**
     * 返回表示此类型的泛型参数的{@link ResolvableType}数组。
     * 如果没有可用的泛型，则返回空数组。
     * 如果需要访问特定的泛型，请考虑使用{@link #getGeneric(int...)}方法，
     * 因为它允许访问嵌套泛型并防止{@code IndexOutOfBoundsExceptions}
     *
     * @see #hasGenerics()
     * @see #getGeneric(int...)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType[] getGenerics() {
        if (this == NONE) {
            return EMPTY_TYPES_ARRAY;
        }
        ResolvableType[] generics = this.generics;
        if (generics == null) {
            if (this.type instanceof Class) {
                Type[] typeParams = ((Class<?>) this.type).getTypeParameters();
                generics = new ResolvableType[typeParams.length];
                for (int i = 0; i < generics.length; i++) {
                    generics[i] = ResolvableType.forType(typeParams[i], this);
                }
            } else if (this.type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
                generics = new ResolvableType[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = forType(actualTypeArguments[i], this.variableResolver);
                }
            } else {
                generics = resolveType().getGenerics();
            }
            this.generics = generics;
        }
        return generics;
    }

    /**
     * 获取和解析泛型参数的便捷方法
     *
     * @see #getGenerics()
     * @see #resolve()
     */
    public Class<?>[] resolveGenerics() {
        ResolvableType[] generics = getGenerics();
        Class<?>[] resolvedGenerics = new Class<?>[generics.length];
        for (int i = 0; i < generics.length; i++) {
            resolvedGenerics[i] = generics[i].resolve();
        }
        return resolvedGenerics;
    }

    /**
     * 获取和解析泛型参数的便捷方法，如果无法解析任何类型，则使用指定的{@code fallback}
     *
     * @see #getGenerics()
     * @see #resolve()
     */
    public Class<?>[] resolveGenerics(Class<?> fallback) {
        ResolvableType[] generics = getGenerics();
        Class<?>[] resolvedGenerics = new Class<?>[generics.length];
        for (int i = 0; i < generics.length; i++) {
            resolvedGenerics[i] = generics[i].resolve(fallback);
        }
        return resolvedGenerics;
    }

    /**
     * 获取和解析特定泛型参数的便捷方法
     *
     * @param indexes 引用泛型参数的索引
     * @see #getGeneric(int...)
     * @see #resolve()
     */
    public Class<?> resolveGeneric(int... indexes) {
        return getGeneric(indexes).resolve();
    }

    /**
     * 将此类型解析为{@link java.lang.Class}，如果无法解析该类型，则返回null。
     * 如果直接解析失败，此方法将考虑{@link TypeVariable}和{@link WildcardType}的边界；
     * 但是，对象的边界。类将被忽略。如果此方法返回一个非null类，而{@link #hasGenerics()}返回false，
     * 那么给定的类型将有效地包装一个普通类，如果需要，允许进行普通类处理
     *
     * @see #resolve(Class)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve() {
        return this.resolved;
    }

    /**
     * 将此类型解析为{@link java.lang.Class}，如果无法解析该类型，则返回指定的{@code fallback}
     * 如果直接解析失败，此方法将考虑{@link TypeVariable}和{@link WildcardType}的边界；
     * 但是，对象的边界。类将被忽略
     *
     * @see #resolve()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve(Class<?> fallback) {
        return (this.resolved != null ? this.resolved : fallback);
    }

    private Class<?> resolveClass() {
        if (this.type == EmptyType.INSTANCE) {
            return null;
        }
        if (this.type instanceof Class) {
            return (Class<?>) this.type;
        }
        if (this.type instanceof GenericArrayType) {
            Class<?> resolvedComponent = getComponentType().resolve();
            return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
        }
        return resolveType().resolve();
    }

    /**
     * 通过单个级别解析此类型，返回已解析的值或{@link #NONE}<br/>
     * 注意：返回的{@link ResolvableType}只能用作中介，因为它无法序列化
     */
    ResolvableType resolveType() {
        if (this.type instanceof ParameterizedType) {
            return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType) {
            Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
            if (resolved == null) {
                resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
            }
            return forType(resolved, this.variableResolver);
        }
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    return resolved;
                }
            }
            // Fallback to bounds
            return forType(resolveBounds(variable.getBounds()), this.variableResolver);
        }
        return NONE;
    }

    private Type resolveBounds(Type[] bounds) {
        if (bounds.length == 0 || bounds[0] == Object.class) {
            return null;
        }
        return bounds[0];
    }

    private ResolvableType resolveVariable(TypeVariable<?> variable) {
        if (this.type instanceof TypeVariable) {
            return resolveType().resolveVariable(variable);
        }
        if (this.type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) this.type;
            Class<?> resolved = resolve();
            if (resolved == null) {
                return null;
            }
            TypeVariable<?>[] variables = resolved.getTypeParameters();
            for (int i = 0; i < variables.length; i++) {
                if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
                    Type actualType = parameterizedType.getActualTypeArguments()[i];
                    return forType(actualType, this.variableResolver);
                }
            }
            Type ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                return forType(ownerType, this.variableResolver).resolveVariable(variable);
            }
        }
        if (this.type instanceof WildcardType) {
            ResolvableType resolved = resolveType().resolveVariable(variable);
            if (resolved != null) {
                return resolved;
            }
        }
        if (this.variableResolver != null) {
            return this.variableResolver.resolveVariable(variable);
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResolvableType)) {
            return false;
        }

        ResolvableType otherType = (ResolvableType) other;
        if (!ObjectUtils.nullSafeEquals(this.type, otherType.type)) {
            return false;
        }
        if (this.typeProvider != otherType.typeProvider && (this.typeProvider == null || otherType.typeProvider == null || !ObjectUtils.nullSafeEquals(this.typeProvider.getType(), otherType.typeProvider.getType()))) {
            return false;
        }
        if (this.variableResolver != otherType.variableResolver && (this.variableResolver == null || otherType.variableResolver == null || !ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(this.componentType, otherType.componentType);
    }

    @Override
    public int hashCode() {
        return (this.hash != null ? this.hash : calculateHashCode());
    }

    private int calculateHashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(this.type);
        if (this.typeProvider != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
        }
        if (this.variableResolver != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
        }
        if (this.componentType != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
        }
        return hashCode;
    }

    /**
     * 将此{@link ResolvableType}适配为{@link VariableResolver}
     */
    VariableResolver asVariableResolver() {
        if (this == NONE) {
            return null;
        }
        return new DefaultVariableResolver(this);
    }

    /**
     * 自定义序列化支持{@link #NONE}.
     */
    private Object readResolve() {
        return (this.type == EmptyType.INSTANCE ? NONE : this);
    }

    /**
     * 以完全解析的形式返回此类型的字符串表示形式(包括任何泛型参数)
     */
    @Override
    public String toString() {
        if (isArray()) {
            return getComponentType() + "[]";
        }
        if (this.resolved == null) {
            return "?";
        }
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
                // Don't bother with variable boundaries for toString()...
                // Can cause infinite recursions in case of self-references
                return "?";
            }
        }
        if (hasGenerics()) {
            return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
        }
        return this.resolved.getName();
    }

    // Factory methods

    /**
     * 返回指定类的{@link ResolvableType}，使用完整的泛型类型信息进行可分配性检查。<br/>
     * 例如：{@code ResolvableType.forClass(MyArrayList.class)}
     *
     * @param clazz 要内省的类(对于这里的典型用例，{@code null}在语义上等同于{@code Object.class})
     * @see #forClass(Class, Class)
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClass(Class<?> clazz) {
        return new ResolvableType(clazz);
    }

    /**
     * 返回指定类的{@link ResolvableType}，仅对原始类进行可分配性检查(类似于{@link Class#isAssignableFrom}，它用作包装器)。<br/>
     * 例如：{@code ResolvableType.forRawClass(List.class)}
     *
     * @param clazz 要内省的类(对于这里的典型用例，{@code null}在语义上等同于{@code Object.class})
     * @see #forClass(Class)
     * @see #getRawClass()
     */
    public static ResolvableType forRawClass(Class<?> clazz) {
        return new ResolvableType(clazz) {
            @Override
            public ResolvableType[] getGenerics() {
                return EMPTY_TYPES_ARRAY;
            }

            @Override
            public boolean isAssignableFrom(Class<?> other) {
                return (clazz == null || ClassUtils.isAssignable(clazz, other));
            }

            @Override
            public boolean isAssignableFrom(ResolvableType other) {
                Class<?> otherClass = other.resolve();
                return (otherClass != null && (clazz == null || ClassUtils.isAssignable(clazz, otherClass)));
            }
        };
    }

    /**
     * 返回具有给定实现类的指定基类型(接口或基类)的{@link ResolvableType} <br/>
     * 例如：{@code ResolvableType.forClass(List.class, MyArrayList.class)}
     *
     * @param baseType            基本类型(不能是 {@code null})
     * @param implementationClass 实现类
     * @see #forClass(Class)
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
        Assert.notNull(baseType, "Base type must not be null");
        ResolvableType asType = forType(implementationClass).as(baseType);
        return (asType == NONE ? forType(baseType) : asType);
    }

    /**
     * 返回具有预声明泛型的指定类的{@link ResolvableType}
     *
     * @param clazz    要内省的类(或接口)
     * @param generics 类的泛型
     * @see #forClassWithGenerics(Class, ResolvableType...)
     */
    public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(generics, "Generics array must not be null");
        ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
        for (int i = 0; i < generics.length; i++) {
            resolvableGenerics[i] = forClass(generics[i]);
        }
        return forClassWithGenerics(clazz, resolvableGenerics);
    }

    /**
     * 返回具有预声明泛型的指定类的{@link ResolvableType}
     *
     * @param clazz    要内省的类(或接口)
     * @param generics 类的泛型
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(generics, "Generics array must not be null");
        TypeVariable<?>[] variables = clazz.getTypeParameters();
        Assert.isTrue(variables.length == generics.length, () -> "Mismatched number of generics specified for " + clazz.toGenericString());
        Type[] arguments = new Type[generics.length];
        for (int i = 0; i < generics.length; i++) {
            ResolvableType generic = generics[i];
            Type argument = (generic != null ? generic.getType() : null);
            arguments[i] = (argument != null && !(argument instanceof TypeVariable) ? argument : variables[i]);
        }
        ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
        return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
    }

    /**
     * 返回指定实例的{@link ResolvableType}。
     * {@code instance}不传递一般信息，但如果它实现了{@link ResolvableTypeProvider}，
     * 则可以使用比基于类实例的简单{@link ResolvableType}更精确的{@link ResolvableType}
     *
     * @param instance instance
     * @see ResolvableTypeProvider
     */
    public static ResolvableType forInstance(Object instance) {
        Assert.notNull(instance, "Instance must not be null");
        if (instance instanceof ResolvableTypeProvider) {
            ResolvableType type = ((ResolvableTypeProvider) instance).getResolvableType();
            if (type != null) {
                return type;
            }
        }
        return ResolvableType.forClass(instance.getClass());
    }

    /**
     * 返回指定字段的{@link ResolvableType}
     *
     * @param field 字段
     * @see #forField(Field, Class)
     */
    public static ResolvableType forField(Field field) {
        Assert.notNull(field, "Field must not be null");
        return forType(null, new FieldTypeProvider(field), null);
    }

    /**
     * 返回具有给定实现的指定字段的{@link ResolvableType}。
     * 当声明字段的类包含实现类满足的通用参数变量时，请使用此变量
     *
     * @param field               字段
     * @param implementationClass 实现类
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, Class<?> implementationClass) {
        Assert.notNull(field, "Field must not be null");
        ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
        return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
    }

    /**
     * 返回具有给定实现的指定字段的{@link ResolvableType}。
     * 当声明字段的类包含实现类型满足的通用参数变量时，请使用此变量
     *
     * @param field              字段
     * @param implementationType 实现类型
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, ResolvableType implementationType) {
        Assert.notNull(field, "Field must not be null");
        ResolvableType owner = (implementationType != null ? implementationType : NONE);
        owner = owner.as(field.getDeclaringClass());
        return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
    }

    /**
     * 返回具有给定嵌套级别的指定字段的{@link ResolvableType}
     *
     * @param field        字段
     * @param nestingLevel 嵌套级别(1表示外部级别；2表示嵌套泛型类型；等等)
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, int nestingLevel) {
        Assert.notNull(field, "Field must not be null");
        return forType(null, new FieldTypeProvider(field), null).getNested(nestingLevel);
    }

    /**
     * 返回具有给定实现和给定嵌套级别的指定字段的{@link ResolvableType}。
     * 当声明字段的类包含实现类满足的通用参数变量时，请使用此变量
     *
     * @param field               字段
     * @param nestingLevel        嵌套级别(1表示外部级别；2表示嵌套泛型类型；等等)
     * @param implementationClass 实现类
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, int nestingLevel, Class<?> implementationClass) {
        Assert.notNull(field, "Field must not be null");
        ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
        return forType(null, new FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
    }

    /**
     * 为指定的构造函数参数返回{@link ResolvableType}.
     *
     * @param constructor    源构造函数(不能为null)
     * @param parameterIndex 参数索引
     * @see #forConstructorParameter(Constructor, int, Class)
     */
    public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex) {
        Assert.notNull(constructor, "Constructor must not be null");
        return forMethodParameter(new MethodParameter(constructor, parameterIndex));
    }

    /**
     * 返回具有给定实现的指定构造函数参数的{@link ResolvableType}。
     * 当声明构造函数的类包含实现类满足的通用参数变量时，请使用此变量
     *
     * @param constructor         源构造函数(不能为null)
     * @param parameterIndex      参数索引
     * @param implementationClass 实现类
     * @see #forConstructorParameter(Constructor, int)
     */
    public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex, Class<?> implementationClass) {
        Assert.notNull(constructor, "Constructor must not be null");
        MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex, implementationClass);
        return forMethodParameter(methodParameter);
    }

    /**
     * 为指定的方法返回类型返回{@link ResolvableType}
     *
     * @param method 源方法
     * @see #forMethodReturnType(Method, Class)
     */
    public static ResolvableType forMethodReturnType(Method method) {
        Assert.notNull(method, "Method must not be null");
        return forMethodParameter(new MethodParameter(method, -1));
    }

    /**
     * 为指定的方法返回类型返回{@link ResolvableType}。
     * 当声明方法的类包含实现类所满足的泛型参数变量时，请使用此变量
     *
     * @param method              源方法
     * @param implementationClass 实现类
     * @see #forMethodReturnType(Method)
     */
    public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
        Assert.notNull(method, "Method must not be null");
        MethodParameter methodParameter = new MethodParameter(method, -1, implementationClass);
        return forMethodParameter(methodParameter);
    }

    /**
     * 为指定的方法参数返回{@link ResolvableType}
     *
     * @param method         源方法(不能为null)
     * @param parameterIndex 参数索引
     * @see #forMethodParameter(Method, int, Class)
     * @see #forMethodParameter(MethodParameter)
     */
    public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
        Assert.notNull(method, "Method must not be null");
        return forMethodParameter(new MethodParameter(method, parameterIndex));
    }

    /**
     * 返回具有给定实现的指定方法参数的{@link ResolvableType}。
     * 当声明方法的类包含实现类所满足的泛型参数变量时，请使用此变量
     *
     * @param method              源方法(不能为null)
     * @param parameterIndex      参数索引
     * @param implementationClass 实现类
     * @see #forMethodParameter(Method, int, Class)
     * @see #forMethodParameter(MethodParameter)
     */
    public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
        Assert.notNull(method, "Method must not be null");
        MethodParameter methodParameter = new MethodParameter(method, parameterIndex, implementationClass);
        return forMethodParameter(methodParameter);
    }

    /**
     * 为指定的{@link MethodParameter}返回{@link ResolvableType}
     *
     * @param methodParameter 源方法参数(不能为null)
     * @see #forMethodParameter(Method, int)
     */
    public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
        return forMethodParameter(methodParameter, (Type) null);
    }

    /**
     * 返回具有给定实现类型的指定{@link MethodParameter}{@link ResolvableType}
     * 当声明方法的类包含实现类型满足的泛型参数变量时，请使用此变量
     *
     * @param methodParameter    源方法参数(不能为null)
     * @param implementationType 实现类
     * @see #forMethodParameter(MethodParameter)
     */
    public static ResolvableType forMethodParameter(MethodParameter methodParameter, ResolvableType implementationType) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        implementationType = (implementationType != null ? implementationType : forType(methodParameter.getContainingClass()));
        ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
        return forType(
                null,
                new MethodParameterTypeProvider(methodParameter),
                owner.asVariableResolver()
        ).getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
    }

    /**
     * 为指定的{@link MethodParameter}返回{@link ResolvableType}，重写要使用特定给定类型解析的目标类型
     *
     * @param methodParameter 源方法参数(不能为null)
     * @param targetType      要解析的类型(方法参数类型的一部分)
     * @see #forMethodParameter(Method, int)
     */
    public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
    }

    /**
     * 在特定的嵌套级别为指定的{@link MethodParameter}返回{@link ResolvableType}，重写目标类型以使用特定的给定类型进行解析
     *
     * @param methodParameter 源方法参数(不能为null)
     * @param targetType      要解析的类型(方法参数类型的一部分)
     * @param nestingLevel    要使用的嵌套级别
     * @see #forMethodParameter(Method, int)
     */
    static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType, int nestingLevel) {
        ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
        return forType(
                targetType,
                new MethodParameterTypeProvider(methodParameter),
                owner.asVariableResolver()
        ).getNested(nestingLevel, methodParameter.typeIndexesPerLevel);
    }

    /**
     * 将{@link ResolvableType}作为指定{@code componentType}的数组返回
     *
     * @param componentType 组件类型
     * @return 作为指定组件类型的数组的可解析类型
     */
    public static ResolvableType forArrayComponent(ResolvableType componentType) {
        Assert.notNull(componentType, "Component type must not be null");
        Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
        return new ResolvableType(arrayClass, null, null, componentType);
    }

    /**
     * 返回指定类型的{@link ResolvableType}。<br/>
     * 注意：生成的{@link ResolvableType}实例可能无法序列化
     *
     * @param type 源类型
     * @see #forType(Type, ResolvableType)
     */
    public static ResolvableType forType(Type type) {
        return forType(type, null, null);
    }

    /**
     * 返回给定所有者类型支持的指定类型的{@link ResolvableType}。<br/>
     * 注意：生成的{@link ResolvableType}实例可能无法序列化
     *
     * @param type  源类型或{@code null}
     * @param owner 用于解析变量的所有者类型
     * @see #forType(Type)
     */
    public static ResolvableType forType(Type type, ResolvableType owner) {
        VariableResolver variableResolver = null;
        if (owner != null) {
            variableResolver = owner.asVariableResolver();
        }
        return forType(type, variableResolver);
    }

    /**
     * 为指定的{@link ParameterizedTypeReference}返回{@link ResolvableType}。<br/>
     * 注意：生成的{@link ResolvableType}实例可能无法序列化
     *
     * @param typeReference 从中获取源类型的引用
     * @see #forType(Type)
     */
    public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
        return forType(typeReference.getType(), null, null);
    }

    /**
     * 返回由给定{@link ResolvableType}支持的指定类型的{@link VariableResolver}
     *
     * @param type             源类型或{@code null}
     * @param variableResolver 变量解析器或{@code null}
     */
    static ResolvableType forType(Type type, VariableResolver variableResolver) {
        return forType(type, null, variableResolver);
    }

    /**
     * 返回由给定{@link ResolvableType}支持的指定类型的{@link VariableResolver}
     *
     * @param type             源类型或{@code null}
     * @param typeProvider     变量解析器或{@code null}
     * @param variableResolver 变量解析器或{@code null}
     */
    static ResolvableType forType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        if (type == null && typeProvider != null) {
            type = SerializableTypeWrapper.forTypeProvider(typeProvider);
        }
        if (type == null) {
            return NONE;
        }
        // For simple Class references, build the wrapper right away -
        // no expensive resolution necessary, so not worth caching...
        if (type instanceof Class) {
            return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
        }
        // Purge empty entries on access since we don't have a clean-up thread or the like.
        cache.purgeUnreferencedEntries();
        // Check the cache - we may have a ResolvableType which has been resolved before...
        ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
        ResolvableType cachedType = cache.get(resultType);
        if (cachedType == null) {
            cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
            cache.put(cachedType, cachedType);
        }
        resultType.resolved = cachedType.resolved;
        return resultType;
    }

    /**
     * 清除内部{@code ResolvableType}/{@code SerializableTypeWrapper}缓存
     */
    public static void clearCache() {
        cache.clear();
        SerializableTypeWrapper.cache.clear();
    }

    /**
     * 用于解析{@link TypeVariable}的策略接口
     */
    interface VariableResolver extends Serializable {
        /**
         * 返回解析器的源代码(用于hashCode和equals)
         */
        Object getSource();

        /**
         * 解析指定的变量
         *
         * @param variable 要解析的变量
         * @return 已解析的变量, 如果没有找到返回{@code null}
         */
        ResolvableType resolveVariable(TypeVariable<?> variable);
    }

    private static class DefaultVariableResolver implements VariableResolver {
        private final ResolvableType source;

        DefaultVariableResolver(ResolvableType resolvableType) {
            this.source = resolvableType;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            return this.source.resolveVariable(variable);
        }

        @Override
        public Object getSource() {
            return this.source;
        }
    }

    private static class TypeVariablesVariableResolver implements VariableResolver {
        private final TypeVariable<?>[] variables;
        private final ResolvableType[] generics;

        public TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics) {
            this.variables = variables;
            this.generics = generics;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            TypeVariable<?> variableToCompare = SerializableTypeWrapper.unwrap(variable);
            for (int i = 0; i < this.variables.length; i++) {
                TypeVariable<?> resolvedVariable = SerializableTypeWrapper.unwrap(this.variables[i]);
                if (ObjectUtils.nullSafeEquals(resolvedVariable, variableToCompare)) {
                    return this.generics[i];
                }
            }
            return null;
        }

        @Override
        public Object getSource() {
            return this.generics;
        }
    }

    private static final class SyntheticParameterizedType implements ParameterizedType, Serializable {
        private final Type rawType;
        private final Type[] typeArguments;

        public SyntheticParameterizedType(Type rawType, Type[] typeArguments) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
        }

        @Override
        public String getTypeName() {
            String typeName = this.rawType.getTypeName();
            if (this.typeArguments.length > 0) {
                StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
                for (Type argument : this.typeArguments) {
                    stringJoiner.add(argument.getTypeName());
                }
                return typeName + stringJoiner;
            }
            return typeName;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return this.rawType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return this.typeArguments;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType otherType = (ParameterizedType) other;
            return otherType.getOwnerType() == null
                    && this.rawType.equals(otherType.getRawType())
                    && Arrays.equals(this.typeArguments, otherType.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    /**
     * 处理通配符类型({@link WildcardType})的边界的内部助手
     */
    private static class WildcardBounds {
        private final Kind kind;
        private final ResolvableType[] bounds;

        /**
         * 用于创建新{@link WildcardBounds}实例的内部构造函数
         *
         * @param kind   边界的类型
         * @param bounds 边界
         * @see #get(ResolvableType)
         */
        public WildcardBounds(Kind kind, ResolvableType[] bounds) {
            this.kind = kind;
            this.bounds = bounds;
        }

        /**
         * 如果此边界与指定的边界类型相同，则返回true
         */
        public boolean isSameKind(WildcardBounds bounds) {
            return this.kind == bounds.kind;
        }

        /**
         * 如果此边界可分配给所有指定类型，则返回true
         *
         * @param types 要测试的类型
         * @return 如果此边界可分配给所有类型，则为true
         */
        public boolean isAssignableFrom(ResolvableType... types) {
            for (ResolvableType bound : this.bounds) {
                for (ResolvableType type : types) {
                    if (!isAssignable(bound, type)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isAssignable(ResolvableType source, ResolvableType from) {
            return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
        }

        /**
         * 返回基础边界
         */
        public ResolvableType[] getBounds() {
            return this.bounds;
        }

        /**
         * 获取指定类型的{@link WildcardBounds}实例，如果指定类型无法解析为通配符类型({@link WildcardType})，则返回null
         *
         * @param type 源类型
         * @return {@link WildcardBounds}实例或null
         */
        public static WildcardBounds get(ResolvableType type) {
            ResolvableType resolveToWildcard = type;
            while (!(resolveToWildcard.getType() instanceof WildcardType)) {
                if (resolveToWildcard == NONE) {
                    return null;
                }
                resolveToWildcard = resolveToWildcard.resolveType();
            }
            WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
            Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
            Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
            ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
            for (int i = 0; i < bounds.length; i++) {
                resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
            }
            return new WildcardBounds(boundsType, resolvableBounds);
        }

        /**
         * 各种各样的界限
         */
        enum Kind {UPPER, LOWER}
    }

    /**
     * 用于表示空值的内部类型
     */
    static class EmptyType implements Type, Serializable {
        static final Type INSTANCE = new EmptyType();

        Object readResolve() {
            return INSTANCE;
        }
    }
}
