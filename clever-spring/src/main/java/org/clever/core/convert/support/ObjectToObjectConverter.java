package org.clever.core.convert.support;

import org.clever.core.convert.ConversionFailedException;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.ClassUtils;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ReflectionUtils;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 泛型转换器，使用约定通过委托给源对象上的方法或targetType上的静态工厂方法或构造函数，将源对象转换为targetType<br/>
 * 警告：此转换器不支持该{@link Object#toString()}方法，用于将sourceType转换为{@code java.lang.String}。
 * 对于{@code toString()}支持，请改用{@link FallbackObjectToStringConverter}转换程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:35 <br/>
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {
    // Cache for the latest to-method resolved on a given Class
    private static final Map<Class<?>, Member> conversionMemberCache = new ConcurrentReferenceHashMap<>(32);

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (sourceType.getType() != targetType.getType() && hasConversionMethodOrConstructor(targetType.getType(), sourceType.getType()));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        Class<?> sourceClass = sourceType.getType();
        Class<?> targetClass = targetType.getType();
        Member member = getValidatedMember(targetClass, sourceClass);
        try {
            if (member instanceof Method) {
                Method method = (Method) member;
                ReflectionUtils.makeAccessible(method);
                if (!Modifier.isStatic(method.getModifiers())) {
                    return method.invoke(source);
                } else {
                    return method.invoke(null, source);
                }
            } else if (member instanceof Constructor) {
                Constructor<?> ctor = (Constructor<?>) member;
                ReflectionUtils.makeAccessible(ctor);
                return ctor.newInstance(source);
            }
        } catch (InvocationTargetException ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
        } catch (Throwable ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex);
        }
        // If sourceClass is Number and targetClass is Integer, the following message should expand to:
        // No toInteger() method exists on java.lang.Number, and no static valueOf/of/from(java.lang.Number)
        // method or Integer(java.lang.Number) constructor exists on java.lang.Integer.
        throw new IllegalStateException(String.format(
                "No to%3$s() method exists on %1$s, and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
                sourceClass.getName(),
                targetClass.getName(),
                targetClass.getSimpleName()
        ));
    }

    static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
        return (getValidatedMember(targetClass, sourceClass) != null);
    }

    private static Member getValidatedMember(Class<?> targetClass, Class<?> sourceClass) {
        Member member = conversionMemberCache.get(targetClass);
        if (isApplicable(member, sourceClass)) {
            return member;
        }
        member = determineToMethod(targetClass, sourceClass);
        if (member == null) {
            member = determineFactoryMethod(targetClass, sourceClass);
            if (member == null) {
                member = determineFactoryConstructor(targetClass, sourceClass);
                if (member == null) {
                    return null;
                }
            }
        }
        conversionMemberCache.put(targetClass, member);
        return member;
    }

    private static boolean isApplicable(Member member, Class<?> sourceClass) {
        if (member instanceof Method) {
            Method method = (Method) member;
            return !Modifier.isStatic(method.getModifiers()) ?
                    ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) :
                    method.getParameterTypes()[0] == sourceClass;
        } else if (member instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) member;
            return (ctor.getParameterTypes()[0] == sourceClass);
        } else {
            return false;
        }
    }

    private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
        if (String.class == targetClass || String.class == sourceClass) {
            // Do not accept a toString() method or any to methods on String itself
            return null;
        }
        Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
        return (method != null && !Modifier.isStatic(method.getModifiers()) && ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null);
    }

    private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
        if (String.class == targetClass) {
            // Do not accept the String.valueOf(Object) method
            return null;
        }
        Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
        if (method == null) {
            method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
            if (method == null) {
                method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
            }
        }
        return method;
    }

    private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
        return ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
    }
}