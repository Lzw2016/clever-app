package org.clever.beans;

/**
 * 用于获取{@link PropertyAccessor}实例的简单工厂外观，特别是{@link BeanWrapper}实例。
 * 隐藏实际的目标实现类及其扩展的公共签名。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:56 <br/>
 */
public final class PropertyAccessorFactory {
    private PropertyAccessorFactory() {
    }

    /**
     * 获取给定目标对象的BeanWrapper，访问JavaBeans风格的属性。
     *
     * @param target 要包裹的目标对象
     * @return 属性访问器
     * @see BeanWrapperImpl
     */
    public static BeanWrapper forBeanPropertyAccess(Object target) {
        return new BeanWrapperImpl(target);
    }

    /**
     * 获取给定目标对象的PropertyAccessor，以直接字段样式访问属性。
     * @param target 要包装的目标对象
     * @return 属性访问器
     * @see DirectFieldAccessor
     */
    public static ConfigurablePropertyAccessor forDirectFieldAccess(Object target) {
        return new DirectFieldAccessor(target);
    }
}
