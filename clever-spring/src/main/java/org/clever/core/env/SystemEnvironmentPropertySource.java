package org.clever.core.env;

import org.clever.util.Assert;

import java.util.Map;

/**
 * 专门用于系统环境变量的{@link MapPropertySource}。
 * 补偿Bash和其他shell中不允许包含句点字符和或连字符的变量的约束；还允许在属性名称上使用大写字母，以便更习惯地使用shell。
 * 例如，调用{@code getProperty("foo.bar")}将尝试查找原始属性或任何“equivalent”属性的值，并返回第一个找到的值：
 * <ul>
 * <li>{@code foo.bar} - 原始名称</li>
 * <li>{@code foo_bar} - 带下划线的句点（如果有）</li>
 * <li>{@code FOO.BAR} - 原件，大写</li>
 * <li>{@code FOO_BAR} - 带下划线和大写</li>
 * </ul>
 * 上述任何连字符变体都可以使用，甚至可以混合使用点/连字符变体。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 21:19 <br/>
 *
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {
    /**
     * 使用给定的名称创建新的SystemEnvironmentPropertySource，并将其委派给给定的{@code MapPropertySource}
     */
    public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    /**
     * 如果此属性源中存在具有给定名称或其任何下划线/大写字母变体的属性，则返回true。
     */
    @Override
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * 如果此属性源中存在具有给定名称的属性或其任何下划线/大写字母变体，则此实现返回true。
     */
    @Override
    public Object getProperty(String name) {
        String actualName = resolvePropertyName(name);
        if (logger.isDebugEnabled() && !name.equals(actualName)) {
            logger.debug("PropertySource '" + getName() + "' does not contain property '" +
                    name + "', but found equivalent '" + actualName + "'"
            );
        }
        return super.getProperty(actualName);
    }

    /**
     * 检查此属性源是否包含具有给定名称的属性，或其任何下划线/大写变体。
     * 如果找到解析名称，则返回解析名称，否则返回原始名称。从不返回null。
     */
    protected final String resolvePropertyName(String name) {
        Assert.notNull(name, "Property name must not be null");
        String resolvedName = checkPropertyName(name);
        if (resolvedName != null) {
            return resolvedName;
        }
        String uppercasedName = name.toUpperCase();
        if (!name.equals(uppercasedName)) {
            resolvedName = checkPropertyName(uppercasedName);
            if (resolvedName != null) {
                return resolvedName;
            }
        }
        return name;
    }

    private String checkPropertyName(String name) {
        // Check name as-is
        if (containsKey(name)) {
            return name;
        }
        // Check name with just dots replaced
        String noDotName = name.replace('.', '_');
        if (!name.equals(noDotName) && containsKey(noDotName)) {
            return noDotName;
        }
        // Check name with just hyphens replaced
        String noHyphenName = name.replace('-', '_');
        if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
            return noHyphenName;
        }
        // Check name with dots and hyphens replaced
        String noDotNoHyphenName = noDotName.replace('-', '_');
        if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
            return noDotNoHyphenName;
        }
        // Give up
        return null;
    }

    @SuppressWarnings("RedundantCollectionOperation")
    private boolean containsKey(String name) {
        return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
    }

    protected boolean isSecurityManagerPresent() {
        return (System.getSecurityManager() != null);
    }
}
