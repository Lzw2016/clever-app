package org.clever.boot;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.spring.shim.ConfigDataEnvironmentShim;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentUpdateListener;
import org.springframework.boot.context.config.ConfigDataNotFoundAction;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

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
        // ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(binder, resourceLoader);
        // ConfigDataLoaders loaders = new ConfigDataLoaders(resourceLoader.getClassLoader());
        // ConfigDataImporter configDataImporter = new ConfigDataImporter(
        //     notFoundAction,
        //     resolvers,
        //     loaders
        // );
    }

    @SneakyThrows
    @Test
    public void t02() {
        DeferredLogFactory logFactory = Supplier::get;
        ConfigurableBootstrapContext bootstrapContext = new DefaultBootstrapContext();
        StandardEnvironment environment = new StandardEnvironment();
        ConfigurationPropertySources.attach(environment);
        RandomValuePropertySource.addToEnvironment(environment);
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Collection<String> additionalProfiles = new ArrayList<>();
        ConfigDataEnvironmentUpdateListener environmentUpdateListener = ConfigDataEnvironmentUpdateListener.NONE;
        ConfigDataEnvironmentShim configDataEnvironment = new ConfigDataEnvironmentShim(
            logFactory, bootstrapContext, environment, resourceLoader, additionalProfiles, environmentUpdateListener
        );
        configDataEnvironment.processAndApply();
        log.info("--> {}", environment);
        BindResult<LoadConfigDataTest.Server> result = Binder.get(environment).bind("clever.server", LoadConfigDataTest.Server.class);
        log.info("server --> {}", result.get());
    }

    @Data
    public static final class Server {
        private String address;
        private Integer port;
        private Duration timeout;
    }
}
