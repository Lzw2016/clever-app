package org.clever.beans.factory.config;

import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 09:48 <br/>
 */
public class BeanHolder<T> {
    private final String beanName;
    private final boolean primary;
    private final T beanInstance;

    public BeanHolder(String beanName, boolean primary, T beanInstance) {
        Assert.hasText(beanName, "Bean name must not be null");
        Assert.notNull(beanInstance, "Bean instance must not be null");
        this.beanName = beanName;
        this.primary = primary;
        this.beanInstance = beanInstance;
    }

    public String getBeanName() {
        return this.beanName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public T getBeanInstance() {
        return this.beanInstance;
    }

    public Class<?> getBeanType() {
        return beanInstance.getClass();
    }

    @Override
    public String toString() {
        return "BeanHolder{" +
                "beanName='" + beanName + '\'' +
                ", primary=" + primary +
                ", beanInstance=" + (beanInstance == null ? null : beanInstance.getClass().getName()) +
                '}';
    }
}
