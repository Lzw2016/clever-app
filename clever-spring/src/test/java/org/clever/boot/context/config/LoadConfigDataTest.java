package org.clever.boot.context.config;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.context.properties.bind.BindResult;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.context.properties.source.ConfigurationPropertySources;
import org.clever.boot.env.RandomValuePropertySource;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.env.StandardEnvironmentTest;
import org.clever.core.io.DefaultResourceLoader;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 16:17 <br/>
 */
@Slf4j
public class LoadConfigDataTest {
    @SneakyThrows
    @Test
    public void t01() {
        StandardEnvironment environment = new StandardEnvironment();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Binder binder = Binder.get(environment);

        ConfigDataNotFoundAction notFoundAction = ConfigDataNotFoundAction.FAIL;
        ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(binder, resourceLoader);
        ConfigDataLoaders loaders = new ConfigDataLoaders(resourceLoader.getClassLoader());
        ConfigDataImporter configDataImporter = new ConfigDataImporter(
                notFoundAction,
                resolvers,
                loaders
        );
    }

    @SneakyThrows
    @Test
    public void t02() {
        StandardEnvironment environment = new StandardEnvironment();
        ConfigurationPropertySources.attach(environment);
        RandomValuePropertySource.addToEnvironment(environment, log);
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Collection<String> additionalProfiles = new ArrayList<>();
        ConfigDataEnvironmentUpdateListener environmentUpdateListener = ConfigDataEnvironmentUpdateListener.NONE;
        ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(
                environment, resourceLoader, additionalProfiles, environmentUpdateListener
        );
        configDataEnvironment.processAndApply();
        log.info("--> {}", environment);

        BindResult<StandardEnvironmentTest.Server> result = Binder.get(environment).bind("server", StandardEnvironmentTest.Server.class);
        log.info("server --> {}", result.get());
    }

    @Data
    public static final class Server {
        private String address;
        private Integer port;
        private Duration timeout;
    }
}
