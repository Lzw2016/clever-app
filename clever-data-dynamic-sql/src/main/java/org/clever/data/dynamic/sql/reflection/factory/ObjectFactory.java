package org.clever.data.dynamic.sql.reflection.factory;

import java.util.List;
import java.util.Properties;

public interface ObjectFactory {
    /**
     * 设置配置属性
     *
     * @param properties 配置属性
     */
    default void setProperties(Properties properties) {
    }

    /**
     * 使用默认构造函数创建新对象
     *
     * @param type 对象类型
     */
    <T> T create(Class<T> type);

    /**
     * 使用指定的构造函数和参数创建新对象
     *
     * @param type                对象类型
     * @param constructorArgTypes 构造函数参数类型
     * @param constructorArgs     构造函数参数值
     */
    <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

    /**
     * 判断类型是否是集合类型
     *
     * @param type 对象类型
     */
    <T> boolean isCollection(Class<T> type);
}
