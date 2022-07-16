package org.clever.boot.context.config;

import java.io.IOException;

/**
 * 可用于为给定 {@link ConfigDataResource} 加载 {@link ConfigData} 的策略类。
 * 应将实现添加为 {@code clever.factories} 条目。
 * <p>
 * 多个加载器不能声明相同的资源。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:39 <br/>
 *
 * @param <R> 资源类型
 */
public interface ConfigDataLoader<R extends ConfigDataResource> {
    /**
     * 返回此实例是否可以加载指定的资源。
     *
     * @param context  加载器上下文
     * @param resource 要检查的资源。
     * @return 如果此加载器支持资源
     */
    default boolean isLoadable(ConfigDataLoaderContext context, R resource) {
        return true;
    }

    /**
     * 为给定资源加载 {@link ConfigData}
     *
     * @param context  加载器上下文
     * @param resource 要加载的资源
     * @return 加载的配置数据或 null 如果应跳过该位置
     * @throws IOException                         IO 错误
     * @throws ConfigDataResourceNotFoundException 如果找不到资源
     */
    ConfigData load(ConfigDataLoaderContext context, R resource) throws IOException, ConfigDataResourceNotFoundException;
}
