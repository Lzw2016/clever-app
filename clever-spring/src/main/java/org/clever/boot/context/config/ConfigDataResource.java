package org.clever.boot.context.config;

/**
 * 可以从中加载 {@link ConfigData} 的单个资源。
 * 实现必须实现有效的{@link #equals(Object) equals}, {@link #hashCode() hashCode} 和 {@link #toString() toString}方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:10 <br/>
 */
public abstract class ConfigDataResource {
    private final boolean optional;

    /**
     * 创建新的非可选 {@link ConfigDataResource} 实例
     */
    public ConfigDataResource() {
        this(false);
    }

    /**
     * 创建新的 {@link ConfigDataResource} 实例.
     *
     * @param optional 如果资源是可选的
     */
    protected ConfigDataResource(boolean optional) {
        this.optional = optional;
    }

    boolean isOptional() {
        return this.optional;
    }
}
