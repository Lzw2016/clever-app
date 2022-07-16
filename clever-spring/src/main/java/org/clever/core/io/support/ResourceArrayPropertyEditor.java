package org.clever.core.io.support;

import org.clever.core.env.Environment;
import org.clever.core.env.PropertyResolver;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.*;

/**
 * {@link org.clever.core.io.Resource}数组编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:38 <br/>
 *
 * @see org.clever.core.io.Resource
 * @see ResourcePatternResolver
 * @see PathMatchingResourcePatternResolver
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {
    private static final Logger logger = LoggerFactory.getLogger(ResourceArrayPropertyEditor.class);

    private final ResourcePatternResolver resourcePatternResolver;
    private PropertyResolver propertyResolver;
    private final boolean ignoreUnresolvablePlaceholders;

    /**
     * 使用默认的{@link PathMatchingResourcePatternResolver}和{@link StandardEnvironment}创建新的ResourceArrayPropertyEditor
     *
     * @see PathMatchingResourcePatternResolver
     * @see Environment
     */
    public ResourceArrayPropertyEditor() {
        this(new PathMatchingResourcePatternResolver(), null, true);
    }

    /**
     * 使用给定的{@link ResourcePatternResolver}和{@link PropertyResolver}(通常是{@link Environment})创建新的ResourceArrayPropertyEditor
     *
     * @param resourcePatternResolver 要使用的ResourcePatternResolver
     * @param propertyResolver        使用要使用的PropertyResolver
     */
    public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver, PropertyResolver propertyResolver) {
        this(resourcePatternResolver, propertyResolver, true);
    }

    /**
     * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
     * and {@link PropertyResolver} (typically an {@link Environment}).
     *
     * @param resourcePatternResolver        要使用的ResourcePatternResolver
     * @param propertyResolver               使用要使用的PropertyResolver
     * @param ignoreUnresolvablePlaceholders 如果找不到相应的系统属性，是否忽略无法解析的占位符
     */
    public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver,
                                       PropertyResolver propertyResolver,
                                       boolean ignoreUnresolvablePlaceholders) {
        Assert.notNull(resourcePatternResolver, "ResourcePatternResolver must not be null");
        this.resourcePatternResolver = resourcePatternResolver;
        this.propertyResolver = propertyResolver;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    /**
     * 将给定的文本视为位置模式，并将其转换为资源数组
     */
    @Override
    public void setAsText(String text) {
        String pattern = resolvePath(text).trim();
        try {
            setValue(this.resourcePatternResolver.getResources(pattern));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
        }
    }

    /**
     * 将给定值视为集合或数组，并将其转换为资源数组。
     * 将字符串元素视为位置模式，并按原样接受资源元素。
     */
    @Override
    public void setValue(Object value) throws IllegalArgumentException {
        if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
            Collection<?> input = (value instanceof Collection ? (Collection<?>) value : Arrays.asList((Object[]) value));
            Set<Resource> merged = new LinkedHashSet<>();
            for (Object element : input) {
                if (element instanceof String) {
                    // A location pattern: resolve it into a Resource array.
                    // Might point to a single resource or to multiple resources.
                    String pattern = resolvePath((String) element).trim();
                    try {
                        Resource[] resources = this.resourcePatternResolver.getResources(pattern);
                        Collections.addAll(merged, resources);
                    } catch (IOException ex) {
                        // ignore - might be an unresolved placeholder or non-existing base directory
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not retrieve resources for pattern '" + pattern + "'", ex);
                        }
                    }
                } else if (element instanceof Resource) {
                    // A Resource object: add it to the result.
                    merged.add((Resource) element);
                } else {
                    throw new IllegalArgumentException(
                            "Cannot convert element [" + element + "] to [" + Resource.class.getName() +
                                    "]: only location String and Resource object supported"
                    );
                }
            }
            super.setValue(merged.toArray(new Resource[0]));
        } else {
            // An arbitrary value: probably a String or a Resource array.
            // setAsText will be called for a String; a Resource array will be used as-is.
            super.setValue(value);
        }
    }

    /**
     * 解析给定路径，必要时用相应的系统属性值替换占位符
     *
     * @param path 原始文件路径
     * @return 解析的文件路径
     * @see PropertyResolver#resolvePlaceholders
     * @see PropertyResolver#resolveRequiredPlaceholders(String)
     */
    protected String resolvePath(String path) {
        if (this.propertyResolver == null) {
            this.propertyResolver = new StandardEnvironment();
        }
        return this.ignoreUnresolvablePlaceholders ?
                this.propertyResolver.resolvePlaceholders(path) :
                this.propertyResolver.resolveRequiredPlaceholders(path);
    }
}
