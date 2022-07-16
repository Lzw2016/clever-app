package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.bind.Binder.Context;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;

/**
 * {@link Binder}用于绑定数据对象的内部策略。数据对象是由递归绑定属性组成的对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:07 <br/>
 *
 * @see JavaBeanBinder
 * @see ValueObjectBinder
 */
interface DataObjectBinder {
    /**
     * 如果 {@link DataObjectBinder} 不支持指定的 {@link Bindable}，则返回绑定实例或null。
     *
     * @param name           正在绑定的名称
     * @param target         可绑定到绑定的
     * @param context        绑定上下文
     * @param propertyBinder 属性绑定器
     * @param <T>            源类型
     * @return 绑定实例或 {@code null}
     */
    <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context, DataObjectPropertyBinder propertyBinder);

    /**
     * 如果 {@link DataObjectBinder} 不支持指定的 {@link Bindable}，则返回新创建的实例或null。
     *
     * @param target  可创建的绑定
     * @param context 绑定上下文
     * @param <T>     源类型
     * @return 创建的实例
     */
    <T> T create(Bindable<T> target, Context context);
}
