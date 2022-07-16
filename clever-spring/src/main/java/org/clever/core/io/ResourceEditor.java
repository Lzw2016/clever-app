package org.clever.core.io;

import org.clever.core.env.PropertyResolver;
import org.clever.core.env.StandardEnvironment;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * 资源描述符编辑器，用于自动转换字符串位置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 14:08 <br/>
 *
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see PropertyResolver#resolvePlaceholders
 */
public class ResourceEditor extends PropertyEditorSupport {
    private final ResourceLoader resourceLoader;
    private PropertyResolver propertyResolver;
    private final boolean ignoreUnresolvablePlaceholders;

    /**
     * 使用DefaultResourceLoader和StandardEnvironment创建ResourceEditor类的新实例
     */
    public ResourceEditor() {
        this(new DefaultResourceLoader(), null);
    }

    /**
     * 使用给定的ResourceLoader和PropertyResolver创建ResourceEditor类的新实例
     *
     * @param resourceLoader   要使用的{@code ResourceLoader}
     * @param propertyResolver 要使用的{@code PropertyResolver}
     */
    public ResourceEditor(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
        this(resourceLoader, propertyResolver, true);
    }

    /**
     * 使用给定的{@link ResourceEditor}创建{@link ResourceLoader}类的新实例
     *
     * @param resourceLoader                 要使用的{@code ResourceLoader}
     * @param propertyResolver               要使用的{@code PropertyResolver}
     * @param ignoreUnresolvablePlaceholders 如果在给定的{@code propertyResolver}中找不到相应的属性，是否忽略无法解析的占位符
     */
    public ResourceEditor(ResourceLoader resourceLoader, PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
        this.propertyResolver = propertyResolver;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    @Override
    public void setAsText(String text) {
        if (StringUtils.hasText(text)) {
            String locationToUse = resolvePath(text).trim();
            setValue(this.resourceLoader.getResource(locationToUse));
        } else {
            setValue(null);
        }
    }

    /**
     * 解析给定路径，如有必要，使用环境中相应的属性值替换占位符
     *
     * @param path 原始文件路径
     * @return 解析的文件路径
     * @see PropertyResolver#resolvePlaceholders
     * @see PropertyResolver#resolveRequiredPlaceholders
     */
    protected String resolvePath(String path) {
        if (this.propertyResolver == null) {
            this.propertyResolver = new StandardEnvironment();
        }
        return (this.ignoreUnresolvablePlaceholders ?
                this.propertyResolver.resolvePlaceholders(path) :
                this.propertyResolver.resolveRequiredPlaceholders(path));
    }

    @Override
    public String getAsText() {
        Resource value = (Resource) getValue();
        try {
            // Try to determine URL for resource.
            return (value != null ? value.getURL().toExternalForm() : "");
        } catch (IOException ex) {
            // Couldn't determine resource URL - return null to indicate
            // that there is no appropriate text representation.
            return null;
        }
    }
}
