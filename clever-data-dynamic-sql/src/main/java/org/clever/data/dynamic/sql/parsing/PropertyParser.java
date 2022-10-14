package org.clever.data.dynamic.sql.parsing;

import java.util.Properties;

/**
 * "${variable}"变量字符串替换处理
 */
public class PropertyParser {
    private static final String KEY_PREFIX = "org.clever.data.dynamic.sql.parsing.PropertyParser.";
    /**
     * 指示是否对占位符启用默认值的特殊属性键<br />
     * 默认值是false（表示禁用占位符上的默认值）<br />
     * 如果指定true，则可以在占位符上指定键和默认值（例如 {@code ${db.username:postgres}}）<br />
     */
    public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

    /**
     * 为占位符上的键和默认值指定分隔符的特殊属性键，默认分隔符是 {@code ":"}
     */
    public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

    private static final String ENABLE_DEFAULT_VALUE = "false";
    private static final String DEFAULT_VALUE_SEPARATOR = ":";

    private PropertyParser() {
    }

    /**
     * "${variable}"变量字符串替换处理
     *
     * @param string    文本内容
     * @param variables 全局变量
     */
    public static String parse(String string, Properties variables) {
        VariableTokenHandler handler = new VariableTokenHandler(variables);
        GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
        return parser.parse(string);
    }

    /**
     * 处理"${variable}"变量替换
     */
    private static class VariableTokenHandler implements TokenHandler {
        /**
         * 全局变量
         */
        private final Properties variables;
        /**
         * 是否启用默认值
         */
        private final boolean enableDefaultValue;
        private final String defaultValueSeparator;

        private VariableTokenHandler(Properties variables) {
            this.variables = variables;
            this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
            this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
        }

        private String getPropertyValue(String key, String defaultValue) {
            return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
        }

        @Override
        public String handleToken(String content) {
            if (variables != null) {
                String key = content;
                if (enableDefaultValue) {
                    final int separatorIndex = content.indexOf(defaultValueSeparator);
                    String defaultValue = null;
                    if (separatorIndex >= 0) {
                        key = content.substring(0, separatorIndex);
                        defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
                    }
                    if (defaultValue != null) {
                        return variables.getProperty(key, defaultValue);
                    }
                }
                if (variables.containsKey(key)) {
                    return variables.getProperty(key);
                }
            }
            return "${" + content + "}";
        }
    }
}
