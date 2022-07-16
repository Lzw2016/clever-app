package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.Binder;

/**
 * 提供给 {@link ConfigDataLocationResolver} 方法的上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:17 <br/>
 */
public interface ConfigDataLocationResolverContext {
    /**
     * 提供对可用于获取先前贡献值的活页夹的访问。
     *
     * @return Binder instance
     */
    Binder getBinder();

    /**
     * 提供对触发解析的父 {@link ConfigDataResource} 的访问，如果没有可用的父，则为 null。
     *
     * @return 父位置
     */
    ConfigDataResource getParent();
}
