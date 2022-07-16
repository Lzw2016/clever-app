package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.*;
import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;

/**
 * 如果必须使用旧处理，则引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:35 <br/>
 */
final class UseLegacyConfigProcessingException extends ConfigDataException {
    /**
     * 用于触发旧处理的属性名称。
     */
    static final ConfigurationPropertyName PROPERTY_NAME = ConfigurationPropertyName.of("clever.config.use-legacy-processing");
    private static final Bindable<Boolean> BOOLEAN = Bindable.of(Boolean.class);
    private static final UseLegacyProcessingBindHandler BIND_HANDLER = new UseLegacyProcessingBindHandler();

    private final ConfigurationProperty configurationProperty;

    UseLegacyConfigProcessingException(ConfigurationProperty configurationProperty) {
        super("Legacy processing requested from " + configurationProperty, null);
        this.configurationProperty = configurationProperty;
    }

    /**
     * 返回请求使用旧处理的源配置属性。
     *
     * @return 配置属性配置属性
     */
    ConfigurationProperty getConfigurationProperty() {
        return this.configurationProperty;
    }

    /**
     * 如果{@link #PROPERTY_NAME}绑定为true，则抛出新的{@link UseLegacyConfigProcessingException}实例。
     *
     * @param binder 要使用的活页夹
     */
    static void throwIfRequested(Binder binder) {
        try {
            binder.bind(PROPERTY_NAME, BOOLEAN, BIND_HANDLER);
        } catch (BindException ex) {
            if (ex.getCause() instanceof UseLegacyConfigProcessingException) {
                throw (UseLegacyConfigProcessingException) ex.getCause();
            }
            throw ex;
        }
    }

    /**
     * {@link BindHandler} 用于检查旧处理属性。
     */
    private static class UseLegacyProcessingBindHandler implements BindHandler {
        @Override
        public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
            if (Boolean.TRUE.equals(result)) {
                throw new UseLegacyConfigProcessingException(context.getConfigurationProperty());
            }
            return result;
        }
    }
}
