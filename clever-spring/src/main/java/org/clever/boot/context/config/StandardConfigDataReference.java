package org.clever.boot.context.config;

import org.clever.boot.env.PropertySourceLoader;
import org.clever.util.StringUtils;

/**
 * 从原始{@link ConfigDataLocation}扩展的引用，最终可以解析为一个或多个{@link StandardConfigDataResource 资源}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:12 <br/>
 */
class StandardConfigDataReference {
    private final ConfigDataLocation configDataLocation;
    private final String resourceLocation;
    private final String directory;
    private final String profile;
    private final PropertySourceLoader propertySourceLoader;

    /**
     * 创建新的 {@link StandardConfigDataReference}
     *
     * @param configDataLocation   传递给解析器的原始位置
     * @param directory            资源的目录，如果引用是文件，则为null
     * @param root                 资源位置的根
     * @param profile              正在加载的配置文件
     * @param extension            资源的文件扩展名
     * @param propertySourceLoader 应用于此引用的属性源加载程序
     */
    StandardConfigDataReference(ConfigDataLocation configDataLocation,
                                String directory,
                                String root,
                                String profile,
                                String extension,
                                PropertySourceLoader propertySourceLoader) {
        this.configDataLocation = configDataLocation;
        String profileSuffix = (StringUtils.hasText(profile)) ? "-" + profile : "";
        this.resourceLocation = root + profileSuffix + ((extension != null) ? "." + extension : "");
        this.directory = directory;
        this.profile = profile;
        this.propertySourceLoader = propertySourceLoader;
    }

    ConfigDataLocation getConfigDataLocation() {
        return this.configDataLocation;
    }

    String getResourceLocation() {
        return this.resourceLocation;
    }

    boolean isMandatoryDirectory() {
        return !this.configDataLocation.isOptional() && this.directory != null;
    }

    String getDirectory() {
        return this.directory;
    }

    String getProfile() {
        return this.profile;
    }

    boolean isSkippable() {
        return this.configDataLocation.isOptional() || this.directory != null || this.profile != null;
    }

    PropertySourceLoader getPropertySourceLoader() {
        return this.propertySourceLoader;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        StandardConfigDataReference other = (StandardConfigDataReference) obj;
        return this.resourceLocation.equals(other.resourceLocation);
    }

    @Override
    public int hashCode() {
        return this.resourceLocation.hashCode();
    }

    @Override
    public String toString() {
        return this.resourceLocation;
    }
}
