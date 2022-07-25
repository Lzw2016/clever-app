package org.clever.core.mapper;

import lombok.SneakyThrows;
import org.clever.beans.BeanUtils;
import org.clever.beans.BeanWrapper;
import org.clever.beans.BeanWrapperImpl;
import org.clever.format.support.DefaultFormattingConversionService;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;

/**
 * JavaBean工具<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/01 21:49 <br/>
 *
 * @see BeanUtils
 * @see BeanMapper
 */
public class BeanCopyUtils {
    private static final DefaultFormattingConversionService CONVERSION_SERVICE = new DefaultFormattingConversionService();

    /**
     * 获取JavaBean的字段名
     *
     * @param source JavaBean对象
     * @param filter 过滤回调函数
     */
    public static Set<String> getBeanPropertyNames(Object source, BiFunction<String, Object, Boolean> filter) {
        final BeanWrapper srcWrapper = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = srcWrapper.getPropertyDescriptors();
        Set<String> propertyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String name = pd.getName();
            Object value = srcWrapper.getPropertyValue(name);
            boolean needAdd = true;
            if (filter != null) {
                needAdd = Optional.ofNullable(filter.apply(name, value)).orElse(true);
            }
            if (needAdd) {
                propertyNames.add(name);
            }
        }
        // propertyNames.toArray(new String[0]);
        return propertyNames;
    }

    /**
     * 将对象source的值拷贝到对象target中
     *
     * @param source              数据源对象
     * @param target              目标对象
     * @param ignoreSourceNull    是否忽略source对象的null字段
     * @param ignoreTargetNotNull 是否忽略target对象的非null字段
     */
    public static void copyTo(Object source, Object target, boolean ignoreSourceNull, boolean ignoreTargetNotNull) {
        Set<String> propertyNames = new HashSet<>();
        if (ignoreSourceNull) {
            propertyNames.addAll(getBeanPropertyNames(source, (name, value) -> value == null));
        }
        if (ignoreTargetNotNull) {
            propertyNames.addAll(getBeanPropertyNames(target, (name, value) -> value != null));
        }
        BeanUtils.copyProperties(source, target, propertyNames.toArray(new String[0]));
    }

    /**
     * 将对象source的值拷贝到对象target中
     *
     * @param source           数据源对象
     * @param target           目标对象
     * @param ignoreSourceNull 是否忽略source对象的null字段
     */
    public static void copyTo(Object source, Object target, boolean ignoreSourceNull) {
        copyTo(source, target, ignoreSourceNull, false);
    }

    /**
     * 将对象source的值拷贝到对象target中
     *
     * @param source 数据源对象
     * @param target 目标对象
     */
    public static void copyTo(Object source, Object target) {
        copyTo(source, target, false, false);
    }

    /**
     * 将对象source的值拷贝到对象target中
     *
     * @param source           数据源对象
     * @param target           目标对象
     * @param ignoreProperties 忽略的字段名
     */
    public static void copyTo(Object source, Object target, String... ignoreProperties) {
        BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    /**
     * 将对象source的值拷贝到对象target中
     *
     * @param source           数据源对象
     * @param target           目标对象
     * @param ignoreProperties 忽略的字段名
     */
    public static void copyTo(Object source, Object target, Set<String> ignoreProperties) {
        if (ignoreProperties != null) {
            BeanUtils.copyProperties(source, target, ignoreProperties.toArray(new String[0]));
        } else {
            BeanUtils.copyProperties(source, target);
        }
    }

    /**
     * 将多个source对象的值拷贝到对象target中<br/>
     *
     * @param target    目标对象
     * @param sourceArr 数据源对象数组
     */
    public static void copyTo(Object target, Object... sourceArr) {
        if (target == null || sourceArr == null) {
            return;
        }
        for (Object source : sourceArr) {
            copyTo(source, target, true, true);
        }
    }

    /**
     * 把多个JavaBean对象转换成另一个JavaBean对象<br/>
     *
     * @param targetClass 需要转换之后的JavaBean类型
     * @param sourceArr   数据源JavaBean
     * @param <T>         需要转换之后的JavaBean类型
     * @return 转换之后的对象
     */
    @SneakyThrows
    public static <T> T mapper(Class<T> targetClass, Object... sourceArr) {
        if (targetClass == null || sourceArr == null) {
            return null;
        }
        T target = targetClass.newInstance();
        for (Object source : sourceArr) {
            copyTo(source, target, true, true);
        }
        return target;
    }

    /**
     * JavaBean转Map
     *
     * @param source 数据源对象
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, Object> toMap(Object source) {
        if (source == null) {
            return Collections.emptyMap();
        }
        if (source instanceof Map) {
            return (Map) source;
        }
        Map<String, Object> map = new HashMap<>();
        getBeanPropertyNames(source, (name, value) -> {
            if (value instanceof Class && "class".equals(name)) {
                return false;
            }
            map.put(name, value);
            return false;
        });
        return map;
    }

    /**
     * Map转JavaBean(类型一致才能转换)
     *
     * @param map                 数据源map
     * @param target              目标对象
     * @param ignoreMapNull       是否忽略map的null值
     * @param ignoreTargetNotNull 是否忽略target对象的非null字段
     */
    public static void toBean(Map<String, Object> map, Object target, boolean ignoreMapNull, boolean ignoreTargetNotNull) {
        final BeanWrapper srcWrapper = new BeanWrapperImpl(target);
        PropertyDescriptor[] pds = srcWrapper.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            String name = pd.getName();
            Object mapValue = map.get(name);
            if (ignoreMapNull && mapValue == null) {
                continue;
            }
            Object value = srcWrapper.getPropertyValue(name);
            if (ignoreTargetNotNull && value != null) {
                continue;
            }
            if (value instanceof Class && "class".equals(name)) {
                continue;
            }
            if (mapValue == null) {
                srcWrapper.setPropertyValue(name, null);
                continue;
            }
            Class<?> valueType = srcWrapper.getPropertyType(name);
            if (valueType == null) {
                continue;
            }
            Class<?> mapValueType = mapValue.getClass();
            if (valueType.isAssignableFrom(mapValueType)) {
                srcWrapper.setPropertyValue(name, mapValue);
            } else if (BigDecimal.class.isAssignableFrom(mapValueType) && Number.class.isAssignableFrom(valueType)) {
                srcWrapper.setPropertyValue(name, mapValue);
            } else if (CONVERSION_SERVICE.canConvert(mapValueType, valueType)) {
                srcWrapper.setPropertyValue(name, CONVERSION_SERVICE.convert(mapValue, valueType));
            }
        }
    }

    /**
     * Map转JavaBean(类型一致才能转换)
     *
     * @param map           数据源map
     * @param target        目标对象
     * @param ignoreMapNull 是否忽略map的null值
     */
    public static void toBean(Map<String, Object> map, Object target, boolean ignoreMapNull) {
        toBean(map, target, ignoreMapNull, false);
    }

    /**
     * Map转JavaBean(类型一致才能转换)
     *
     * @param map    数据源map
     * @param target 目标对象
     */
    public static void toBean(Map<String, Object> map, Object target) {
        toBean(map, target, false, false);
    }

    /**
     * Map转JavaBean(类型一致才能转换)
     *
     * @param map         数据源Map
     * @param targetClass 需要转换之后的JavaBean类型
     * @param <T>         需要转换之后的JavaBean类型
     */
    @SneakyThrows
    public static <T> T toBean(Map<String, Object> map, Class<T> targetClass) {
        if (targetClass == null || map == null) {
            return null;
        }
        T target = targetClass.newInstance();
        toBean(map, target);
        return target;
    }
}
