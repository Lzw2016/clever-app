package org.clever.util;

/**
 * 用于解析文本中占位符的帮助器类。通常应用于文件路径。
 * 文本可能包含{@code ${...}}占位符，解析为系统属性：例如{@code ${user.dir}}。
 * 可以使用键和值之间的":"分隔符提供默认值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 21:04 <br/>
 *
 * @see #PLACEHOLDER_PREFIX
 * @see #PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 */
public abstract class SystemPropertyUtils {
    /**
     * 系统属性占位符的前缀: "${".
     */
    public static final String PLACEHOLDER_PREFIX = "${";
    /**
     * 系统特性占位符的后缀: "}".
     */
    public static final String PLACEHOLDER_SUFFIX = "}";
    /**
     * 系统属性占位符的值分隔符: ":".
     */
    public static final String VALUE_SEPARATOR = ":";

    private static final PropertyPlaceholderHelper strictHelper = new PropertyPlaceholderHelper(
            PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false
    );
    private static final PropertyPlaceholderHelper nonStrictHelper = new PropertyPlaceholderHelper(
            PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true
    );

    /**
     * 解析{@code ${...}}给定文本中的占位符，将其替换为相应的系统特性值
     *
     * @param text 要解析的字符串
     * @return 解析的字符串
     * @throws IllegalArgumentException 如果存在无法解析的占位符
     * @see #PLACEHOLDER_PREFIX
     * @see #PLACEHOLDER_SUFFIX
     */
    public static String resolvePlaceholders(String text) {
        return resolvePlaceholders(text, false);
    }

    /**
     * 解析{@code ${...}}给定文本中的占位符，将其替换为相应的系统特性值。
     * 如果标志设置为true，则忽略没有默认值的不可解析占位符，并原封不动地传递
     *
     * @param text                           要解析的字符串
     * @param ignoreUnresolvablePlaceholders 是否忽略未解析的占位符
     * @return 解析的字符串
     * @throws IllegalArgumentException 如果存在无法解析的占位符
     * @see #PLACEHOLDER_PREFIX
     * @see #PLACEHOLDER_SUFFIX “IgnoreUnsolvablePlaceholders”标志是 {@code false}
     */
    public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
        if (text.isEmpty()) {
            return text;
        }
        PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
        return helper.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
    }

    /**
     * 根据系统属性和系统环境变量进行解析的占位符解析器实现
     */
    private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {
        private final String text;

        public SystemPropertyPlaceholderResolver(String text) {
            this.text = text;
        }

        @Override
        public String resolvePlaceholder(String placeholderName) {
            try {
                String propVal = System.getProperty(placeholderName);
                if (propVal == null) {
                    // Fall back to searching the system environment.
                    propVal = System.getenv(placeholderName);
                }
                return propVal;
            } catch (Throwable ex) {
                System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" + this.text + "] as system property: " + ex);
                return null;
            }
        }
    }
}
