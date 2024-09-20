package org.clever.core;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 21:46 <br/>
 */
public class AppContextHolder {
    private static final AppContext APP_CONTEXT = new AppContext();

    /**
     * 获取bean对象
     *
     * @param beanName bean名称
     * @return bean不存在就返回null
     */
    public static <T> T getBean(String beanName) {
        return APP_CONTEXT.getBean(beanName);
    }

    /**
     * 获取bean对象
     *
     * @param beanName bean名称
     * @param required false: bean不存在就返回null; true: bean不存在就抛出异常
     */
    public static <T> T getBean(String beanName, boolean required) {
        return APP_CONTEXT.getBean(beanName, required);
    }

    /**
     * 获取bean对象
     *
     * @param beanName     bean名称
     * @param requiredType bean类型
     * @return bean不存在就返回null
     */
    public static <T> T getBean(String beanName, Class<T> requiredType) {
        return APP_CONTEXT.getBean(beanName, requiredType);
    }

    /**
     * 获取bean对象
     *
     * @param beanName     bean名称
     * @param requiredType bean类型
     * @param required     false: bean不存在就返回null; true: bean不存在就抛出异常
     */
    public static <T> T getBean(String beanName, Class<T> requiredType, boolean required) {
        return APP_CONTEXT.getBean(beanName, requiredType, required);
    }

    /**
     * 获取bean对象
     *
     * @param requiredType bean类型
     * @return bean不存在就返回null
     */
    public static <T> T getBean(Class<T> requiredType) {
        return APP_CONTEXT.getBean(requiredType);
    }

    /**
     * 获取bean对象
     *
     * @param requiredType bean类型
     * @param required     false: bean不存在就返回null; true: bean不存在就抛出异常
     */
    public static <T> T getBean(Class<T> requiredType, boolean required) {
        return APP_CONTEXT.getBean(requiredType, required);
    }

    /**
     * 获取bean对象
     *
     * @param requiredType bean类型
     * @return bean不存在就返回空集合
     */
    public static <T> List<T> getBeans(Class<T> requiredType) {
        return APP_CONTEXT.getBeans(requiredType);
    }

    /**
     * bean对象是否存在
     *
     * @param beanName bean类型
     */
    public static boolean containsBean(String beanName) {
        return APP_CONTEXT.containsBean(beanName);
    }

    /**
     * 获取bean类型
     *
     * @param beanName bean名称
     * @return bean不存在就返回null
     */
    public static Class<?> getBeanType(String beanName) {
        return APP_CONTEXT.getBeanType(beanName);
    }

    /**
     * bean数量
     */
    public static int getBeanCount() {
        return APP_CONTEXT.getBeanCount();
    }

    /**
     * 获取所有注册的bean名称
     */
    public static String[] getBeanNames() {
        return APP_CONTEXT.getBeanNames();
    }

    /**
     * 获取所有指定类型的bean名称
     *
     * @param type bean类型
     * @return bean不存在就返回空数组
     */
    public static String[] getBeanNamesForType(Class<?> type) {
        return APP_CONTEXT.getBeanNamesForType(type);
    }

    /**
     * 注册一个Bean对象
     *
     * @param beanName bean名称
     * @param bean     bean对象
     * @param primary  当前注册的bean是否是主要的
     */
    public static void registerBean(String beanName, Object bean, boolean primary) {
        APP_CONTEXT.registerBean(beanName, bean, primary);
    }

    /**
     * 删除一个Bean
     *
     * @param beanName bean名称
     */
    public static void removeBean(String beanName) {
        APP_CONTEXT.removeBean(beanName);
    }
}
