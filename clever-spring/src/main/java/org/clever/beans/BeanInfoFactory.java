package org.clever.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

/**
 * 用于为bean创建{@link BeanInfo}实例的策略接口。<br/>
 * 可以用于插入自定义bean属性解析策略(例如针对JVM上的其他语言)或更高效的{@link BeanInfo}检索算法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:44 <br/>
 *
 * @see CachedIntrospectionResults
 */
public interface BeanInfoFactory {
    /**
     * 如果支持，返回给定类的bean信息
     *
     * @param beanClass bean类
     * @throws IntrospectionException 在例外情况下
     */
    BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;
}
