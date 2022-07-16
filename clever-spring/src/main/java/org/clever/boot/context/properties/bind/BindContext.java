package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;

/**
 * {@link BindHandler BindHandlers} 使用的上下文信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:06 <br/>
 */
public interface BindContext {
    /**
     * 返回正在执行绑定操作的源绑定器
     *
     * @return 源绑定器
     */
    Binder getBinder();

    /**
     * 返回绑定的当前深度。根绑定从深度0开始。每个后续属性绑定将深度增加1。
     *
     * @return 当前绑定的深度
     */
    int getDepth();

    /**
     * 返回 {@link Binder} 正在使用的 {@link ConfigurationPropertySource sources} 的 {@link Iterable}
     *
     * @return 来源
     */
    Iterable<ConfigurationPropertySource> getSources();

    /**
     * 返回实际绑定的 {@link ConfigurationProperty}，如果尚未确定属性，则返回null。
     *
     * @return 配置属性（可以为null）
     */
    ConfigurationProperty getConfigurationProperty();
}
