package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginProvider;

/**
 * 绑定失败时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:16 <br/>
 */
public class BindException extends RuntimeException implements OriginProvider {
    private final Bindable<?> target;
    private final ConfigurationProperty property;
    private final ConfigurationPropertyName name;

    BindException(ConfigurationPropertyName name, Bindable<?> target, ConfigurationProperty property, Throwable cause) {
        super(buildMessage(name, target), cause);
        this.name = name;
        this.target = target;
        this.property = property;
    }

    /**
     * 返回要绑定的配置属性的名称
     *
     * @return 配置属性名称
     */
    public ConfigurationPropertyName getName() {
        return this.name;
    }

    /**
     * 返回被绑定的目标
     *
     * @return 绑定目标
     */
    public Bindable<?> getTarget() {
        return this.target;
    }

    /**
     * 返回要绑定的项的配置属性名称
     *
     * @return 配置属性名称
     */
    public ConfigurationProperty getProperty() {
        return this.property;
    }

    @Override
    public Origin getOrigin() {
        return Origin.from(this.name);
    }

    private static String buildMessage(ConfigurationPropertyName name, Bindable<?> target) {
        return "Failed to bind properties"
                + ((name != null) ? " under '" + name + "'" : "")
                + " to " + target.getType();
    }
}
