package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.Ordered;
import org.clever.core.annotation.Order;
import org.clever.core.env.Environment;
import org.clever.core.io.ResourceLoader;

import java.util.Collections;
import java.util.List;

/**
 * 用于将 {@link ConfigDataLocation 位置} 解析为一个或多个 {@link ConfigDataResource 资源} 的策略接口。
 * 应将实现添加为 {@code clever.factories} 条目。
 * 支持以下构造函数参数类型：
 * <ul>
 * <li>{@link Binder} - 如果解析器需要从初始 {@link Environment} 中获取值 </li>
 * <li>{@link ResourceLoader} - 如果解析器需要资源加载器 </li>
 * </ul>
 * <p>
 * 解析器可以实现 {@link Ordered} 或使用 {@link Order @Order} 注释。将使用支持给定位置的第一个解析器。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:13 <br/>
 */
public interface ConfigDataLocationResolver<R extends ConfigDataResource> {
    /**
     * 如果此解析器可以解析指定的位置地址，则返回。
     *
     * @param context  位置解析器上下文
     * @param location 要检查的位置。
     * @return 如果此解析器支持该位置
     */
    boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location);

    /**
     * 将 {@link ConfigDataLocation} 解析为一个或多个 {@link ConfigDataResource} 实例。
     *
     * @param context  位置解析器上下文
     * @param location 应该解决的位置
     * @return 按优先级升序排列的 {@link ConfigDataResource 资源}
     * @throws ConfigDataLocationNotFoundException 在无法找到的非可选位置
     * @throws ConfigDataResourceNotFoundException 如果找不到已解析的资源
     */
    List<R> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException;

    /**
     * 根据可用配置文件将 {@link ConfigDataLocation} 解析为一个或多个 {@link ConfigDataResource} 实例。
     * 一旦从贡献值推导出配置文件，就会调用此方法。默认情况下，此方法返回一个空列表。
     *
     * @param context  位置解析器上下文
     * @param location 应该解决的位置
     * @param profiles profile 信息
     * @return 按优先级升序排列的已解决位置列表
     * @throws ConfigDataLocationNotFoundException 在无法找到的非可选位置
     */
    default List<R> resolveProfileSpecific(ConfigDataLocationResolverContext context, ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
        return Collections.emptyList();
    }
}
