package org.clever.boot.context.logging;

import lombok.extern.slf4j.Slf4j;
import org.clever.boot.ansi.AnsiOutput;
import org.clever.boot.context.config.ConfigDataBootstrap;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.logging.LogLevel;
import org.clever.core.env.StandardEnvironment;
import org.clever.util.StringUtils;
import org.junit.jupiter.api.Test;

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
        Binder.get(environment).bind("clever.output.ansi.enabled", AnsiOutput.Enabled.class).ifBound(AnsiOutput::setEnabled);
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

    @Test
    public void t02() {
        StandardEnvironment environment = new StandardEnvironment();
        LoggingBootstrap loggingBootstrap = new LoggingBootstrap(Thread.currentThread().getContextClassLoader());
        loggingBootstrap.setLogLevel("root", LogLevel.TRACE);
        loggingBootstrap.setLogLevel("org.clever.core.env.OriginTrackedYamlLoader", LogLevel.DEBUG);
        loggingBootstrap.setLogLevel("org.clever.core.env.StandardEnvironment", LogLevel.DEBUG);
        loggingBootstrap.init(environment);
        log.trace("### #. -> {}", System.currentTimeMillis());
        log.debug("### #. -> {}", System.currentTimeMillis());
        log.info("### #. -> {}", System.currentTimeMillis());
        log.warn("### #. -> {}", System.currentTimeMillis());
        log.error("### #. -> {}", System.currentTimeMillis());
        log.info("### 1. -> {}", System.currentTimeMillis());
        ConfigDataBootstrap configDataBootstrap = new ConfigDataBootstrap();
        configDataBootstrap.init(environment);
        Binder.get(environment).bind("clever.output.ansi.enabled", AnsiOutput.Enabled.class).ifBound(AnsiOutput::setEnabled);
        loggingBootstrap.init(environment);
        log.debug("### 5. -> {}", System.currentTimeMillis());
        log.info("### 6. -> {}", System.currentTimeMillis());
        log.warn("### 7. -> {}", System.currentTimeMillis());
        log.error("### 8. -> {}", System.currentTimeMillis());
        loggingBootstrap.destroy();
        log.info("### 9. -> {}", System.currentTimeMillis());
    }
}
