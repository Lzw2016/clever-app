package org.clever.core.io;

import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 类路径资源的资源实现。
 * 使用给定的类加载器或给定的类来加载资源。
 * 支持{@code java.io.File}如果文件资源位于类路径中。
 * 始终支持解析为URL
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:53 <br/>
 *
 * @see ClassLoader#getResourceAsStream(String)
 * @see Class#getResourceAsStream(String)
 */
public class ClassPathResource extends AbstractFileResolvingResource {
    private final String path;
    private ClassLoader classLoader;
    private Class<?> clazz;

    /**
     * 为类加载器的使用创建一个新的类路径资源。
     * 前导斜杠将被删除，因为类加载器资源访问方法将不接受它。
     * 线程上下文类加载器将用于加载资源
     *
     * @param path 类路径中的绝对路径
     * @see java.lang.ClassLoader#getResourceAsStream(String)
     * @see org.clever.util.ClassUtils#getDefaultClassLoader()
     */
    public ClassPathResource(String path) {
        this(path, (ClassLoader) null);
    }

    /**
     * 为类加载器的使用创建一个新的类路径资源。前导斜杠将被删除，因为类加载器资源访问方法将不接受它
     *
     * @param path        类路径中的绝对路径
     * @param classLoader 加载资源的类加载器，或线程上下文类加载器为null
     * @see ClassLoader#getResourceAsStream(String)
     */
    public ClassPathResource(String path, ClassLoader classLoader) {
        Assert.notNull(path, "Path must not be null");
        String pathToUse = StringUtils.cleanPath(path);
        if (pathToUse.startsWith("/")) {
            pathToUse = pathToUse.substring(1);
        }
        this.path = pathToUse;
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }

    /**
     * 为类使用创建新的ClassPathResource。路径可以是相对于给定类的，也可以是通过斜杠在类路径中的绝对路径
     *
     * @param path  类路径中的相对或绝对路径
     * @param clazz 要加载资源的类
     * @see java.lang.Class#getResourceAsStream
     */
    public ClassPathResource(String path, Class<?> clazz) {
        Assert.notNull(path, "Path must not be null");
        this.path = StringUtils.cleanPath(path);
        this.clazz = clazz;
    }

    /**
     * 返回此资源的路径（作为类路径中的资源路径）
     */
    public final String getPath() {
        return this.path;
    }

    /**
     * 返回将从中获取此资源的类加载器
     */
    public final ClassLoader getClassLoader() {
        return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
    }

    /**
     * 此实现检查资源URL的解析
     *
     * @see java.lang.ClassLoader#getResource(String)
     * @see java.lang.Class#getResource(String)
     */
    @Override
    public boolean exists() {
        return (resolveURL() != null);
    }

    /**
     * 此实现首先检查资源URL的解析，然后继续{@link AbstractFileResolvingResource}的长度检查
     *
     * @see java.lang.ClassLoader#getResource(String)
     * @see java.lang.Class#getResource(String)
     */
    @Override
    public boolean isReadable() {
        URL url = resolveURL();
        return (url != null && checkReadable(url));
    }

    /**
     * 解析基础类路径资源的URL
     *
     * @return 已解析的URL，如果无法解析，则为null
     */
    protected URL resolveURL() {
        try {
            if (this.clazz != null) {
                return this.clazz.getResource(this.path);
            } else if (this.classLoader != null) {
                return this.classLoader.getResource(this.path);
            } else {
                return ClassLoader.getSystemResource(this.path);
            }
        } catch (IllegalArgumentException ex) {
            // Should not happen according to the JDK's contract:
            // see https://github.com/openjdk/jdk/pull/2662
            return null;
        }
    }

    /**
     * 此实现为给定的类路径资源打开一个InputStream
     *
     * @see java.lang.ClassLoader#getResourceAsStream(String)
     * @see java.lang.Class#getResourceAsStream(String)
     */
    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is;
        if (this.clazz != null) {
            is = this.clazz.getResourceAsStream(this.path);
        } else if (this.classLoader != null) {
            is = this.classLoader.getResourceAsStream(this.path);
        } else {
            is = ClassLoader.getSystemResourceAsStream(this.path);
        }
        if (is == null) {
            throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
        }
        return is;
    }

    /**
     * 此实现返回基础类路径资源的URL（如果可用）
     *
     * @see java.lang.ClassLoader#getResource(String)
     * @see java.lang.Class#getResource(String)
     */
    @Override
    public URL getURL() throws IOException {
        URL url = resolveURL();
        if (url == null) {
            throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
        }
        return url;
    }

    /**
     * 此实现创建一个ClassPathResource，应用相对于此描述符的基础资源路径的给定路径
     *
     * @see org.clever.util.StringUtils#applyRelativePath(String, String)
     */
    @Override
    public Resource createRelative(String relativePath) {
        String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
        return this.clazz != null ?
                new ClassPathResource(pathToUse, this.clazz) :
                new ClassPathResource(pathToUse, this.classLoader);
    }

    /**
     * 此实现返回此类路径资源引用的文件的名称
     *
     * @see org.clever.util.StringUtils#getFilename(String)
     */
    @Override
    public String getFilename() {
        return StringUtils.getFilename(this.path);
    }

    /**
     * 此实现返回包含类路径位置的描述
     */
    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder("class path resource [");
        String pathToUse = this.path;
        if (this.clazz != null && !pathToUse.startsWith("/")) {
            builder.append(ClassUtils.classPackageAsResourcePath(this.clazz));
            builder.append('/');
        }
        if (pathToUse.startsWith("/")) {
            pathToUse = pathToUse.substring(1);
        }
        builder.append(pathToUse);
        builder.append(']');
        return builder.toString();
    }

    /**
     * 此实现比较基础类路径位置
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassPathResource)) {
            return false;
        }
        ClassPathResource otherRes = (ClassPathResource) other;
        return (this.path.equals(otherRes.path)
                && ObjectUtils.nullSafeEquals(this.classLoader, otherRes.classLoader)
                && ObjectUtils.nullSafeEquals(this.clazz, otherRes.clazz));
    }

    /**
     * 此实现返回基础类路径位置的hashCode
     */
    @Override
    public int hashCode() {
        return this.path.hashCode();
    }
}
