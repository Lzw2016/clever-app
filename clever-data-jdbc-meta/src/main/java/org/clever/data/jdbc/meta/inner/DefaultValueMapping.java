package org.clever.data.jdbc.meta.inner;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.meta.model.Column;

/**
 * 不同数据库需要做映射 defaultValue
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/20 17:57 <br/>
 */
public class DefaultValueMapping {
    protected static String defaultValue(String defaultValue) {
        return StringUtils.trim(defaultValue);
    }

    public static String mysql(Column column) {
        return defaultValue(column.getDefaultValue());
    }

    public static String oracle(Column column) {
        return defaultValue(column.getDefaultValue());
    }

    public static String postgresql(Column column) {
        return defaultValue(column.getDefaultValue());
    }
}
