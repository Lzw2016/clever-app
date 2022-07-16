package org.clever.core.env;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 未找到所需属性时引发异常
 *
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:13 <br/>
 * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
 * @see ConfigurablePropertyResolver#validateRequiredProperties()
 */
public class MissingRequiredPropertiesException extends IllegalStateException {
    private final Set<String> missingRequiredProperties = new LinkedHashSet<>();

    void addMissingRequiredProperty(String key) {
        this.missingRequiredProperties.add(key);
    }

    @Override
    public String getMessage() {
        return "The following properties were declared as required but could not be resolved: " + getMissingRequiredProperties();
    }

    /**
     * 返回标记为必需但在验证时不存在的属性集
     * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
     * @see ConfigurablePropertyResolver#validateRequiredProperties()
     */
    public Set<String> getMissingRequiredProperties() {
        return this.missingRequiredProperties;
    }
}
