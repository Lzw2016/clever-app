package org.clever.boot.context.config;

import org.clever.boot.origin.Origin;
import org.clever.util.Assert;

/**
 * 找不到 {@link ConfigDataLocation} 时引发 {@link ConfigDataNotFoundException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:19 <br/>
 */
public class ConfigDataLocationNotFoundException extends ConfigDataNotFoundException {
    private final ConfigDataLocation location;

    /**
     * 创建一个新的 {@link ConfigDataLocationNotFoundException}
     *
     * @param location 找不到的位置
     */
    public ConfigDataLocationNotFoundException(ConfigDataLocation location) {
        this(location, null);
    }

    /**
     * 创建一个新的 {@link ConfigDataLocationNotFoundException}
     *
     * @param location 找不到的位置
     * @param cause    异常原因
     */
    public ConfigDataLocationNotFoundException(ConfigDataLocation location, Throwable cause) {
        this(location, getMessage(location), cause);
    }

    /**
     * 创建一个新的 {@link ConfigDataLocationNotFoundException}
     *
     * @param location 找不到的位置
     * @param message  异常消息
     * @param cause    异常原因
     */
    public ConfigDataLocationNotFoundException(ConfigDataLocation location, String message, Throwable cause) {
        super(message, cause);
        Assert.notNull(location, "Location must not be null");
        this.location = location;
    }

    /**
     * 返回找不到的位置。
     *
     * @return 那个 location
     */
    public ConfigDataLocation getLocation() {
        return this.location;
    }

    @Override
    public Origin getOrigin() {
        return Origin.from(this.location);
    }

    @Override
    public String getReferenceDescription() {
        return getReferenceDescription(this.location);
    }

    private static String getMessage(ConfigDataLocation location) {
        return String.format("Config data %s cannot be found", getReferenceDescription(location));
    }

    private static String getReferenceDescription(ConfigDataLocation location) {
        return String.format("location '%s'", location);
    }
}
