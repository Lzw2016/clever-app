package org.clever.core.env;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.context.properties.bind.BindResult;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.env.YamlPropertySourceLoader;
import org.clever.core.io.DefaultResourceLoader;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/06/19 22:41 <br/>
 */
@Slf4j
public class StandardEnvironmentTest {
    @Test
    public void t01() {
        System.setProperty("HOMEPATH", "C:\\Users\\lizw\\AppData\\Local\\override");
        StandardEnvironment standardEnvironment = new StandardEnvironment();
        log.info("### ActiveProfiles         --> {}", Arrays.asList(standardEnvironment.getActiveProfiles()));
        log.info("### HOMEPATH               --> {}", standardEnvironment.getProperty("HOMEPATH"));
        log.info("### HOMEPATH(Properties)   --> {}", standardEnvironment.getSystemProperties().get("HOMEPATH"));
        log.info("### HOMEPATH(Environment)  --> {}", standardEnvironment.getSystemEnvironment().get("HOMEPATH"));

//        log.info("### SystemProperties      -->");
//        standardEnvironment.getSystemProperties().forEach((k, v) -> log.info("\t {}={}", k, v));
//        log.info("### SystemEnvironment     -->");
//        standardEnvironment.getSystemEnvironment().forEach((k, v) -> log.info("\t {}={}", k, v));

        System.setProperty("HOMEPATH", "C:\\Users\\lizw\\AppData\\Local\\override!!!");
        log.info("### HOMEPATH               --> {}", standardEnvironment.getProperty("HOMEPATH"));
        log.info("### HOMEPATH(Properties)   --> {}", standardEnvironment.getSystemProperties().get("HOMEPATH"));
        log.info("### HOMEPATH(Environment)  --> {}", standardEnvironment.getSystemEnvironment().get("HOMEPATH"));
    }

    @SneakyThrows
    @Test
    public void t02() {
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        List<PropertySource<?>> list = yamlPropertySourceLoader.load(
                "application.yml",
                resourceLoader.getResource("classpath:application.yml")
        );
        MutablePropertySources mutablePropertySources = new MutablePropertySources();
        for (PropertySource<?> source : list) {
            log.info("source --> {}", source);
            mutablePropertySources.addFirst(source);
        }
        StandardEnvironment environment = new StandardEnvironment(mutablePropertySources);
        BindResult<Server> result = Binder.get(environment).bind("server", Server.class);
        log.info("server --> {}", result.get());
        // 加载dev
        list = yamlPropertySourceLoader.load(
                "application-dev.yml",
                resourceLoader.getResource("classpath:application-dev.yml")
        );
        for (PropertySource<?> source : list) {
            log.info("source --> {}", source);
            mutablePropertySources.addFirst(source);
        }
        result = Binder.get(environment).bind("server", Server.class);
        log.info("server --> {}", result.get());
    }

    @Data
    public static final class Server {
        private String address;
        private Integer port;
        private Duration timeout;
    }
}
