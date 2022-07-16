package org.clever.core.io;

/**
 * 协议特定资源句柄的解决策略<br/>
 * 用作{@link DefaultResourceLoader}的SPI，允许处理自定义协议，而无需对加载程序实现（或应用程序上下文实现）进行子类化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:19 <br/>
 */
@FunctionalInterface
public interface ProtocolResolver {
    /**
     * 如果此实现的协议匹配，则根据给定的资源加载程序解析给定的位置
     *
     * @param location       用户指定的资源位置
     * @param resourceLoader 关联的资源加载器
     * @return 如果给定位置与此解析器的协议匹配，则为相应的资源句柄，否则为null
     */
    Resource resolve(String location, ResourceLoader resourceLoader);
}
