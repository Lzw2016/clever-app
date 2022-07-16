package org.clever.boot.context.config;

/**
 * 从包含 {@link ConfigDataResource} 和原始 {@link ConfigDataLocation} 的 {@link ConfigDataLocationResolvers} 返回的结果。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:28 <br/>
 */
class ConfigDataResolutionResult {
    private final ConfigDataLocation location;
    private final ConfigDataResource resource;
    private final boolean profileSpecific;

    ConfigDataResolutionResult(ConfigDataLocation location, ConfigDataResource resource, boolean profileSpecific) {
        this.location = location;
        this.resource = resource;
        this.profileSpecific = profileSpecific;
    }

    ConfigDataLocation getLocation() {
        return this.location;
    }

    ConfigDataResource getResource() {
        return this.resource;
    }

    boolean isProfileSpecific() {
        return this.profileSpecific;
    }
}
