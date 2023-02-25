package org.clever.core;

import org.apache.commons.lang3.Conversion;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/01 11:38 <br/>
 */
public class Conv extends Conversion {
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

    public static Float asFloat(Object obj) {
        return asFloat(obj, 0F);
    }

    public static Float asFloat(Object obj, Float def) {
        Number result = asNumber(obj, def);
        if (result == null) {
            return null;
        }
        return result.floatValue();
    }

    public static Short asShort(Object obj) {
        return asShort(obj, (short) 0);
    }

    public static Short asShort(Object obj, Short def) {
        Number result = asNumber(obj, def);
        if (result == null) {
            return null;
        }
        return result.shortValue();
    }

    public static Double asDouble(Object obj) {
        return asDouble(obj, 0D);
    }

    public static Double asDouble(Object obj, Double def) {
        Number result = asNumber(obj, def);
        if (result == null) {
            return null;
        }
        return result.doubleValue();
    }

    public static Date asDate(Object obj) {
        return asDate(obj, new Date());
    }

    public static Date asDate(Object obj, Date def) {
        Date result = def;
        try {
            result = DateUtils.parseDate(obj);
        } catch (Exception ignored) {
        }
        return result;
    }

    public static Timestamp asTimestamp(Object obj, Timestamp def) {
        Timestamp result = def;
        try {
            Date date = DateUtils.parseDate(obj);
            if (date != null) {
                result = new Timestamp(date.getTime());
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static Timestamp asTimestamp(Object obj) {
        return asTimestamp(obj, new Timestamp(SystemClock.now()));
    }

    public static String asString(Object obj) {
        return asString(obj, StringUtils.EMPTY);
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
            return !Objects.equals(0, obj);
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

    public static String dateTimeStr(Date date) {
        return DateUtils.formatToString(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String timeStr(Date date) {
        return DateUtils.formatToString(date, "HH:mm:ss");
    }

    public static String dateStr(Date date) {
        return DateUtils.formatToString(date, "yyyy-MM-dd");
    }

    /**
     * 把一种类型，通过类型强制转换成另一种类型，转换失败抛出异常<br/>
     *
     * @param object 待转换的数据
     */
    @SuppressWarnings("unchecked")
    public static <E> E converter(Object object) {
        return (E) object;
    }

    /**
     * 把一种类型，通过类型强制转换成另一种类型，转换失败不会抛出异常<br/>
     * 1.注意：defaultValue与返回值类型一定要一致<br/>
     *
     * @param object       待转换的数据
     * @param defaultValue 转换失败返回的默认值
     */
    @SuppressWarnings("unchecked")
    public static <E> E converter(Object object, E defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        try {
            return (E) object;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 对象toString操作
     */
    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer
                || obj instanceof Long
                || obj instanceof Float
                || obj instanceof Double) {
            return new BigDecimal(obj.toString()).toString();
        }
        return obj.toString();
    }
}
