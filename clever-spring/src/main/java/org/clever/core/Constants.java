package org.clever.core;

import org.clever.util.Assert;
import org.clever.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 此类可用于解析公共静态final成员中包含常量定义的其他类。此类的{@code asXXXX}方法允许通过字符串名称访问这些常量值。
 * <p>考虑包含{@code public final static int CONSTANT1 = 66;}的类Foo；
 * 此类的一个实例包装{@code Foo.class}将从给定参数{@code "CONSTANT1"}的{@code asNumber}方法返回常量值66
 * <p>该类非常适合在PropertyEditor中使用，使它们能够识别与常量本身相同的名称，并使它们不必维护自己的映射。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:23 <br/>
 */
public class Constants {
    /**
     * 内省类的名称。
     */
    private final String className;
    /**
     * 从字符串字段名称映射到对象值
     */
    private final Map<String, Object> fieldCache = new HashMap<>();

    /**
     * 创建一个新的常量转换器类来包装给定的类。
     * <p>所有公共静态最终变量都将公开，无论其类型如何
     *
     * @param clazz 要分析的类
     * @throws IllegalArgumentException 如果提供的clazz为null
     */
    public Constants(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        this.className = clazz.getName();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (ReflectionUtils.isPublicStaticFinal(field)) {
                String name = field.getName();
                try {
                    Object value = field.get(null);
                    this.fieldCache.put(name, value);
                } catch (IllegalAccessException ex) {
                    // just leave this field and continue
                }
            }
        }
    }

    /**
     * 返回分析的类的名称
     */
    public final String getClassName() {
        return this.className;
    }

    /**
     * 返回公开的常量数
     */
    public final int getSize() {
        return this.fieldCache.size();
    }

    /**
     * 将字段缓存公开给子类：从字符串字段名到对象值的Map
     */
    protected final Map<String, Object> getFieldCache() {
        return this.fieldCache;
    }

    /**
     * 将常量值转换为数字
     *
     * @param code 字段的名称(从不为 {@code null})
     * @return 数字值
     * @throws ConstantException 如果未找到字段名或类型与数字不兼容
     * @see #asObject
     */
    public Number asNumber(String code) throws ConstantException {
        Object obj = asObject(code);
        if (!(obj instanceof Number)) {
            throw new ConstantException(this.className, code, "not a Number");
        }
        return (Number) obj;
    }

    /**
     * 以字符串形式返回常量值
     *
     * @param code 字段的名称(从不为 {@code null})
     * @return 字符串值即使不是字符串也有效(调用 toString 函数)
     * @throws ConstantException 如果未找到字段名
     * @see #asObject
     */
    public String asString(String code) throws ConstantException {
        return asObject(code).toString();
    }

    /**
     * 分析给定字符串(接受大写或小写)，如果它是我们正在分析的类中常量字段的名称，则返回适当的值
     *
     * @param code 字段的名称(从不为 {@code null})
     * @return 对象值
     * @throws ConstantException 如果没有这样的领域
     */
    public Object asObject(String code) throws ConstantException {
        Assert.notNull(code, "Code must not be null");
        String codeToUse = code.toUpperCase(Locale.ENGLISH);
        Object val = this.fieldCache.get(codeToUse);
        if (val == null) {
            throw new ConstantException(this.className, codeToUse, "not found");
        }
        return val;
    }

    /**
     * 返回给定常量组的所有名称。
     * <p>请注意，此方法假定常量的命名符合常量值的标准Java约定（即所有大写）。
     * 在该方法的主逻辑启动之前，所提供的{@code namePrefix}将大写（不区分区域设置）
     *
     * @param namePrefix 要搜索的常量名称的前缀(可能是 {@code null})
     * @return 常量名称集
     */
    public Set<String> getNames(String namePrefix) {
        String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
        Set<String> names = new HashSet<>();
        for (String code : this.fieldCache.keySet()) {
            if (code.startsWith(prefixToUse)) {
                names.add(code);
            }
        }
        return names;
    }

    /**
     * 返回给定bean属性名称的常量组的所有名称
     *
     * @param propertyName bean属性的名称
     * @return 值Set集合
     * @see #propertyToConstantNamePrefix
     */
    public Set<String> getNamesForProperty(String propertyName) {
        return getNames(propertyToConstantNamePrefix(propertyName));
    }

    /**
     * 返回给定常量组的所有名称。
     * <p>请注意，此方法假定常量的命名符合常量值的标准Java约定（即所有大写）。
     * 在该方法的主逻辑启动之前，所提供的{@code nameSuffix}将被大写（以不区分区域设置的方式）
     *
     * @param nameSuffix 要搜索的常量名称的后缀(可能是 {@code null})
     * @return 常量名称集
     */
    public Set<String> getNamesForSuffix(String nameSuffix) {
        String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
        Set<String> names = new HashSet<>();
        for (String code : this.fieldCache.keySet()) {
            if (code.endsWith(suffixToUse)) {
                names.add(code);
            }
        }
        return names;
    }

    /**
     * 返回给定常量组的所有值。
     * <p>请注意，此方法假定常量的命名符合常量值的标准Java约定（即所有大写）。
     * 在该方法的主逻辑启动之前，所提供的{@code namePrefix}将大写（不区分区域设置）
     *
     * @param namePrefix 要搜索的常量名称的前缀(可能是 {@code null})
     * @return 值集
     */
    public Set<Object> getValues(String namePrefix) {
        String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
        Set<Object> values = new HashSet<>();
        this.fieldCache.forEach((code, value) -> {
            if (code.startsWith(prefixToUse)) {
                values.add(value);
            }
        });
        return values;
    }

    /**
     * 返回给定bean属性名称的常量组的所有值。
     *
     * @param propertyName bean属性的名称
     * @return 值集
     * @see #propertyToConstantNamePrefix
     */
    public Set<Object> getValuesForProperty(String propertyName) {
        return getValues(propertyToConstantNamePrefix(propertyName));
    }

    /**
     * 返回给定常量组的所有值。
     * <p>请注意，此方法假定常量的命名符合常量值的标准Java约定（即所有大写）。
     * 在该方法的主逻辑启动之前，所提供的{@code namePrefix}将被大写（以不区分区域设置的方式）
     *
     * @param nameSuffix 要搜索的常量名称的后缀(可能是 {@code null})
     * @return 值集
     */
    public Set<Object> getValuesForSuffix(String nameSuffix) {
        String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
        Set<Object> values = new HashSet<>();
        this.fieldCache.forEach((code, value) -> {
            if (code.endsWith(suffixToUse)) {
                values.add(value);
            }
        });
        return values;
    }

    /**
     * 在给定的常量组中查找给定值。
     * <p>将返回第一个匹配项。
     *
     * @param value      要查找的常量值
     * @param namePrefix 要搜索的常量名称的前缀(可能是 {@code null})
     * @return 常量字段的名称
     * @throws ConstantException 如果找不到值
     */
    public String toCode(Object value, String namePrefix) throws ConstantException {
        String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
        for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
            if (entry.getKey().startsWith(prefixToUse) && entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        throw new ConstantException(this.className, prefixToUse, value);
    }

    /**
     * 在给定bean属性名称的常量组中查找给定值。将返回第一个匹配项。
     *
     * @param value        要查找的常量值
     * @param propertyName bean属性的名称
     * @return 常量字段的名称
     * @throws ConstantException 如果找不到值
     * @see #propertyToConstantNamePrefix
     */
    public String toCodeForProperty(Object value, String propertyName) throws ConstantException {
        return toCode(value, propertyToConstantNamePrefix(propertyName));
    }

    /**
     * 在给定的常量组中查找给定值。
     * <p>将返回第一个匹配项。
     *
     * @param value      要查找的常量值
     * @param nameSuffix 要搜索的常量名称的后缀(可能是 {@code null})
     * @return 常量字段的名称
     * @throws ConstantException 如果找不到值
     */
    public String toCodeForSuffix(Object value, String nameSuffix) throws ConstantException {
        String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
        for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
            if (entry.getKey().endsWith(suffixToUse) && entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        throw new ConstantException(this.className, suffixToUse, value);
    }

    /**
     * 将给定的bean属性名称转换为常量名称前缀。
     * <p>使用常见的命名习惯用法：将所有小写字符转换为大写，并在大写字符前加下划线。
     * <pre>{@code
     * 示例: "imageSize" → "IMAGE_SIZE"
     * 示例: "imagesize" → "IMAGESIZE"
     * 示例: "ImageSize" → "_IMAGE_SIZE"
     * 示例: "IMAGESIZE" → "_I_M_A_G_E_S_I_Z_E"
     * }</pre>
     *
     * @param propertyName bean属性的名称
     * @return 对应的常量名称前缀
     * @see #getValuesForProperty
     * @see #toCodeForProperty
     */
    public String propertyToConstantNamePrefix(String propertyName) {
        StringBuilder parsedPrefix = new StringBuilder();
        for (int i = 0; i < propertyName.length(); i++) {
            char c = propertyName.charAt(i);
            if (Character.isUpperCase(c)) {
                parsedPrefix.append('_');
                parsedPrefix.append(c);
            } else {
                parsedPrefix.append(Character.toUpperCase(c));
            }
        }
        return parsedPrefix.toString();
    }

    /**
     * 当要求{@link Constants}类提供无效的常量名称时引发异常
     */
    public static class ConstantException extends IllegalArgumentException {
        /**
         * 请求无效的常量名称时引发。
         *
         * @param className 包含常量定义的类的名称
         * @param field     常量名称无效
         * @param message   问题描述
         */
        public ConstantException(String className, String field, String message) {
            super("Field '" + field + "' " + message + " in class [" + className + "]");
        }

        /**
         * 查找无效常量值时引发。
         *
         * @param className  包含常量定义的类的名称
         * @param namePrefix 搜索的常量名称的前缀
         * @param value      查找的常量值
         */
        public ConstantException(String className, String namePrefix, Object value) {
            super("No '" + namePrefix + "' field with value '" + value + "' found in class [" + className + "]");
        }
    }
}
