package org.clever.boot.context.config;

/**
 * 如果不支持 {@link ConfigDataLocation}，则引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:28 <br/>
 */
public class UnsupportedConfigDataLocationException extends ConfigDataException {
    private final ConfigDataLocation location;

    /**
     * 创建新的 {@link UnsupportedConfigDataLocationException}
     *
     * @param location 不支持的位置
     */
    UnsupportedConfigDataLocationException(ConfigDataLocation location) {
        super("Unsupported config data location '" + location + "'", null);
        this.location = location;
    }

    /**
     * 返回不支持的位置引用。
     *
     * @return 不支持的位置引用
     */
    public ConfigDataLocation getLocation() {
        return this.location;
    }
}
