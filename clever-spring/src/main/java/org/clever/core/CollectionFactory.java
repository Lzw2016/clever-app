package org.clever.core;

import org.clever.util.Assert;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;
import org.clever.util.ReflectionUtils;

import java.util.*;

/**
 * 集合类创建工厂，用于创建各种集合
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:48 <br/>
 */
public class CollectionFactory {
    private static final Set<Class<?>> approximableCollectionTypes = new HashSet<>();
    private static final Set<Class<?>> approximableMapTypes = new HashSet<>();

    static {
        // Standard collection interfaces
        approximableCollectionTypes.add(Collection.class);
        approximableCollectionTypes.add(List.class);
        approximableCollectionTypes.add(Set.class);
        approximableCollectionTypes.add(SortedSet.class);
        approximableCollectionTypes.add(NavigableSet.class);
        approximableMapTypes.add(Map.class);
        approximableMapTypes.add(SortedMap.class);
        approximableMapTypes.add(NavigableMap.class);

        // Common concrete collection classes
        approximableCollectionTypes.add(ArrayList.class);
        approximableCollectionTypes.add(LinkedList.class);
        approximableCollectionTypes.add(HashSet.class);
        approximableCollectionTypes.add(LinkedHashSet.class);
        approximableCollectionTypes.add(TreeSet.class);
        approximableCollectionTypes.add(EnumSet.class);
        approximableMapTypes.add(HashMap.class);
        approximableMapTypes.add(LinkedHashMap.class);
        approximableMapTypes.add(TreeMap.class);
        approximableMapTypes.add(EnumMap.class);
    }

    /**
     * 确定给定集合类型是否为可近似的类型，即{@link #createApproximateCollection}可以近似的类型
     */
    public static boolean isApproximableCollectionType(Class<?> collectionType) {
        return (collectionType != null && approximableCollectionTypes.contains(collectionType));
    }

    /**
     * 为给定集合创建最接近的集合
     *
     * @param collection 原始集合对象，可能为null
     * @param capacity   初始容量
     * @see #isApproximableCollectionType
     * @see java.util.LinkedList
     * @see java.util.ArrayList
     * @see java.util.EnumSet
     * @see java.util.TreeSet
     * @see java.util.LinkedHashSet
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> Collection<E> createApproximateCollection(Object collection, int capacity) {
        if (collection instanceof LinkedList) {
            return new LinkedList<>();
        } else if (collection instanceof List) {
            return new ArrayList<>(capacity);
        } else if (collection instanceof EnumSet) {
            // Cast is necessary for compilation in Eclipse 4.4.1.
            Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
            enumSet.clear();
            return enumSet;
        } else if (collection instanceof SortedSet) {
            return new TreeSet<>(((SortedSet<E>) collection).comparator());
        } else {
            return new LinkedHashSet<>(capacity);
        }
    }

    /**
     * 确定给定的映射类型是否是可近似的类型，即{@link #createApproximateMap}可以近似的类型
     */
    public static boolean isApproximableMapType(Class<?> mapType) {
        return (mapType != null && approximableMapTypes.contains(mapType));
    }

    /**
     * 为给定Map创建最接近的Map
     *
     * @param map      原始Map对象，可能为null
     * @param capacity 初始容量
     * @see #isApproximableMapType
     * @see java.util.EnumMap
     * @see java.util.TreeMap
     * @see java.util.LinkedHashMap
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Map<K, V> createApproximateMap(Object map, int capacity) {
        if (map instanceof EnumMap) {
            EnumMap enumMap = new EnumMap((EnumMap) map);
            enumMap.clear();
            return enumMap;
        } else if (map instanceof SortedMap) {
            return new TreeMap<>(((SortedMap<K, V>) map).comparator());
        } else {
            return new LinkedHashMap<>(capacity);
        }
    }

    /**
     * 根据给定的集合类型创建一个集合
     *
     * @param collectionType 目标集合的所需类型，不能为null
     * @param capacity       建议的初始容量
     */
    public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
        return createCollection(collectionType, null, capacity);
    }

    /**
     * 根据给定的集合类型创建一个集合，在使用枚举集合(EnumSet)时不保证集合泛型类型的安全性，需要指定正确的elementType值
     *
     * @param collectionType 目标集合的所需类型，不能为null
     * @param elementType    集合的元素类型，可以null，仅与枚举集合的创建有关
     * @param capacity       建议的初始容量
     */
    @SuppressWarnings({"unchecked", "cast", "SortedCollectionWithNonComparableKeys"})
    public static <E> Collection<E> createCollection(Class<?> collectionType, Class<?> elementType, int capacity) {
        Assert.notNull(collectionType, "Collection type must not be null");
        if (collectionType.isInterface()) {
            if (Set.class == collectionType || Collection.class == collectionType) {
                return new LinkedHashSet<>(capacity);
            } else if (List.class == collectionType) {
                return new ArrayList<>(capacity);
            } else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
                return new TreeSet<>();
            } else {
                throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
            }
        } else if (EnumSet.class.isAssignableFrom(collectionType)) {
            Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
            // Cast is necessary for compilation in Eclipse 4.4.1.
            return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
        } else {
            if (!Collection.class.isAssignableFrom(collectionType)) {
                throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
            }
            try {
                return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate Collection type: " + collectionType.getName(), ex);
            }
        }
    }

    /**
     * 为给定的Map类型创建最合适的Map集合
     *
     * @param mapType  Map集合的类型，不能为null
     * @param capacity 建议的初始容量
     */
    public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
        return createMap(mapType, null, capacity);
    }

    /**
     * 为给定的Map类型创建最合适的Map集合，如果所需的mapType为EnumMap，则无法保证类型安全，需要指定正确的mapType值
     *
     * @param mapType  Map集合的类型，不能为null
     * @param keyType  集合的key类型，可以null，仅与mapType为EnumMap的集合的创建有关
     * @param capacity 建议的初始容量
     */
    @SuppressWarnings({"rawtypes", "unchecked", "SortedCollectionWithNonComparableKeys"})
    public static <K, V> Map<K, V> createMap(Class<?> mapType, Class<?> keyType, int capacity) {
        Assert.notNull(mapType, "Map type must not be null");
        if (mapType.isInterface()) {
            if (Map.class == mapType) {
                return new LinkedHashMap<>(capacity);
            } else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
                return new TreeMap<>();
            } else if (MultiValueMap.class == mapType) {
                return new LinkedMultiValueMap();
            } else {
                throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
            }
        } else if (EnumMap.class == mapType) {
            Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
            return new EnumMap(asEnumType(keyType));
        } else {
            if (!Map.class.isAssignableFrom(mapType)) {
                throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
            }
            try {
                return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
            }
        }
    }

    /**
     * 创建一个{@link java.util.Properties}变体，自动将非字符串值适应{@link Properties#getProperty}中的字符串表示。
     * <p>此外，返回的{@code Properties}实例根据属性的键按字母数字对属性进行排序。
     *
     * @return 新{@code Properties}实例
     */
    public static Properties createStringAdaptingProperties() {
        return new SortedProperties(false) {
            @Override
            public String getProperty(String key) {
                Object value = get(key);
                return (value != null ? value.toString() : null);
            }
        };
    }

    /**
     * 将给定类型强制转换为Enum的子类型
     *
     * @param enumType 枚举类型，不能为null
     * @return 作为枚举子类型的给定类型
     * @throws IllegalArgumentException 如果给定类型不是枚举的子类型
     */
    @SuppressWarnings("rawtypes")
    private static Class<? extends Enum> asEnumType(Class<?> enumType) {
        Assert.notNull(enumType, "Enum type must not be null");
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
        }
        return enumType.asSubclass(Enum.class);
    }
}
