package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当{@link ConfigurationPropertySource}元素未绑定时抛出{@link BindException}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:20 <br/>
 */
public class UnboundConfigurationPropertiesException extends RuntimeException {
    private final Set<ConfigurationProperty> unboundProperties;

    public UnboundConfigurationPropertiesException(Set<ConfigurationProperty> unboundProperties) {
        super(buildMessage(unboundProperties));
        this.unboundProperties = Collections.unmodifiableSet(unboundProperties);
    }

    public Set<ConfigurationProperty> getUnboundProperties() {
        return this.unboundProperties;
    }

    private static String buildMessage(Set<ConfigurationProperty> unboundProperties) {
        StringBuilder builder = new StringBuilder();
        builder.append("The elements [");
        String message = unboundProperties.stream().map((p) -> p.getName().toString()).collect(Collectors.joining(","));
        builder.append(message).append("] were left unbound.");
        return builder.toString();
    }
}
