package org.clever.beans;

import org.clever.core.AttributeAccessorSupport;

/**
 * {@link org.clever.core.AttributeAccessorSupport}的扩展，
 * 将属性作为{@link BeanMetadataAttribute}对象保存，以便跟踪定义源
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:08 <br/>
 */
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {
    private Object source;

    /**
     * 设置此元数据元素的配置源对象。对象的确切类型将取决于所使用的配置机制
     */
    public void setSource(Object source) {
        this.source = source;
    }

    @Override
    public Object getSource() {
        return this.source;
    }

    /**
     * 将给定的BeanMetadataAttribute添加到此访问者的属性集
     *
     * @param attribute 要注册的BeanMetadataAttribute对象
     */
    public void addMetadataAttribute(BeanMetadataAttribute attribute) {
        super.setAttribute(attribute.getName(), attribute);
    }

    /**
     * 在此访问者的属性集中查找给定的BeanMetadataAttribute
     *
     * @param name 属性的名称
     * @return 对应的BeanMetadataAttribute对象，如果未定义此类属性，则为null
     */
    public BeanMetadataAttribute getMetadataAttribute(String name) {
        return (BeanMetadataAttribute) super.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, new BeanMetadataAttribute(name, value));
    }

    @Override
    public Object getAttribute(String name) {
        BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.getAttribute(name);
        return (attribute != null ? attribute.getValue() : null);
    }

    @Override
    public Object removeAttribute(String name) {
        BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.removeAttribute(name);
        return (attribute != null ? attribute.getValue() : null);
    }
}
