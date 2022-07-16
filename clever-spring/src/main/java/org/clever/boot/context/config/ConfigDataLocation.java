package org.clever.boot.context.config;

import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginProvider;
import org.clever.util.StringUtils;

import java.util.Objects;

/**
 * 可以 {@link ConfigDataLocationResolver 解析} 为一个或多个 {@link ConfigDataResource 配置数据资源} 的用户指定位置。
 * {@link ConfigDataLocation} 是 {@link String} 值的简单包装器。
 * 值的确切格式取决于底层技术，但通常是由前缀和路径组成的类似 URL 的语法。
 * 例如，{@code crypt:somehost/somepath}。
 * <p>
 * 位置可以是强制性的或 {@link #isOptional() 可选的} 。可选位置以 {@code optional:}为前缀
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:12 <br/>
 */
public final class ConfigDataLocation implements OriginProvider {
    /**
     * 用于指示 {@link ConfigDataResource} 是可选的前缀。
     */
    public static final String OPTIONAL_PREFIX = "optional:";

    private final boolean optional;
    private final String value;
    private final Origin origin;

    private ConfigDataLocation(boolean optional, String value, Origin origin) {
        this.value = value;
        this.optional = optional;
        this.origin = origin;
    }

    /**
     * 如果位置是可选的并且应该忽略，则返回 {@link ConfigDataNotFoundException}.
     *
     * @return 如果位置是可选的
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * 返回位置的值（始终不包括任何用户指定的{@code optional:} 前缀）。
     *
     * @return 位置值
     */
    public String getValue() {
        return this.value;
    }

    /**
     * 如果 {@link #getValue()} 具有指定的前缀，则返回
     *
     * @param prefix 要检查的前缀
     * @return 如果值有前缀
     */
    public boolean hasPrefix(String prefix) {
        return this.value.startsWith(prefix);
    }

    /**
     * 返回 {@link #getValue()} 并删除指定的前缀。
     * 如果该位置没有给定的前缀，则 {@link #getValue()} 将原样返回。
     *
     * @param prefix 要检查的前缀
     * @return 去掉前缀的值
     */
    public String getNonPrefixedValue(String prefix) {
        if (hasPrefix(prefix)) {
            return this.value.substring(prefix.length());
        }
        return this.value;
    }

    @Override
    public Origin getOrigin() {
        return this.origin;
    }

    /**
     * 返回一个 {@link ConfigDataLocation} 元素的数组，该数组是通过将此 {@link ConfigDataLocation} 拆分为分隔符 {@code ";"} 而构建的。
     *
     * @return 分割位置
     */
    public ConfigDataLocation[] split() {
        return split(";");
    }

    /**
     * 返回通过围绕指定分隔符拆分此 {@link ConfigDataLocation} 构建的 {@link ConfigDataLocation} 元素数组。
     *
     * @param delimiter 要拆分的分隔符
     * @return 分割位置
     */
    public ConfigDataLocation[] split(String delimiter) {
        String[] values = StringUtils.delimitedListToStringArray(toString(), delimiter);
        ConfigDataLocation[] result = new ConfigDataLocation[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = Objects.requireNonNull(of(values[i])).withOrigin(getOrigin());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConfigDataLocation other = (ConfigDataLocation) obj;
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return (!this.optional) ? this.value : OPTIONAL_PREFIX + this.value;
    }

    /**
     * 创建一个新的 {@link ConfigDataLocation} 具有特定的 {@link Origin}
     *
     * @param origin 要设置的origin
     * @return 一个新的 {@link ConfigDataLocation} 实例。
     */
    ConfigDataLocation withOrigin(Origin origin) {
        return new ConfigDataLocation(this.optional, this.value, origin);
    }

    /**
     * 从字符串创建新的 {@link ConfigDataLocation} 的工厂方法。
     *
     * @param location 位置字符串
     * @return 如果未提供位置，则为 {@link ConfigDataLocation} 实例或 null
     */
    public static ConfigDataLocation of(String location) {
        boolean optional = location != null && location.startsWith(OPTIONAL_PREFIX);
        String value = (!optional) ? location : location.substring(OPTIONAL_PREFIX.length());
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new ConfigDataLocation(optional, value, null);
    }
}
