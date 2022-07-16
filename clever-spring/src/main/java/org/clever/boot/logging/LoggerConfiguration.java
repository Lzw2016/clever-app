package org.clever.boot.logging;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 表示{@link LoggingSystem}记录器配置的不可变类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:42 <br/>
 */
public final class LoggerConfiguration {
    private final String name;
    private final LogLevel configuredLevel;
    private final LogLevel effectiveLevel;

    /**
     * 创建一个新的{@link LoggerConfiguration 实例}
     *
     * @param name            记录器的名称
     * @param configuredLevel 记录器的配置级别
     * @param effectiveLevel  记录器的有效级别
     */
    public LoggerConfiguration(String name, LogLevel configuredLevel, LogLevel effectiveLevel) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(effectiveLevel, "EffectiveLevel must not be null");
        this.name = name;
        this.configuredLevel = configuredLevel;
        this.effectiveLevel = effectiveLevel;
    }

    /**
     * 返回记录器的配置级别。
     *
     * @return 记录器的配置级别
     */
    public LogLevel getConfiguredLevel() {
        return this.configuredLevel;
    }

    /**
     * 返回记录器的有效级别。
     *
     * @return 记录器的有效级别
     */
    public LogLevel getEffectiveLevel() {
        return this.effectiveLevel;
    }

    /**
     * 返回记录器的名称。
     *
     * @return 记录器的名称
     */
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof LoggerConfiguration) {
            LoggerConfiguration other = (LoggerConfiguration) obj;
            boolean rtn;
            rtn = ObjectUtils.nullSafeEquals(this.name, other.name);
            rtn = rtn && ObjectUtils.nullSafeEquals(this.configuredLevel, other.configuredLevel);
            rtn = rtn && ObjectUtils.nullSafeEquals(this.effectiveLevel, other.effectiveLevel);
            return rtn;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
        result = prime * result + ObjectUtils.nullSafeHashCode(this.configuredLevel);
        result = prime * result + ObjectUtils.nullSafeHashCode(this.effectiveLevel);
        return result;
    }

    @Override
    public String toString() {
        return "LoggerConfiguration [name=" + this.name +
                ", configuredLevel=" + this.configuredLevel +
                ", effectiveLevel=" + this.effectiveLevel +
                "]";
    }
}
