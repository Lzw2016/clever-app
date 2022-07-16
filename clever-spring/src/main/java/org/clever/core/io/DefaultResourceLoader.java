package org.clever.core.io;

import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ResourceUtils;
import org.clever.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ResourceLoader}接口的默认实现。
 * 由{@link ResourceEditor}使用，也可以单独使用。
 * 如果位置值是URL，则返回{@link UrlResource}；如果是非URL路径或“classpath:”伪URL，则返回{@link ClassPathResource}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:18 <br/>
 *
 * @see FileSystemResourceLoader
 */
public class DefaultResourceLoader implements ResourceLoader {
    private ClassLoader classLoader;
    private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);
    private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);

    /**
     * 创建新的DefaultResourceLoader<br/>
     * 类加载器访问将在实际访问资源时使用线程上下文类加载器进行
     *
     * @see java.lang.Thread#getContextClassLoader()
     */
    public DefaultResourceLoader() {
    }

    /**
     * 创建新的DefaultResourceLoader
     *
     * @param classLoader 加载类路径资源的类加载器，或在实际资源访问时使用线程上下文类加载器的类加载器为null
     */
    public DefaultResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 指定用于加载类路径资源的类加载器，或指定null用于在实际访问资源时使用线程上下文类加载器。
     * 默认情况下，类加载器访问将在实际访问资源时使用线程上下文类加载器进行
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 返回用于加载类路径资源的类加载器。
     * 将传递给此资源加载程序创建的所有ClassPathResource对象的ClassPathResource构造函数
     *
     * @see ClassPathResource
     */
    @Override
    public ClassLoader getClassLoader() {
        return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
    }

    /**
     * 将给定的解析器注册到此资源加载器，以便处理其他协议
     * <p>任何这样的解析器都将在加载程序的标准解析规则之前调用。因此，它也可以覆盖任何默认规则
     *
     * @see #getProtocolResolvers()
     */
    public void addProtocolResolver(ProtocolResolver resolver) {
        Assert.notNull(resolver, "ProtocolResolver must not be null");
        this.protocolResolvers.add(resolver);
    }

    /**
     * 返回当前注册的协议解析器的集合，允许进行内省和修改
     */
    public Collection<ProtocolResolver> getProtocolResolvers() {
        return this.protocolResolvers;
    }

    /**
     * 获取给定值类型的缓存，由资源设置关键字
     *
     * @param valueType 值类型，例如ASM {@code MetadataReader}
     * @return 缓存 {@link Map}, 在{@code ResourceLoader}级别共享
     */
    @SuppressWarnings("unchecked")
    public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
        return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
    }

    /**
     * 清除此资源加载程序中的所有资源缓存
     *
     * @see #getResourceCache
     */
    public void clearResourceCaches() {
        this.resourceCaches.clear();
    }

    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
            Resource resource = protocolResolver.resolve(location, this);
            if (resource != null) {
                return resource;
            }
        }
        if (location.startsWith("/")) {
            return getResourceByPath(location);
        } else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
        } else {
            try {
                // Try to parse the location as a URL...
                URL url = new URL(location);
                return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
            } catch (MalformedURLException ex) {
                // No URL -> resolve as resource path.
                return getResourceByPath(location);
            }
        }
    }

    /**
     * 返回给定路径上资源的资源句柄
     * <p>默认实现支持类路径位置。这应该适用于独立实现，但可以覆盖，例如针对Servlet容器的实现
     *
     * @param path 资源的路径
     * @return 相应的资源句柄
     * @see ClassPathResource
     */
    protected Resource getResourceByPath(String path) {
        return new ClassPathContextResource(path, getClassLoader());
    }

    /**
     * ClassPathResource，通过实现ContextResource接口显式表示上下文相对路径
     */
    protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {
        public ClassPathContextResource(String path, ClassLoader classLoader) {
            super(path, classLoader);
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }

        @Override
        public Resource createRelative(String relativePath) {
            String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
            return new ClassPathContextResource(pathToUse, getClassLoader());
        }
    }
}
