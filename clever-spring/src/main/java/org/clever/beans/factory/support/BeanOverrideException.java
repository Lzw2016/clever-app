package org.clever.beans.factory.support;

import org.clever.beans.factory.BeanDefinitionStoreException;
import org.clever.beans.factory.config.BeanHolder;

/**
 * 参考 {@code BeanDefinitionOverrideException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 09:46 <br/>
 */
public class BeanOverrideException extends BeanDefinitionStoreException {
    private final BeanHolder<?> bean;
    private final BeanHolder<?> existingBean;

    public BeanOverrideException(String beanName, BeanHolder<?> bean, BeanHolder<?> existingBean) {
        super(
                bean.toString(),
                beanName,
                "Cannot register bean [" + bean + "] for bean '" + beanName + "': There is already [" + existingBean + "] bound."
        );
        this.bean = bean;
        this.existingBean = existingBean;
    }

    /**
     * 返回bean定义来自的资源的描述
     */
    @Override
    public String getResourceDescription() {
        return String.valueOf(super.getResourceDescription());
    }

    /**
     * 返回bean的名称
     */
    @Override
    public String getBeanName() {
        return String.valueOf(super.getBeanName());
    }

    /**
     * 返回新注册的bean定义
     *
     * @see #getBeanName()
     */
    public BeanHolder<?> getBeanDefinition() {
        return this.bean;
    }

    /**
     * 返回相同名称的现有bean定义
     *
     * @see #getBeanName()
     */
    public BeanHolder<?> getExistingDefinition() {
        return this.existingBean;
    }
}
