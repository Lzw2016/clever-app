package org.clever.spring.boot;

import org.clever.spring.shim.ConfigDataEnvironmentShim;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentUpdateListener;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 用于读取配置文件，参考 {@code ConfigDataEnvironmentPostProcessor}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/15 17:39 <br/>
 */
public class ConfigDataBootstrap {
    protected volatile boolean initialized = false;
    private final ResourceLoader resourceLoader;
    private final Collection<String> additionalProfiles;
    private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;

    public ConfigDataBootstrap() {
        this(new DefaultResourceLoader(), new ArrayList<>(), ConfigDataEnvironmentUpdateListener.NONE);
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
    public void init(StandardEnvironment environment, String[] args) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        if (args != null && args.length > 0) {
            MutablePropertySources sources = environment.getPropertySources();
            sources.addFirst(new SimpleCommandLinePropertySource(args));
        }
        ConfigurationPropertySources.attach(environment);
        RandomValuePropertySource.addToEnvironment(environment);
        DeferredLogFactory logFactory = Supplier::get;
        // DeferredLogFactory logFactory = new DeferredLogs();
        ConfigurableBootstrapContext bootstrapContext = new DefaultBootstrapContext();
        ConfigDataEnvironmentShim configDataEnvironment = new ConfigDataEnvironmentShim(
            logFactory, bootstrapContext, environment, resourceLoader, additionalProfiles, environmentUpdateListener
        );
        configDataEnvironment.processAndApply();
    }

    /**
     * 读取系统配置到 {@code environment} 中
     */
    public void init(StandardEnvironment environment) {
        init(environment, null);
    }
}
