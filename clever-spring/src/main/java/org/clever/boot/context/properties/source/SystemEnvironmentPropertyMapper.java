package org.clever.boot.context.properties.source;

import org.clever.boot.context.properties.source.ConfigurationPropertyName.Form;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

/**
 * {@link PropertyMapper}用于系统环境变量。
 * 通过删除无效字符、转换为小写并将“_”替换为“.”来映射名称。
 * 例如，“SERVER_PORT”映射到“server.port”。
 * 此外，数字元素映射到索引（例如，“HOST_0”映射到“host[0]”）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:48 <br/>
 *
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class SystemEnvironmentPropertyMapper implements PropertyMapper {
    public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

    @Override
    public List<String> map(ConfigurationPropertyName configurationPropertyName) {
        String name = convertName(configurationPropertyName);
        String legacyName = convertLegacyName(configurationPropertyName);
        if (name.equals(legacyName)) {
            return Collections.singletonList(name);
        }
        return Arrays.asList(name, legacyName);
    }

    private String convertName(ConfigurationPropertyName name) {
        return convertName(name, name.getNumberOfElements());
    }

    private String convertName(ConfigurationPropertyName name, int numberOfElements) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfElements; i++) {
            if (result.length() > 0) {
                result.append('_');
            }
            result.append(name.getElement(i, Form.UNIFORM).toUpperCase(Locale.ENGLISH));
        }
        return result.toString();
    }

    private String convertLegacyName(ConfigurationPropertyName name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.getNumberOfElements(); i++) {
            if (result.length() > 0) {
                result.append('_');
            }
            result.append(convertLegacyNameElement(name.getElement(i, Form.ORIGINAL)));
        }
        return result.toString();
    }

    private Object convertLegacyNameElement(String element) {
        return element.replace('-', '_').toUpperCase(Locale.ENGLISH);
    }

    @Override
    public ConfigurationPropertyName map(String propertySourceName) {
        return convertName(propertySourceName);
    }

    private ConfigurationPropertyName convertName(String propertySourceName) {
        try {
            return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
        } catch (Exception ex) {
            return ConfigurationPropertyName.EMPTY;
        }
    }

    private CharSequence processElementValue(CharSequence value) {
        String result = value.toString().toLowerCase(Locale.ENGLISH);
        return isNumber(result) ? "[" + result + "]" : result;
    }

    private static boolean isNumber(String string) {
        return string.chars().allMatch(Character::isDigit);
    }

    @Override
    public BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
        return this::isAncestorOf;
    }

    private boolean isAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
        return name.isAncestorOf(candidate) || isLegacyAncestorOf(name, candidate);
    }

    private boolean isLegacyAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
        if (!hasDashedEntries(name)) {
            return false;
        }
        ConfigurationPropertyName legacyCompatibleName = buildLegacyCompatibleName(name);
        return legacyCompatibleName != null && legacyCompatibleName.isAncestorOf(candidate);
    }

    private ConfigurationPropertyName buildLegacyCompatibleName(ConfigurationPropertyName name) {
        StringBuilder legacyCompatibleName = new StringBuilder();
        for (int i = 0; i < name.getNumberOfElements(); i++) {
            if (i != 0) {
                legacyCompatibleName.append('.');
            }
            legacyCompatibleName.append(name.getElement(i, Form.DASHED).replace('-', '.'));
        }
        return ConfigurationPropertyName.ofIfValid(legacyCompatibleName);
    }

    boolean hasDashedEntries(ConfigurationPropertyName name) {
        for (int i = 0; i < name.getNumberOfElements(); i++) {
            if (name.getElement(i, Form.DASHED).indexOf('-') != -1) {
                return true;
            }
        }
        return false;
    }
}
