package org.clever.core;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 断言工具类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/17 23:25 <br/>
 */
public class Assert {
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isFalse(boolean expression, Supplier<String> messageSupplier) {
        if (expression) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void isNotBlank(String text, String message) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isNotBlank(String text, Supplier<String> messageSupplier) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void isBlank(String text, String message) {
        if (StringUtils.isNotBlank(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isBlank(String text, Supplier<String> messageSupplier) {
        if (StringUtils.isNotBlank(text)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void doesNotContain(String textToSearch, String substring, String message) {
        if ((textToSearch != null
            && !textToSearch.isEmpty())
            && (substring != null
            && !substring.isEmpty())
            && textToSearch.contains(substring)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (CollectionUtils.isEmpty(map)) {
            throw new IllegalArgumentException(message);
        }
    }

    @SuppressWarnings("rawtypes")
    private static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof Optional) {
            return ((Optional) obj).isEmpty();
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).isEmpty();
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        }
        // else
        return false;
    }

    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isNull(Object object, Supplier<String> messageSupplier) {
        if (object != null) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public static void state(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalStateException(nullSafeGet(messageSupplier));
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void hasText(String text, String message) {
        if (!org.springframework.util.StringUtils.hasText(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void hasText(String text, Supplier<String> messageSupplier) {
        if (!org.springframework.util.StringUtils.hasText(text)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    public static void hasLength(String text, String message) {
        if (!org.springframework.util.StringUtils.hasLength(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * obj对象能强转为type类型
     */
    public static void isInstanceOf(Class<?> type, Object obj, Supplier<String> messageSupplier) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
        }
    }

    /**
     * obj对象能强转为type类型
     */
    public static void isInstanceOf(Class<?> type, Object obj, String message) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, message);
        }
    }

    /**
     * obj对象能强转为type类型
     */
    public static void isInstanceOf(Class<?> type, Object obj) {
        isInstanceOf(type, obj, "");
    }

    /**
     * subType是superType的子类或者子接口
     */
    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, "");
    }

    /**
     * subType是superType的子类或者子接口
     */
    public static void isAssignable(Class<?> superType, Class<?> subType, String message) {
        notNull(superType, "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, message);
        }
    }

    /**
     * subType是superType的子类或者子接口
     */
    public static void isAssignable(Class<?> superType, Class<?> subType, Supplier<String> messageSupplier) {
        notNull(superType, "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
        }
    }

    /**
     * 数组不为null，也不是空数组
     */
    public static void notEmpty(Object[] array, String message) {
        if (ObjectUtils.isEmpty(array)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数组不包含null元素。
     * <p>注意：如果阵列为空，则不会抱怨！
     * <pre>{@code
     * Assert.noNullElements(array, "The array must contain non-null elements");
     * }</pre>
     *
     * @param array   要检查的数组
     * @param message 断言失败时要使用的异常消息
     * @throws IllegalArgumentException 如果对象数组包含null元素
     */
    public static void noNullElements(Object[] array, String message) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * 断言集合不包含 {@code null} 元素
     * <p>注意：如果集合为空，则无需投诉！
     * <pre>{@code
     * Assert.noNullElements(collection, "Collection must contain non-null elements");
     * }</pre>
     *
     * @param collection 要检查的集合
     * @param message    断言失败时要使用的异常消息
     * @throws IllegalArgumentException 如果集合包含 {@code null} 元素
     */
    public static void noNullElements(Collection<?> collection, String message) {
        if (collection != null) {
            for (Object element : collection) {
                if (element == null) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    private static String nullSafeGet(Supplier<String> messageSupplier) {
        return (messageSupplier != null ? messageSupplier.get() : null);
    }

    private static void instanceCheckFailed(Class<?> type, Object obj, String msg) {
        String className = (obj != null ? obj.getClass().getName() : "null");
        String result = "";
        boolean defaultMessage = true;
        if (org.springframework.util.StringUtils.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, className);
                defaultMessage = false;
            }
        }
        if (defaultMessage) {
            result = result + ("Object of class [" + className + "] must be an instance of " + type);
        }
        throw new IllegalArgumentException(result);
    }

    private static void assignableCheckFailed(Class<?> superType, Class<?> subType, String msg) {
        String result = "";
        boolean defaultMessage = true;
        if (org.springframework.util.StringUtils.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, subType);
                defaultMessage = false;
            }
        }
        if (defaultMessage) {
            result = result + (subType + " is not assignable to " + superType);
        }
        throw new IllegalArgumentException(result);
    }

    private static boolean endsWithSeparator(String msg) {
        return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
    }

    private static String messageWithTypeName(String msg, Object typeName) {
        return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
    }
}
