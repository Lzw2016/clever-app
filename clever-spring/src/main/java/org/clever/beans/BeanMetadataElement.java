package org.clever.beans;

/**
 * 由携带配置源对象的bean元数据元素实现的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:07 <br/>
 */
public interface BeanMetadataElement {
    /**
     * 返回此元数据元素的配置源对象(可能为null)
     */
    default Object getSource() {
        return null;
    }
}
