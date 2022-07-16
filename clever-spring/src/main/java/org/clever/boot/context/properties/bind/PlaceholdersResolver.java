package org.clever.boot.context.properties.bind;

import org.clever.core.env.PropertyResolver;

/**
 * {@link Binder}用于解析属性占位符的可选策略。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:23 <br/>
 *
 * @see PropertySourcesPlaceholdersResolver
 */
@FunctionalInterface
public interface PlaceholdersResolver {
    /**
     * 无操作 {@link PropertyResolver}.
     */
    PlaceholdersResolver NONE = (value) -> value;

    /**
     * 调用以解析给定值中的任何占位符。
     *
     * @param value 源值
     * @return 已解析占位符的值
     */
    Object resolvePlaceholders(Object value);
}
