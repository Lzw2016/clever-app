package org.clever.core;

import org.clever.util.Assert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 此类的目的是支持捕获和传递泛型类型<br/>
 * 为了捕获泛型类型并在运行时保留它，您需要创建一个子类（理想情况下为匿名内联类），如下所示:
 * <pre>{@code
 * ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<List<String>>() {};
 * }</pre>
 * 然后，可以使用生成的typeRef实例获取一个类型实例，该实例在运行时携带捕获的参数化类型信息<br/>
 * 有关“super type tokens”的更多信息，请参阅Neal Gafter's博客<br/>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 11:59 <br/>
 */
public abstract class ParameterizedTypeReference<T> {
    private final Type type;

    protected ParameterizedTypeReference() {
        Class<?> parameterizedTypeReferenceSubclass = findParameterizedTypeReferenceSubclass(getClass());
        Type type = parameterizedTypeReferenceSubclass.getGenericSuperclass();
        Assert.isInstanceOf(ParameterizedType.class, type, "Type must be a parameterized type");
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");
        this.type = actualTypeArguments[0];
    }

    private ParameterizedTypeReference(Type type) {
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ParameterizedTypeReference && this.type.equals(((ParameterizedTypeReference<?>) other).type));
    }

    @Override
    public int hashCode() {
        return this.type.hashCode();
    }

    @Override
    public String toString() {
        return "ParameterizedTypeReference<" + this.type + ">";
    }

    /**
     * 构建一个包装给定类型的{@code ParameterizedTypeReference}
     *
     * @param type 泛型类型(可能通过反射获得，例如从{@link java.lang.reflect.Method#getGenericReturnType()}获得)
     */
    public static <T> ParameterizedTypeReference<T> forType(Type type) {
        return new ParameterizedTypeReference<T>(type) {
        };
    }

    private static Class<?> findParameterizedTypeReferenceSubclass(Class<?> child) {
        Class<?> parent = child.getSuperclass();
        if (Object.class == parent) {
            throw new IllegalStateException("Expected ParameterizedTypeReference superclass");
        } else if (ParameterizedTypeReference.class == parent) {
            return child;
        } else {
            return findParameterizedTypeReferenceSubclass(parent);
        }
    }
}
