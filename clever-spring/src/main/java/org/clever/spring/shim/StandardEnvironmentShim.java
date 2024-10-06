package org.clever.spring.shim;

import lombok.Getter;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * 包装 {@link org.springframework.core.env.StandardEnvironment} 类型
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/20 11:58 <br/>
 */
public class StandardEnvironmentShim extends AbstractEnvironment implements Shim<StandardEnvironment> {
    @Getter
    private final StandardEnvironment rawObj;

    public StandardEnvironmentShim(final ClassLoader classLoader, MutablePropertySources propertySources) {
        this.rawObj = ReflectionsUtils.newInstance(
            classLoader,
            "org.springframework.core.env.StandardEnvironment",
            new Class[]{
                MutablePropertySources.class,
            },
            new Object[]{
                propertySources,
            }
        );
    }

    public StandardEnvironmentShim(MutablePropertySources propertySources) {
        this(ReflectionsUtils.getDefClassLoader(), propertySources);
    }
}
