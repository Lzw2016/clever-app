package org.clever.beans.factory;

import org.clever.beans.FatalBeanException;

/**
 * 当BeanFactory遇到无效的bean定义时引发异常：例如，在bean元数据不完整或相互矛盾的情况下。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 09:42 <br/>
 */
public class BeanDefinitionStoreException extends FatalBeanException {
    private final String resourceDescription;
    private final String beanName;

    /**
     * 创建新的BeanDefinitionStoreException
     *
     * @param msg 细节消息（按原样用作异常消息）
     */
    public BeanDefinitionStoreException(String msg) {
        super(msg);
        this.resourceDescription = null;
        this.beanName = null;
    }

    /**
     * 创建新的BeanDefinitionStoreException
     *
     * @param msg   细节消息（按原样用作异常消息）
     * @param cause 根本原因（可能为空）
     */
    public BeanDefinitionStoreException(String msg, Throwable cause) {
        super(msg, cause);
        this.resourceDescription = null;
        this.beanName = null;
    }

    /**
     * 创建新的BeanDefinitionStoreException
     *
     * @param resourceDescription bean定义来自的资源的描述
     * @param msg                 细节消息（按原样用作异常消息）
     */
    public BeanDefinitionStoreException(String resourceDescription, String msg) {
        super(msg);
        this.resourceDescription = resourceDescription;
        this.beanName = null;
    }

    /**
     * 创建新的BeanDefinitionStoreException
     *
     * @param resourceDescription bean定义来自的资源的描述
     * @param msg                 细节消息（按原样用作异常消息）
     * @param cause               根本原因（可能为空）
     */
    public BeanDefinitionStoreException(String resourceDescription, String msg, Throwable cause) {
        super(msg, cause);
        this.resourceDescription = resourceDescription;
        this.beanName = null;
    }

    /**
     * 创建新的BeanDefinitionStoreException
     *
     * @param resourceDescription bean定义来自的资源的描述
     * @param beanName            bean的名称
     * @param msg                 细节消息（附加到指示资源和bean名称的介绍性消息之后）
     */
    public BeanDefinitionStoreException(String resourceDescription, String beanName, String msg) {
        this(resourceDescription, beanName, msg, null);
    }

    /**
     * 创建新的BeanDefinitionStoreException
     *
     * @param resourceDescription bean定义来自的资源的描述
     * @param beanName            bean的名称
     * @param msg                 细节消息（附加到指示资源和bean名称的介绍性消息之后）
     * @param cause               根本原因（可能为空）
     */
    public BeanDefinitionStoreException(String resourceDescription, String beanName, String msg, Throwable cause) {
        super("Invalid bean definition with name '" + beanName + "' defined in " + resourceDescription + ": " + msg, cause);
        this.resourceDescription = resourceDescription;
        this.beanName = beanName;
    }

    /**
     * 返回bean定义来自的资源的描述（如果可用）。
     */
    public String getResourceDescription() {
        return this.resourceDescription;
    }

    /**
     * 返回bean的名称（如果可用）。
     */
    public String getBeanName() {
        return this.beanName;
    }
}
