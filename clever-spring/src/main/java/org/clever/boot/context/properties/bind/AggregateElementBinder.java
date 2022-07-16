package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;

/**
 * {@link AggregateBinder} 实现可用于递归绑定元素的绑定器。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:17 <br/>
 */
@FunctionalInterface
interface AggregateElementBinder {
    /**
     * 将给定名称绑定到可绑定的目标。
     *
     * @param name   要绑定的名称
     * @param target 目标可绑定
     * @return 绑定对象或null
     */
    default Object bind(ConfigurationPropertyName name, Bindable<?> target) {
        return bind(name, target, null);
    }

    /**
     * 将给定名称绑定到可绑定的目标，可以选择限制为单个源。
     *
     * @param name   要绑定的名称
     * @param target 目标可绑定
     * @param source 元素的源或null以使用所有源
     * @return 绑定对象或null
     */
    Object bind(ConfigurationPropertyName name, Bindable<?> target, ConfigurationPropertySource source);
}
