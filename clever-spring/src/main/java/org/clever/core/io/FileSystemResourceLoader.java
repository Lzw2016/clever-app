package org.clever.core.io;

/**
 * {@link ResourceLoader}实现，将普通路径解析为文件系统资源，而不是类路径资源（后者是{@link DefaultResourceLoader}的默认策略）。
 * 注意：普通路径将始终被解释为相对于当前VM工作目录，即使它们以斜杠开头。(这与Servlet容器中的语义一致)使用明确的“file：”前缀强制执行绝对文件路径。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:46 <br/>
 *
 * @see DefaultResourceLoader
 */
public class FileSystemResourceLoader extends DefaultResourceLoader {
    /**
     * 将资源路径解析为文件系统路径
     * <p>注意：即使给定的路径以斜杠开头，它也会被解释为相对于当前VM工作目录
     *
     * @param path 资源的路径
     * @return 相应的资源句柄
     * @see FileSystemResource
     */
    @Override
    protected Resource getResourceByPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new FileSystemContextResource(path);
    }

    /**
     * FileSystemResource，通过实现ContextResource接口显式表示上下文相对路径
     */
    private static class FileSystemContextResource extends FileSystemResource implements ContextResource {
        public FileSystemContextResource(String path) {
            super(path);
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }
    }
}
