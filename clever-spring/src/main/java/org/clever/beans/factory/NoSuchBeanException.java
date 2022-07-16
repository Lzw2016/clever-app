package org.clever.beans.factory;

import org.clever.beans.BeansException;
import org.clever.core.ResolvableType;

/**
 * 参考 {@code NoSuchBeanDefinitionException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 10:18 <br/>
 */
public class NoSuchBeanException extends BeansException {
    private final String beanName;
    private final ResolvableType resolvableType;

    /**
     * 创建一个新的 {@code NoSuchBeanException}
     *
     * @param name 缺少的bean的名称
     */
    public NoSuchBeanException(String name) {
        super("No bean named '" + name + "' available");
        this.beanName = name;
        this.resolvableType = null;
    }

    /**
     * 创建一个新的 {@code NoSuchBeanException}
     *
     * @param name    缺少的bean的名称
     * @param message 描述问题的详细消息
     */
    public NoSuchBeanException(String name, String message) {
        super("No bean named '" + name + "' available: " + message);
        this.beanName = name;
        this.resolvableType = null;
    }

    /**
     * 创建一个新的 {@code NoSuchBeanException}
     *
     * @param type 缺少的bean的所需类型
     */
    public NoSuchBeanException(Class<?> type) {
        this(ResolvableType.forClass(type));
    }

    /**
     * 创建一个新的 {@code NoSuchBeanException}
     *
     * @param type    缺少的bean的所需类型
     * @param message 描述问题的详细消息
     */
    public NoSuchBeanException(Class<?> type, String message) {
        this(ResolvableType.forClass(type), message);
    }

    /**
     * 创建一个新的 {@code NoSuchBeanException}
     *
     * @param type 缺失bean的完整类型声明
     */
    public NoSuchBeanException(ResolvableType type) {
        super("No qualifying bean of type '" + type + "' available");
        this.beanName = null;
        this.resolvableType = type;
    }

    /**
     * 创建一个新的 {@code NoSuchBeanException}
     *
     * @param type    缺失bean的完整类型声明
     * @param message 描述问题的详细消息
     */
    public NoSuchBeanException(ResolvableType type, String message) {
        super("No qualifying bean of type '" + type + "' available: " + message);
        this.beanName = null;
        this.resolvableType = type;
    }

    /**
     * 如果按名称查找失败，则返回缺少的bean的名称
     */
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * 如果按类型查找失败，则返回缺少的bean的所需类型
     */
    public Class<?> getBeanType() {
        return (this.resolvableType != null ? this.resolvableType.resolve() : null);
    }

    /**
     * 如果按类型查找失败，则返回缺失bean的所需可解析类型
     */
    public ResolvableType getResolvableType() {
        return this.resolvableType;
    }

    /**
     * 返回当只需要一个匹配bean时找到的bean数。
     * 对于常规的NoSuchBeanException，该值始终为0。
     */
    public int getNumberOfBeansFound() {
        return 0;
    }
}
