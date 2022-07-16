package org.clever.boot.context.config;

import org.clever.boot.context.properties.source.ConfigurationPropertySources;
import org.clever.boot.env.RandomValuePropertySource;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.io.DefaultResourceLoader;
import org.clever.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 用于读取配置文件，参考 {@code ConfigDataEnvironmentPostProcessor}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/15 17:39 <br/>
 */
public class ConfigDataBootstrap {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ResourceLoader resourceLoader;
    private final Collection<String> additionalProfiles;
    private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;

    public ConfigDataBootstrap() {
        this(
                new DefaultResourceLoader(),
                new ArrayList<>(),
                ConfigDataEnvironmentUpdateListener.NONE
        );
    }

    public ConfigDataBootstrap(ResourceLoader resourceLoader,
                               Collection<String> additionalProfiles,
                               ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
        this.resourceLoader = resourceLoader;
        this.additionalProfiles = additionalProfiles;
        this.environmentUpdateListener = environmentUpdateListener;
    }

    /**
     * 读取系统配置到 {@code environment} 中
     */
    public void init(StandardEnvironment environment) {
        ConfigurationPropertySources.attach(environment);
        RandomValuePropertySource.addToEnvironment(environment, logger);
        ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(
                environment, resourceLoader, additionalProfiles, environmentUpdateListener
        );
        configDataEnvironment.processAndApply();
    }
}
