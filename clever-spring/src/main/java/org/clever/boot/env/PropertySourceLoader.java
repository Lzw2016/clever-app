package org.clever.boot.env;

import org.clever.core.env.PropertySource;
import org.clever.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * 通过FactoriesLoader定位的策略接口，用于加载{@link PropertySource}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:40 <br/>
 */
public interface PropertySourceLoader {
    /**
     * 返回加载程序支持的文件扩展名(不包括'.').
     *
     * @return 文件扩展名
     */
    String[] getFileExtensions();

    /**
     * 将资源加载到一个或多个属性源中。
     * 实现可以返回包含单个源的列表，或者在多文档格式（如yaml）的情况下，返回资源中每个文档的源。
     *
     * @param name     属性源的根名称。如果加载了多个文档，则应在加载的每个源的名称中添加一个附加后缀。
     * @param resource 要加载的资源
     * @return 列出属性源
     * @throws IOException 如果无法加载源
     */
    List<PropertySource<?>> load(String name, Resource resource) throws IOException;
}
