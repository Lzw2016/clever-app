package org.clever.data.dynamic.sql.utils;

import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/21 14:48 <br/>
 */
public class Conv {
    public static final String HH_mm_ss = "HH:mm:ss";
    public static final String yyyy_MM_dd = "yyyy-MM-dd";
    public static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
    public static final String yyyy_MM_dd_HH_mm = "yyyy-MM-dd HH:mm";

    /**
     * 定义可能出现的时间日期格式<br />
     * 参考: <a href="https://www.jianshu.com/p/cf2f1f26dd0a">连接</a>
     */
    private static final String[] parsePatterns = {
        yyyy_MM_dd, yyyy_MM_dd_HH_mm_ss, yyyy_MM_dd_HH_mm,
        "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm",
        "yyyyMMdd", "yyyyMMdd HH:mm:ss", "yyyyMMdd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    };

    public static Number asNumber(Object obj, Number def) {
        if (obj == null) {
            return def;
        }
        if (obj instanceof BigInteger) {
            return (BigInteger) obj;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return (Number) obj;
        }
        try {
            return new BigDecimal(String.valueOf(obj));
        } catch (Exception e) {
            return def;
        }
    }

    public static Integer asInteger(Object obj) {
        return asInteger(obj, 0);
    }

    public static Integer asInteger(Object obj, Integer def) {
        Number result = asNumber(obj, def);
        if (result == null) {
            return null;
        }
        return result.intValue();
    }

    public static Long asLong(Object obj) {
        return asLong(obj, 0L);
    }

    public static Long asLong(Object obj, Long def) {
        Number result = asNumber(obj, def);
        if (result == null) {
            return null;
        }
        return result.longValue();
    }

    public static Date asDate(Object obj) {
        return asDate(obj, new Date());
    }

    public static Date asDate(Object obj, Date def) {
        Date result = def;
        try {
            result = parseDate(obj);
        } catch (Exception ignored) {
        }
        return result;
    }

    @SneakyThrows
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        if (str instanceof Date) {
            return (Date) str;
        }
        if (str instanceof Long || str instanceof Integer) {
            long time = (long) str;
            return new Date(time);
        }
        if (String.valueOf(str).length() != 8 && NumberUtils.isDigits(String.valueOf(str))) {
            long time = NumberUtils.toLong(String.valueOf(str), -1L);
            if (time != -1L) {
                return new Date(time);
            }
        }
        return DateUtils.parseDate(String.valueOf(str), parsePatterns);
    }

    public static String asString(Object obj) {
        return asString(obj, "");
    }

    public static String asString(Object obj, String def) {
        if (obj == null) {
            return def;
        }
        if (obj instanceof Integer) {
            return BigDecimal.valueOf((Integer) obj).toPlainString();
        }
        if (obj instanceof Float) {
            return BigDecimal.valueOf((Float) obj).toPlainString();
        }
        if (obj instanceof Long) {
            return BigDecimal.valueOf((Long) obj).toPlainString();
        }
        if (obj instanceof Double) {
            return BigDecimal.valueOf((Double) obj).toPlainString();
        }
        if (obj instanceof Number) {
            return new BigDecimal(String.valueOf(obj)).toPlainString();
        }
        try {
            return String.valueOf(obj);
        } catch (Exception e) {
            return def;
        }
    }

    public static Boolean asBoolean(Object obj) {
        return asBoolean(obj, false);
    }

    public static Boolean asBoolean(Object obj, Boolean def) {
        if (obj == null) {
            return def;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Number) {
            return !Objects.equals(0, ((Number) obj).intValue());
        }
        if (obj instanceof String) {
            String value = asString(obj);
            return (value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("y"));
        }
        try {
            return Boolean.parseBoolean(String.valueOf(obj));
        } catch (Exception e) {
            return def;
        }
    }

    public static BigDecimal asDecimal(Object obj) {
        return asDecimal(obj, BigDecimal.ZERO);
    }

    public static BigDecimal asDecimal(Object obj, BigDecimal def) {
        if (obj == null) {
            return def;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Float || obj instanceof Double) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        if (obj instanceof Long || obj instanceof Integer || obj instanceof Byte || obj instanceof Short) {
            return BigDecimal.valueOf(((Number) obj).longValue());
        }
        try {
            return new BigDecimal(String.valueOf(obj));
        } catch (Exception e) {
            return def;
        }
    }
}
