package org.clever.core.io.support;

import org.clever.core.io.Resource;
import org.clever.core.io.ResourceLoader;

import java.io.IOException;

/**
 * 用于将位置模式(例如，Ant样式的路径模式)解析为资源对象的策略接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 22:18 <br/>
 *
 * @see org.clever.core.io.Resource
 * @see org.clever.core.io.ResourceLoader
 */
public interface ResourcePatternResolver extends ResourceLoader {
    /**
     * 类路径中所有匹配资源的伪URL前缀："classpath*:"这与ResourceLoader的类路径URL前缀不同，
     * 因为它检索给定名称(例如"/beans.xml")的所有匹配资源，例如在所有已部署JAR文件的根中
     *
     * @see org.clever.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
     */
    String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

    /**
     * 将给定的位置模式解析为资源对象。
     * 应尽可能避免指向相同物理资源的重叠资源条目。
     * 结果应该具有set语义
     *
     * @param locationPattern 要解析的位置模式
     * @return 相应的资源对象
     * @throws IOException 如果IO错误
     */
    Resource[] getResources(String locationPattern) throws IOException;
}
