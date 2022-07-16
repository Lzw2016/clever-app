package org.clever.beans.propertyeditors;

import org.clever.core.io.ClassPathResource;
import org.clever.util.ClassUtils;
import org.clever.util.ResourceUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * {@code java.net.URI} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:39 <br/>
 *
 * @see java.net.URI
 * @see URLEditor
 */
public class URIEditor extends PropertyEditorSupport {
    private final ClassLoader classLoader;
    private final boolean encode;

    /**
     * 创建一个新的编码URI编辑器，将"classpath:"位置转换为标准URI（不尝试将它们解析为物理资源）
     */
    public URIEditor() {
        this(true);
    }

    /**
     * 创建一个新的URIEditor，将位置转换为标准URI（不尝试将它们解析为物理资源）
     *
     * @param encode 指示是否对字符串进行编码
     */
    public URIEditor(boolean encode) {
        this.classLoader = null;
        this.encode = encode;
    }

    /**
     * Create a new URIEditor, using the given ClassLoader to resolve
     * "classpath:" locations into physical resource URLs.
     *
     * @param classLoader the ClassLoader to use for resolving "classpath:" locations
     *                    (may be {@code null} to indicate the default ClassLoader)
     */
    public URIEditor(ClassLoader classLoader) {
        this(classLoader, true);
    }

    /**
     * 创建一个新的URIEditor，使用给定的类加载器将"classpath:"位置解析为物理资源URL
     *
     * @param classLoader 用于解析"classpath:"位置的类加载器（可以为null以指示默认的类加载器）
     * @param encode      指示是否对字符串进行编码
     */
    public URIEditor(ClassLoader classLoader, boolean encode) {
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        this.encode = encode;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            String uri = text.trim();
            if (this.classLoader != null && uri.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
                ClassPathResource resource = new ClassPathResource(uri.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length()), this.classLoader);
                try {
                    setValue(resource.getURI());
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
                }
            } else {
                try {
                    setValue(createURI(uri));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
                }
            }
        } else {
            setValue(null);
        }
    }

    /**
     * 为给定的用户指定的字符串值创建URI实例。默认实现将该值编码为符合RFC-2396的URI
     *
     * @param value 要转换为URI实例的值
     * @return URI实例
     * @throws java.net.URISyntaxException 如果URI转换失败
     */
    protected URI createURI(String value) throws URISyntaxException {
        int colonIndex = value.indexOf(':');
        if (this.encode && colonIndex != -1) {
            int fragmentIndex = value.indexOf('#', colonIndex + 1);
            String scheme = value.substring(0, colonIndex);
            String ssp = value.substring(colonIndex + 1, (fragmentIndex > 0 ? fragmentIndex : value.length()));
            String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
            return new URI(scheme, ssp, fragment);
        } else {
            // not encoding or the value contains no scheme - fallback to default
            return new URI(value);
        }
    }

    @Override
    public String getAsText() {
        URI value = (URI) getValue();
        return (value != null ? value.toString() : "");
    }
}
