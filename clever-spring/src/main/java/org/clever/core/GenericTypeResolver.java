package org.clever.core;

import org.clever.util.Assert;
import org.clever.util.ConcurrentReferenceHashMap;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper类，用于根据类型变量解析泛型类型<br/>
 * 主要用于内部使用，解析方法参数类型，即使它们是泛型声明的
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:56 <br/>
 */
public final class GenericTypeResolver {
    /**
     * 缓存从类到类型变量的映射
     */
    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentReferenceHashMap<>();

    private GenericTypeResolver() {
    }

    /**
     * 确定给定方法的泛型返回类型的目标类型，其中在给定类上声明形式类型变量
     *
     * @param method 反射的函数
     * @param clazz  用于解析类型变量的类
     * @return 对应的泛型参数或返回类型
     */
    public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(clazz, "Class must not be null");
        return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
    }

    /**
     * 根据给定的目标方法解析给定泛型接口的单一类型参数，该目标方法假定返回给定接口或其实现
     *
     * @param method     检查返回类型的目标方法
     * @param genericIfc 从中解析类型参数的泛型接口或超类
     * @return 方法返回类型的已解析参数类型，如果不可解析或单个参数的类型为{@link WildcardType}，则为null
     */
    public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
        Assert.notNull(method, "Method must not be null");
        ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericIfc);
        if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
            return null;
        }
        return getSingleGeneric(resolvableType);
    }

    /**
     * 针对给定目标类解析给定泛型接口的单一类型参数，该目标类被假定为实现泛型接口，并可能为其类型变量声明具体类型
     *
     * @param clazz      要检查的目标类
     * @param genericIfc 从中解析类型参数的泛型接口或超类
     * @return 参数的已解析类型，如果不可解析，则为null
     */
    public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
        ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
        if (!resolvableType.hasGenerics()) {
            return null;
        }
        return getSingleGeneric(resolvableType);
    }

    private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
        Assert.isTrue(
                resolvableType.getGenerics().length == 1,
                () -> "Expected 1 type argument on generic interface [" + resolvableType + "] but found " + resolvableType.getGenerics().length
        );
        return resolvableType.getGeneric().resolve();
    }

    /**
     * 根据给定目标类解析给定泛型接口的类型参数，该目标类被假定为实现泛型接口，并可能为其类型变量声明具体类型
     *
     * @param clazz      要检查的目标类
     * @param genericIfc 从中解析类型参数的泛型接口或超类
     * @return 每个参数的已解析类型，数组大小与实际类型参数的数量匹配，如果不可解析，则为null
     */
    public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
        ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
        if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
            return null;
        }
        return type.resolveGenerics(Object.class);
    }

    /**
     * 根据给定的上下文类解析给定的泛型类型，并尽可能替换类型变量
     *
     * @param genericType  (潜在的)泛型类型
     * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中的类(可以为null)
     * @return 解析的类型(可能是现有的给定泛型类型)
     */
    public static Type resolveType(Type genericType, Class<?> contextClass) {
        if (contextClass != null) {
            if (genericType instanceof TypeVariable) {
                ResolvableType resolvedTypeVariable = resolveVariable((TypeVariable<?>) genericType, ResolvableType.forClass(contextClass));
                if (resolvedTypeVariable != ResolvableType.NONE) {
                    Class<?> resolved = resolvedTypeVariable.resolve();
                    if (resolved != null) {
                        return resolved;
                    }
                }
            } else if (genericType instanceof ParameterizedType) {
                ResolvableType resolvedType = ResolvableType.forType(genericType);
                if (resolvedType.hasUnresolvableGenerics()) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    ResolvableType contextType = ResolvableType.forClass(contextClass);
                    for (int i = 0; i < typeArguments.length; i++) {
                        Type typeArgument = typeArguments[i];
                        if (typeArgument instanceof TypeVariable) {
                            ResolvableType resolvedTypeArgument = resolveVariable((TypeVariable<?>) typeArgument, contextType);
                            if (resolvedTypeArgument != ResolvableType.NONE) {
                                generics[i] = resolvedTypeArgument.resolve();
                            } else {
                                generics[i] = ResolvableType.forType(typeArgument).resolve();
                            }
                        } else {
                            generics[i] = ResolvableType.forType(typeArgument).resolve();
                        }
                    }
                    Class<?> rawClass = resolvedType.getRawClass();
                    if (rawClass != null) {
                        return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
                    }
                }
            }
        }
        return genericType;
    }

    private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
        ResolvableType resolvedType;
        if (contextType.hasGenerics()) {
            resolvedType = ResolvableType.forType(typeVariable, contextType);
            if (resolvedType.resolve() != null) {
                return resolvedType;
            }
        }
        ResolvableType superType = contextType.getSuperType();
        if (superType != ResolvableType.NONE) {
            resolvedType = resolveVariable(typeVariable, superType);
            if (resolvedType.resolve() != null) {
                return resolvedType;
            }
        }
        for (ResolvableType ifc : contextType.getInterfaces()) {
            resolvedType = resolveVariable(typeVariable, ifc);
            if (resolvedType.resolve() != null) {
                return resolvedType;
            }
        }
        return ResolvableType.NONE;
    }

    /**
     * 根据给定的TypeVariable映射解析指定的泛型类型<br/>
     *
     * @param genericType 要解析的泛型类型
     * @param map         要解决的类型变量映射
     * @return 解析为类或对象的类型, 否则就是 {@code Object.class}
     */
    @SuppressWarnings("rawtypes")
    public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
        return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
    }

    /**
     * 为指定的类构建{@link TypeVariable#getName TypeVariable名}到{@link Class 具体类}的映射<br/>
     * 搜索所有超级类型、封闭类型和接口
     *
     * @see #resolveType(Type, Map)
     */
    @SuppressWarnings("rawtypes")
    public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
        Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
        if (typeVariableMap == null) {
            typeVariableMap = new HashMap<>();
            buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
            typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
        }
        return typeVariableMap;
    }

    @SuppressWarnings("rawtypes")
    private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
        if (type != ResolvableType.NONE) {
            Class<?> resolved = type.resolve();
            if (resolved != null && type.getType() instanceof ParameterizedType) {
                TypeVariable<?>[] variables = resolved.getTypeParameters();
                for (int i = 0; i < variables.length; i++) {
                    ResolvableType generic = type.getGeneric(i);
                    while (generic.getType() instanceof TypeVariable<?>) {
                        generic = generic.resolveType();
                    }
                    if (generic != ResolvableType.NONE) {
                        typeVariableMap.put(variables[i], generic.getType());
                    }
                }
            }
            buildTypeVariableMap(type.getSuperType(), typeVariableMap);
            for (ResolvableType interfaceType : type.getInterfaces()) {
                buildTypeVariableMap(interfaceType, typeVariableMap);
            }
            if (resolved != null && resolved.isMemberClass()) {
                buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {
        private final Map<TypeVariable, Type> typeVariableMap;

        public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
            this.typeVariableMap = typeVariableMap;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            Type type = this.typeVariableMap.get(variable);
            return (type != null ? ResolvableType.forType(type) : null);
        }

        @Override
        public Object getSource() {
            return this.typeVariableMap;
        }
    }
}
