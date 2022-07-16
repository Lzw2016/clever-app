package org.clever.boot.context.properties.bind;

/**
 * {@link DataObjectBinder} 实现可以使用的绑定器来绑定数据对象属性。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:08 <br/>
 */
interface DataObjectPropertyBinder {
    /**
     * 绑定给定的属性。
     *
     * @param propertyName 属性名称（小写虚线形式，例如名字）
     * @param target       目标可绑定
     * @return 约束值或 {@code null}
     */
    Object bindProperty(String propertyName, Bindable<?> target);
}
