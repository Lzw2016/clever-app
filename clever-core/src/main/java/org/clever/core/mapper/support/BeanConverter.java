package org.clever.core.mapper.support;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.beans.BeanCopier;
import net.sf.cglib.core.Converter;
import org.apache.commons.lang3.ClassUtils;
import org.clever.core.converter.TypeConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JavaBean与JavaBean之间的转换，使用cglib的BeanCopier实现(性能极好)<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2019/12/19 10:49 <br/>
 */
@Slf4j
public class BeanConverter {
    /**
     * 自定义的cglib Converter
     */
    private final ConverterAdapter converter;

    public BeanConverter() {
        this(Collections.emptyList());
    }

    public BeanConverter(List<TypeConverter> converters) {
        this.converter = new ConverterAdapter(converters == null ? Collections.emptyList() : converters);
    }

    /**
     * 将对象source的值拷贝到对象target中<br/>
     * <b>注意：此操作source中的属性会覆盖target中的属性，无论source中的属性是不是空的</b><br/>
     *
     * @param source 数据源对象
     * @param target 目标对象
     */
    public void copyTo(Object source, Object target) {
        // TODO 可以缓存 BeanCopier 对象
        BeanCopier beanCopier = BeanCopier.create(source.getClass(), target.getClass(), true);
        beanCopier.copy(source, target, converter);
    }

    /**
     * 不同类型的JavaBean对象转换<br/>
     *
     * @param source      数据源JavaBean
     * @param targetClass 需要转换之后的JavaBean类型
     * @param <T>         需要转换之后的JavaBean类型
     * @return 转换之后的对象
     */
    public <T> T mapper(Object source, Class<T> targetClass) {
        T result;
        try {
            result = targetClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(String.format("创建对象实例[%s]失败，缺少默认的构造函数", targetClass.getName()), e);
        }
        copyTo(source, result);
        return result;
    }

    public BeanConverter registerConverter(TypeConverter converter) {
        this.converter.registerConverter(converter);
        return this;
    }

    /**
     * 创建一个新的 BeanConverter 扩展默认的 converter
     */
    public BeanConverter newExtBeanConverter(TypeConverter... typeConverters) {
        BeanConverter extBeanConverter = new BeanConverter(Arrays.asList(typeConverters));
        for (ResolvedTypeConverter resolvedTypeConverter : this.converter.converterList) {
            if (!extBeanConverter.converter.converterList.contains(resolvedTypeConverter)) {
                extBeanConverter.converter.converterList.add(resolvedTypeConverter);
            }
        }
        return extBeanConverter;
    }

    /**
     * cglib Converter 适配器 (代理和缓存所有真正的转换类型)
     */
    static class ConverterAdapter implements Converter {
        /**
         * 真实的类型转换工具
         */
        private final List<ResolvedTypeConverter> converterList;

        ConverterAdapter(List<TypeConverter> converterList) {
            Objects.requireNonNull(converterList, "converterList must not be null");
            this.converterList = converterList.stream()
                    .map(this::resolveTypeConverter)
                    .collect(Collectors.toList());
        }

        private ResolvedTypeConverter resolveTypeConverter(TypeConverter converter) {
            return new ResolvedTypeConverter(converter);
        }

        public void registerConverter(TypeConverter converter) {
            if (converter == null) {
                return;
            }
            ResolvedTypeConverter resolvedTypeConverter = resolveTypeConverter(converter);
            if (!converterList.contains(resolvedTypeConverter)) {
                converterList.add(resolvedTypeConverter);
            }
        }

        @Override
        public Object convert(Object source, Class targetType, Object context) {
            if (source == null) {
                return null;
            }
            // 得到 source 对象类型
            Class<?> sourceType = source.getClass();
            if (ClassUtils.isAssignable(sourceType, targetType, true)) {
                // 类型相同直接返回
                return source;
            }
            for (ResolvedTypeConverter resolvedTypeConverter : converterList) {
                if (!resolvedTypeConverter.support(source, targetType)) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug(
                            "[ConverterAdapter] 自定义类型转换 sourceType=[{}] --> targetType=[{}] | 转换器[{}]",
                            sourceType.getName(),
                            targetType.getName(),
                            resolvedTypeConverter.delegatingConverter.getClass().getName()
                    );
                }
                return resolvedTypeConverter.convert(source, targetType, context);
            }
            return null;
        }
    }

    /**
     * TypeConverter 实现
     */
    static class ResolvedTypeConverter implements TypeConverter {
        /**
         * 真实的 TypeConverter 实现对象(委托 TypeConverter)
         */
        private final TypeConverter delegatingConverter;

        /**
         * @param delegatingConverter 真实的 TypeConverter 实现对象(委托 TypeConverter)
         */
        ResolvedTypeConverter(TypeConverter delegatingConverter) {
            Objects.requireNonNull(delegatingConverter, "delegatingConverter must not be null");
            this.delegatingConverter = delegatingConverter;
        }

        @Override
        public boolean support(Object source, Class<?> targetType) {
            return delegatingConverter.support(source, targetType);
        }

        @Override
        public Object convert(Object source, Class<?> targetType, Object context) {
            return delegatingConverter.convert(source, targetType, context);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ResolvedTypeConverter that = (ResolvedTypeConverter) o;
            // noinspection ConstantConditions
            if (delegatingConverter == null || that.delegatingConverter == null) {
                return false;
            }
            return delegatingConverter.getClass().getName().equals(that.delegatingConverter.getClass().getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegatingConverter.getClass().getName());
        }
    }
}
