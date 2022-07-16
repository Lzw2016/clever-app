package org.clever.core.io;

import org.clever.util.ResourceUtils;

/**
 * 用于加载资源（例如，类路径或文件系统资源）的策略接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 14:09 <br/>
 *
 * @see Resource
 * @see org.clever.core.io.support.ResourcePatternResolver
 */
public interface ResourceLoader {
    /**
     * 用于从类路径加载的伪URL前缀："classpath:".
     */
    String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

    /**
     * 返回指定资源位置的{@code Resource}
     *
     * @param location 资源位置
     * @return 相应的资源句柄（从不为null）
     * @see #CLASSPATH_URL_PREFIX
     * @see Resource#exists()
     * @see Resource#getInputStream()
     */
    Resource getResource(String location);

    /**
     * 公开此{@code ResourceLoader}使用的{@link ClassLoader}
     *
     * @return 类加载器（仅当系统类加载器不可访问时才为null）
     * @see org.clever.util.ClassUtils#getDefaultClassLoader()
     * @see org.clever.util.ClassUtils#forName(String, ClassLoader)
     */
    ClassLoader getClassLoader();
}
