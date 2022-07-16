package org.clever.core.style;

import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 使用{@code toString}样式约定将对象转换为字符串形式，通常用于调试目的。
 *
 * <p>使用引擎盖下方的反射访客图案，很好地封装了每种样式对象的样式算法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:41 <br/>
 */
public class DefaultValueStyler implements ValueStyler {
    private static final String EMPTY = "[[empty]]";
    private static final String NULL = "[null]";
    private static final String COLLECTION = "collection";
    private static final String SET = "set";
    private static final String LIST = "list";
    private static final String MAP = "map";
    private static final String EMPTY_MAP = MAP + EMPTY;
    private static final String ARRAY = "array";

    @Override
    public String style(Object value) {
        if (value == null) {
            return NULL;
        } else if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof Class) {
            return ClassUtils.getShortName((Class<?>) value);
        } else if (value instanceof Method) {
            Method method = (Method) value;
            return method.getName() + "@" + ClassUtils.getShortName(method.getDeclaringClass());
        } else if (value instanceof Map) {
            return style((Map<?, ?>) value);
        } else if (value instanceof Map.Entry) {
            return style((Map.Entry<?, ?>) value);
        } else if (value instanceof Collection) {
            return style((Collection<?>) value);
        } else if (value.getClass().isArray()) {
            return styleArray(ObjectUtils.toObjectArray(value));
        } else {
            return String.valueOf(value);
        }
    }

    private <K, V> String style(Map<K, V> value) {
        if (value.isEmpty()) {
            return EMPTY_MAP;
        }
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Map.Entry<K, V> entry : value.entrySet()) {
            result.add(style(entry));
        }
        return MAP + result;
    }

    private String style(Map.Entry<?, ?> value) {
        return style(value.getKey()) + " -> " + style(value.getValue());
    }

    private String style(Collection<?> value) {
        String collectionType = getCollectionTypeString(value);
        if (value.isEmpty()) {
            return collectionType + EMPTY;
        }
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Object o : value) {
            result.add(style(o));
        }
        return collectionType + result;
    }

    private String getCollectionTypeString(Collection<?> value) {
        if (value instanceof List) {
            return LIST;
        } else if (value instanceof Set) {
            return SET;
        } else {
            return COLLECTION;
        }
    }

    private String styleArray(Object[] array) {
        if (array.length == 0) {
            return ARRAY + '<' + ClassUtils.getShortName(array.getClass().getComponentType()) + '>' + EMPTY;
        }
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Object o : array) {
            result.add(style(o));
        }
        return ARRAY + '<' + ClassUtils.getShortName(array.getClass().getComponentType()) + '>' + result;
    }
}
