package org.clever.boot.util;

import org.clever.core.annotation.AnnotationAwareOrderComparator;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 通过注入可用参数来实例化对象的简单工厂。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:29 <br/>
 */
public class Instantiator<T> {
    private static final Comparator<Constructor<?>> CONSTRUCTOR_COMPARATOR = Comparator.<Constructor<?>>comparingInt(Constructor::getParameterCount).reversed();

    private final Class<?> type;
    private final Map<Class<?>, Function<Class<?>, Object>> availableParameters;

    /**
     * 为给定类型创建一个新的 {@link Instantiator} 实例。
     *
     * @param type                要实例化的类型
     * @param availableParameters 用于注册可用参数的使用者
     */
    public Instantiator(Class<?> type, Consumer<AvailableParameters> availableParameters) {
        this.type = type;
        this.availableParameters = getAvailableParameters(availableParameters);
    }

    private Map<Class<?>, Function<Class<?>, Object>> getAvailableParameters(Consumer<AvailableParameters> availableParameters) {
        Map<Class<?>, Function<Class<?>, Object>> result = new LinkedHashMap<>();
        availableParameters.accept(new AvailableParameters() {
            @Override
            public void add(Class<?> type, Object instance) {
                result.put(type, (factoryType) -> instance);
            }

            @Override
            public void add(Class<?> type, Function<Class<?>, Object> factory) {
                result.put(type, factory);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 实例化给定的类名集，必要时注入构造函数参数。
     *
     * @param names 要实例化的类名
     * @return 实例化实例的列表
     */
    public List<T> instantiate(Collection<String> names) {
        return instantiate((ClassLoader) null, names);
    }

    /**
     * 实例化给定的类名集，必要时注入构造函数参数。
     *
     * @param classLoader 源类加载器
     * @param names       要实例化的类名
     * @return 实例化实例的列表
     */
    public List<T> instantiate(ClassLoader classLoader, Collection<String> names) {
        Assert.notNull(names, "Names must not be null");
        return instantiate(names.stream().map((name) -> TypeSupplier.forName(classLoader, name)));
    }

    /**
     * 实例化给定的类集，必要时注入构造函数参数。
     *
     * @param types 要实例化的类型
     * @return 实例化实例的列表
     */
    public List<T> instantiateTypes(Collection<Class<?>> types) {
        Assert.notNull(types, "Types must not be null");
        return instantiate(types.stream().map(TypeSupplier::forType));
    }

    private List<T> instantiate(Stream<TypeSupplier> typeSuppliers) {
        List<T> instances = typeSuppliers.map(this::instantiate).collect(Collectors.toList());
        AnnotationAwareOrderComparator.sort(instances);
        return Collections.unmodifiableList(instances);
    }

    private T instantiate(TypeSupplier typeSupplier) {
        try {
            Class<?> type = typeSupplier.get();
            Assert.isAssignable(this.type, type);
            return instantiate(type);
        } catch (Throwable ex) {
            throw new IllegalArgumentException(
                    "Unable to instantiate " + this.type.getName() + " [" + typeSupplier.getName() + "]", ex
            );
        }
    }

    @SuppressWarnings("unchecked")
    private T instantiate(Class<?> type) throws Exception {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        Arrays.sort(constructors, CONSTRUCTOR_COMPARATOR);
        for (Constructor<?> constructor : constructors) {
            Object[] args = getArgs(constructor.getParameterTypes());
            if (args != null) {
                ReflectionUtils.makeAccessible(constructor);
                return (T) constructor.newInstance(args);
            }
        }
        throw new IllegalAccessException("Unable to find suitable constructor");
    }

    private Object[] getArgs(Class<?>[] parameterTypes) {
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Function<Class<?>, Object> parameter = getAvailableParameter(parameterTypes[i]);
            if (parameter == null) {
                return null;
            }
            args[i] = parameter.apply(this.type);
        }
        return args;
    }

    private Function<Class<?>, Object> getAvailableParameter(Class<?> parameterType) {
        for (Map.Entry<Class<?>, Function<Class<?>, Object>> entry : this.availableParameters.entrySet()) {
            if (entry.getKey().isAssignableFrom(parameterType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 用于注册可用参数的回调
     */
    public interface AvailableParameters {
        /**
         * 添加具有实例值的参数
         *
         * @param type     参数类型
         * @param instance 应注入的实例
         */
        void add(Class<?> type, Object instance);

        /**
         * 添加带有实例工厂的参数
         *
         * @param type    参数类型
         * @param factory 用于创建应注入的实例的工厂
         */
        void add(Class<?> type, Function<Class<?>, Object> factory);
    }

    /**
     * 提供类类型的 {@link Supplier}
     */
    private interface TypeSupplier {
        String getName();

        Class<?> get() throws ClassNotFoundException;

        static TypeSupplier forName(ClassLoader classLoader, String name) {
            return new TypeSupplier() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Class<?> get() throws ClassNotFoundException {
                    return ClassUtils.forName(name, classLoader);
                }
            };
        }

        static TypeSupplier forType(Class<?> type) {
            return new TypeSupplier() {
                @Override
                public String getName() {
                    return type.getName();
                }

                @Override
                public Class<?> get() {
                    return type;
                }
            };
        }
    }
}
