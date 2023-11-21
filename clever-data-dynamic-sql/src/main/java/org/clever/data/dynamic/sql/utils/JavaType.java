package org.clever.data.dynamic.sql.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/21 15:38 <br/>
 */
public class JavaType {
    private static final Set<String> SUPPORT_JAVA_TYPE = new HashSet<>();

    static {
        SUPPORT_JAVA_TYPE.add("int");
        SUPPORT_JAVA_TYPE.add("long");
        SUPPORT_JAVA_TYPE.add("decimal");
        SUPPORT_JAVA_TYPE.add("char");
        SUPPORT_JAVA_TYPE.add("string");
        SUPPORT_JAVA_TYPE.add("date");
        SUPPORT_JAVA_TYPE.add("bool");
    }

    public static boolean supportType(String type) {
        return SUPPORT_JAVA_TYPE.contains(type);
    }

    public static String allType() {
        return StringUtils.join(SUPPORT_JAVA_TYPE, ",");
    }

    public static Object conv(String javaType, Object value) {
        if (StringUtils.isBlank(javaType) || value == null) {
            return value;
        }
        Object newValue = value;
        switch (javaType) {
            case "int":
                newValue = Conv.asInteger(value, null);
                break;
            case "long":
                newValue = Conv.asLong(value, null);
                break;
            case "decimal":
                newValue = Conv.asDecimal(value, null);
                break;
            case "char":
                newValue = Conv.asString(value, null);
                if (newValue != null) {
                    if (!newValue.toString().isEmpty()) {
                        newValue = newValue.toString().charAt(0);
                    } else {
                        newValue = null;
                    }
                }
                break;
            case "string":
                newValue = Conv.asString(value, null);
                break;
            case "date":
                newValue = Conv.asDate(value, null);
                break;
            case "bool":
                newValue = Conv.asBoolean(value, null);
                break;
        }
        if (newValue == null) {
            throw new RuntimeException("参数类型转换失败，javaType=" + javaType + "，" + "参数值=" + value);
        }
        return newValue;
    }
}
