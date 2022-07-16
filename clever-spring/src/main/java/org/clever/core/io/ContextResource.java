package org.clever.core.io;

/**
 * 从封闭的“context”加载的资源的扩展接口，
 * 例如从{@code javax.servlet.ServletContext}，
 * 但也来自普通类路径路径或相对文件系统路径(指定时没有显式前缀，因此相对于本地{@link ResourceLoader}的上下文应用)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:23 <br/>
 */
public interface ContextResource extends Resource {
    /**
     * 返回封闭“context”中的路径。
     * 这通常是相对于特定于上下文的根目录的路径，
     * 例如ServletContext根目录或PortletContext根目录
     */
    String getPathWithinContext();
}
