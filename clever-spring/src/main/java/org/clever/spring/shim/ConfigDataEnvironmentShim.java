package org.clever.spring.shim;

import lombok.Getter;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentUpdateListener;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

import java.util.Collection;

/**
 * 包装 {@link org.springframework.boot.context.config.ConfigDataEnvironment} 类型
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/20 11:34 <br/>
 */
@SuppressWarnings("JavadocReference")
public class ConfigDataEnvironmentShim implements Shim<Object> {
    /**
     * 原始的 {@link org.springframework.boot.context.config.ConfigDataEnvironment} 对象
     */
    @Getter
    private final Object rawObj;

    public ConfigDataEnvironmentShim(final ClassLoader classLoader,
                                     DeferredLogFactory logFactory,
                                     ConfigurableBootstrapContext bootstrapContext,
                                     ConfigurableEnvironment environment,
                                     ResourceLoader resourceLoader,
                                     Collection<String> additionalProfiles,
                                     ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
        this.rawObj = ReflectionsUtils.newInstance(
            classLoader,
            "org.springframework.boot.context.config.ConfigDataEnvironment",
            new Class[]{
                DeferredLogFactory.class,
                ConfigurableBootstrapContext.class,
                ConfigurableEnvironment.class,
                ResourceLoader.class,
                Collection.class,
                ConfigDataEnvironmentUpdateListener.class,
            },
            new Object[]{
                logFactory,
                bootstrapContext,
                environment,
                resourceLoader,
                additionalProfiles,
                environmentUpdateListener,
            }
        );
    }

    public ConfigDataEnvironmentShim(DeferredLogFactory logFactory,
                                     ConfigurableBootstrapContext bootstrapContext,
                                     ConfigurableEnvironment environment,
                                     ResourceLoader resourceLoader,
                                     Collection<String> additionalProfiles,
                                     ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
        this(
            ReflectionsUtils.getDefClassLoader(),
            logFactory,
            bootstrapContext,
            environment,
            resourceLoader,
            additionalProfiles,
            environmentUpdateListener
        );
    }

    public void processAndApply() {
        ReflectionsUtils.invokeMethod(rawObj, "processAndApply", new Class[0], new Object[0]);
    }
}
