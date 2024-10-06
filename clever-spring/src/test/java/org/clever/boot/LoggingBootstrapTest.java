package org.clever.boot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.spring.boot.ConfigDataBootstrap;
import org.clever.spring.boot.LoggingBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/15 17:32 <br/>
 */
@Slf4j
public class LoggingBootstrapTest {
    @Test
    public void t01() {
        log.trace("### #. -> {}", System.currentTimeMillis());
        log.debug("### #. -> {}", System.currentTimeMillis());
        log.info("### #. -> {}", System.currentTimeMillis());
        log.warn("### #. -> {}", System.currentTimeMillis());
        log.error("### #. -> {}", System.currentTimeMillis());
        log.info("### 1. -> {}", System.currentTimeMillis());
        StandardEnvironment environment = new StandardEnvironment();
        ConfigDataBootstrap configDataBootstrap = new ConfigDataBootstrap();
        configDataBootstrap.init(environment);
        log.info("### 2. -> {}", System.currentTimeMillis());
        LoggingBootstrap loggingBootstrap = new LoggingBootstrap(Thread.currentThread().getContextClassLoader());
        log.info("### 3. -> {}", System.currentTimeMillis());
        Binder.get(environment).bind("spring.output.ansi.enabled", AnsiOutput.Enabled.class).ifBound(AnsiOutput::setEnabled);
        log.info("### 4. -> {}", System.currentTimeMillis());
        loggingBootstrap.init(environment);
        log.debug("### 5. -> {}", System.currentTimeMillis());
        log.info("### 6. -> {}", System.currentTimeMillis());
        log.warn("### 7. -> {}", System.currentTimeMillis());
        log.error("### 8. -> {}", System.currentTimeMillis());
        log.info("Activating profiles [{}]", StringUtils.arrayToCommaDelimitedString(environment.getActiveProfiles()));
        loggingBootstrap.destroy();
        log.info("### 9. -> {}", System.currentTimeMillis());
    }

    @SneakyThrows
    @Test
    public void t02() {
        StandardEnvironment environment = new StandardEnvironment();
        LoggingBootstrap loggingBootstrap = new LoggingBootstrap(Thread.currentThread().getContextClassLoader());
        loggingBootstrap.setLogLevel("root", LogLevel.TRACE);
        loggingBootstrap.setLogLevel("org.springframework.boot.env.OriginTrackedYamlLoader", LogLevel.DEBUG);
        loggingBootstrap.setLogLevel("org.springframework.core.env.StandardEnvironment", LogLevel.DEBUG);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        loggingBootstrap.init(environment);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        log.trace("### #. -> {}", System.currentTimeMillis());
        log.debug("### #. -> {}", System.currentTimeMillis());
        log.info("### #. -> {}", System.currentTimeMillis());
        log.warn("### #. -> {}", System.currentTimeMillis());
        log.error("### #. -> {}", System.currentTimeMillis());
        log.info("### 1. -> {}", System.currentTimeMillis());
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        log.debug("### 5. -> {}", System.currentTimeMillis());
        log.info("### 6. -> {}", System.currentTimeMillis());
        log.warn("### 7. -> {}", System.currentTimeMillis());
        log.error("### 8. -> {}", System.currentTimeMillis());
        loggingBootstrap.destroy();
        log.info("### 9. -> {}", System.currentTimeMillis());
    }
}
