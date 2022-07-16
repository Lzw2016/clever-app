package org.clever.core;

import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ObjectUtils;
import org.clever.util.ReflectionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.*;

/**
 * 1.支持序列化的{@link java.lang.reflect.Type}封装。<br/>
 * 2.仅支持GenericArrayType(泛型数组类型)、ParameterizedType(泛型参数化类型)、TypeVariable(泛型类型变量)、WildcardType(泛型类型表达式)。<br/>
 * 3.除了类(final class)之外，对返回更多类型的方法(例如: {@link GenericArrayType#getGenericComponentType()})的调用将自动包装<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 10:44 <br/>
 */
final class SerializableTypeWrapper {
    /**
     * 支持的封装(变为序列化)的Type<br/>
     * <pre>{@code
     * 1.GenericArrayType  泛型数组
     * 2.ParameterizedType 泛型参数化类型
     * 3.TypeVariable      泛型类型变量
     * 4.WildcardType      泛型类型表达式
     * }</pre>
     */
    private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class};
    /**
     * 可序列化类型的缓存
     * <pre>{@code Map<不支持的序列化的Type, 对应支持序列化的Type>}</pre>
     */
    static final ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<>(256);

    private SerializableTypeWrapper() {
    }

    /**
     * 返回可序列化的{@link Field#getGenericType()}
     */
    public static Type forField(Field field) {
        return forTypeProvider(new FieldTypeProvider(field));
    }

    /**
     * Return a {@link Serializable} variant of
     * {@link MethodParameter#getGenericParameterType()}.
     */
    public static Type forMethodParameter(MethodParameter methodParameter) {
        return forTypeProvider(new MethodParameterTypeProvider(methodParameter));
    }

    /**
     * 解除对{@link java.lang.reflect.Type}的封装，返回原始不支持序列化的{@link java.lang.reflect.Type}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Type> T unwrap(T type) {
        Type unwrapped = null;
        if (type instanceof SerializableTypeProxy) {
            unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
        }
        return (unwrapped != null ? (T) unwrapped : type);
    }

    /**
     * 返回一个可序列化类型的{@link java.lang.reflect.Type}
     */
    static Type forTypeProvider(TypeProvider provider) {
        Type providedType = provider.getType();
        if (providedType == null || providedType instanceof Serializable) {
            // No serializable type wrapping necessary (e.g. for java.lang.Class)
            return providedType;
        }
        // graalvm支持
        // noinspection ConstantConditions
        if (NativeDetector.inNativeImage() || !Serializable.class.isAssignableFrom(Class.class)) {
            // Let's skip any wrapping attempts if types are generally not serializable in
            // the current runtime environment (even java.lang.Class itself, e.g. on GraalVM native images)
            return providedType;
        }
        // Obtain a serializable type proxy for the given provider...
        Type cached = cache.get(providedType);
        if (cached != null) {
            return cached;
        }
        // 使用TypeProxyInvocationHandler创建SerializableTypeProxy动态代理(支持序列化的Type)
        for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
            if (type.isInstance(providedType)) {
                ClassLoader classLoader = provider.getClass().getClassLoader();
                Class<?>[] interfaces = new Class<?>[]{type, SerializableTypeProxy.class, Serializable.class};
                InvocationHandler handler = new TypeProxyInvocationHandler(provider);
                cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
                cache.put(providedType, cached);
                return cached;
            }
        }
        throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
    }

    /**
     * 用于JDK动态代理支持TypeProvider的接口
     */
    interface SerializableTypeProxy {
        /**
         * 返回{@link TypeProvider}
         */
        TypeProvider getTypeProvider();
    }

    /**
     * 支持序列化的{@link java.lang.reflect.Type}接口
     */
    interface TypeProvider extends Serializable {
        /**
         * 返回底层真实的{@link java.lang.reflect.Type}(不支持序列化)
         */
        Type getType();

        /**
         * 返回包装(type)的原始对象(source)，如果未知就返回{@code null}
         */
        default Object getSource() {
            return null;
        }
    }

    /**
     * SerializableTypeProxy的动态代理实现逻辑
     */
    private static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {
        private final TypeProvider provider;

        public TypeProxyInvocationHandler(TypeProvider provider) {
            this.provider = provider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 定义equals、hashCode、getTypeProvider函数的处理逻辑
            switch (method.getName()) {
                case "equals":
                    Object other = args[0];
                    // Unwrap proxies for speed
                    if (other instanceof Type) {
                        other = unwrap((Type) other);
                    }
                    return ObjectUtils.nullSafeEquals(this.provider.getType(), other);
                case "hashCode":
                    return ObjectUtils.nullSafeHashCode(this.provider.getType());
                case "getTypeProvider":
                    return this.provider;
            }
            // 当Type类原有函数返回类型是Type或Type[]且没有参数时，使用MethodInvokeTypeProvider且继续封装Type使其支持序列化
            if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
                return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
            } else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
                Type[] result = new Type[((Type[]) method.invoke(this.provider.getType())).length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
                }
                return result;
            }
            // 在原始对象上调用函数，实现Type类原有的功能
            try {
                return method.invoke(this.provider.getType(), args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    /**
     * 支持序列化的{@link java.lang.reflect.Field}，Field的序列化逻辑的实现
     */
    static class FieldTypeProvider implements TypeProvider {
        private final String fieldName;
        private final Class<?> declaringClass;
        private transient Field field;

        public FieldTypeProvider(Field field) {
            this.fieldName = field.getName();
            this.declaringClass = field.getDeclaringClass();
            this.field = field;
        }

        @Override
        public Type getType() {
            return this.field.getGenericType();
        }

        @Override
        public Object getSource() {
            return this.field;
        }

        private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            inputStream.defaultReadObject();
            try {
                this.field = this.declaringClass.getDeclaredField(this.fieldName);
            } catch (Throwable ex) {
                throw new IllegalStateException("Could not find original class structure", ex);
            }
        }
    }

    /**
     * 支持序列化的{@link MethodParameter}，MethodParameter的序列化逻辑的实现
     */
    static class MethodParameterTypeProvider implements TypeProvider {
        private final String methodName;
        private final Class<?>[] parameterTypes;
        private final Class<?> declaringClass;
        private final int parameterIndex;
        private transient MethodParameter methodParameter;

        public MethodParameterTypeProvider(MethodParameter methodParameter) {
            this.methodName = (methodParameter.getMethod() != null ? methodParameter.getMethod().getName() : null);
            this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
            this.declaringClass = methodParameter.getDeclaringClass();
            this.parameterIndex = methodParameter.getParameterIndex();
            this.methodParameter = methodParameter;
        }

        @Override
        public Type getType() {
            return this.methodParameter.getGenericParameterType();
        }

        @Override
        public Object getSource() {
            return this.methodParameter;
        }

        private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            inputStream.defaultReadObject();
            try {
                if (this.methodName != null) {
                    this.methodParameter = new MethodParameter(this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
                } else {
                    this.methodParameter = new MethodParameter(this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
                }
            } catch (Throwable ex) {
                throw new IllegalStateException("Could not find original class structure", ex);
            }
        }
    }

    /**
     * 当{@link java.lang.reflect.Type}类或其子类的无参函数返回的是Type或Type[]类型时，对这类函数封装以支持序列化
     */
    static class MethodInvokeTypeProvider implements TypeProvider {
        private final TypeProvider provider;
        private final String methodName;
        private final Class<?> declaringClass;
        private final int index;
        private transient Method method;
        private transient volatile Object result;

        public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
            this.provider = provider;
            this.methodName = method.getName();
            this.declaringClass = method.getDeclaringClass();
            this.index = index;
            this.method = method;
        }

        @Override
        public Type getType() {
            Object result = this.result;
            if (result == null) {
                // Lazy invocation of the target method on the provided type
                result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
                // Cache the result for further calls to getType()
                this.result = result;
            }
            return (result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result);
        }

        @Override
        public Object getSource() {
            return null;
        }

        private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            inputStream.defaultReadObject();
            Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
            if (method == null) {
                throw new IllegalStateException("Cannot find method on deserialization: " + this.methodName);
            }
            if (method.getReturnType() != Type.class && method.getReturnType() != Type[].class) {
                throw new IllegalStateException("Invalid return type on deserialized method - needs to be Type or Type[]: " + method);
            }
            this.method = method;
        }
    }
}
