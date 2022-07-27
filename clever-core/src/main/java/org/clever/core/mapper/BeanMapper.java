package org.clever.core.mapper;

import org.clever.core.converter.StringToDateConverter;
import org.clever.core.converter.StringToNumberConverter;
import org.clever.core.converter.TypeConverter;
import org.clever.core.mapper.support.BeanConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JavaBean与JavaBean之间的转换，使用cglib的BeanCopier实现(性能极好)<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2019/12/19 14:30 <br/>
 */
public class BeanMapper {
    private static final BeanConverter BEAN_CONVERTER = new BeanConverter();

    static {
        BEAN_CONVERTER.registerConverter(new StringToDateConverter())
                .registerConverter(new StringToNumberConverter());
    }

    public static void registerConverter(TypeConverter converter) {
        BEAN_CONVERTER.registerConverter(converter);
    }

    /**
     * 将对象source的值拷贝到对象target中<br/>
     * <b>注意：此操作source中的属性会覆盖target中的属性，无论source中的属性是不是空的</b><br/>
     *
     * @param source 数据源对象
     * @param target 目标对象
     */
    public static void copyTo(Object source, Object target) {
        BEAN_CONVERTER.copyTo(source, target);
    }

    /**
     * 将对象source的值拷贝到对象target中<br/>
     * <b>注意：此操作source中的属性会覆盖target中的属性，无论source中的属性是不是空的</b><br/>
     *
     * @param source         数据源对象
     * @param target         目标对象
     * @param typeConverters 自定义转换函数
     */
    public static void copyTo(Object source, Object target, TypeConverter... typeConverters) {
        BeanConverter tmp = BEAN_CONVERTER.newExtBeanConverter(typeConverters);
        tmp.copyTo(source, target);
    }

    /**
     * 不同类型的JavaBean对象转换<br/>
     *
     * @param source      数据源JavaBean
     * @param targetClass 需要转换之后的JavaBean类型
     * @param <T>         需要转换之后的JavaBean类型
     * @return 转换之后的对象
     */
    public static <T> T mapper(Object source, Class<T> targetClass) {
        return BEAN_CONVERTER.mapper(source, targetClass);
    }

    /**
     * 不同类型的JavaBean对象转换<br/>
     *
     * @param source         数据源JavaBean
     * @param targetClass    需要转换之后的JavaBean类型
     * @param typeConverters 自定义转换函数
     * @param <T>            需要转换之后的JavaBean类型
     * @return 转换之后的对象
     */
    public static <T> T mapper(Object source, Class<T> targetClass, TypeConverter... typeConverters) {
        BeanConverter tmp = BEAN_CONVERTER.newExtBeanConverter(typeConverters);
        return tmp.mapper(source, targetClass);
    }

    /**
     * 不同类型的JavaBean对象转换，基于Collection(集合)的批量转换<br/>
     * 如：List&lt;ThisBean&gt; <---->  List&lt;OtherBean&gt;<br/>
     *
     * @param sources     数据源JavaBean集合
     * @param targetClass 需要转换之后的JavaBean类型
     * @param <T>         需要转换之后的JavaBean类型
     * @return 转换之后的对象集合
     */
    public static <T> List<T> mapper(Collection<?> sources, Class<T> targetClass) {
        if (sources == null) {
            return null;
        }
        List<T> destinationList = new ArrayList<>(sources.size());
        for (Object sourceObject : sources) {
            T target = mapper(sourceObject, targetClass);
            destinationList.add(target);
        }
        return destinationList;
    }
}
