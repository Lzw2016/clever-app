package org.clever.spring.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * 在启动时记录应用程序信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 22:14 <br/>
 */
public class StartupInfoLogger {
    private static final Logger logger = LoggerFactory.getLogger(StartupInfoLogger.class);
    private static final long HOST_NAME_RESOLVE_THRESHOLD = 200;

    private final Class<?> sourceClass;

    public StartupInfoLogger(Class<?> sourceClass) {
        this.sourceClass = sourceClass;
    }

    public void logStarting(Logger applicationLog) {
        Assert.notNull(applicationLog, "Log must not be null");
        if (applicationLog.isInfoEnabled()) {
            applicationLog.info(String.valueOf(this.getStartingMessage()));
        }
    }

    public void logStarted(Logger applicationLog, Duration timeTakenToStartup) {
        if (applicationLog.isInfoEnabled()) {
            applicationLog.info(String.valueOf(getStartedMessage(timeTakenToStartup)));
        }
    }

    private CharSequence getStartingMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Starting ");
        appendApplicationName(message);
        appendVersion(message, this.sourceClass);
        appendJavaVersion(message);
        appendOn(message);
        appendPid(message);
        appendContext(message);
        return message;
    }

    private CharSequence getStartedMessage(Duration timeTakenToStartup) {
        StringBuilder message = new StringBuilder();
        message.append("Started ");
        appendApplicationName(message);
        message.append(" in ");
        message.append(timeTakenToStartup.toMillis() / 1000.0);
        message.append(" seconds");
        try {
            double uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
            message.append(" (JVM running for ").append(uptime).append(")");
        } catch (Throwable ex) {
            // No JVM time available
        }
        return message;
    }

    private void appendApplicationName(StringBuilder message) {
        String name = (this.sourceClass != null) ? ClassUtils.getShortName(this.sourceClass) : "application";
        message.append(name);
    }

    private void appendVersion(StringBuilder message, Class<?> source) {
        append(message, "v", () -> source.getPackage().getImplementationVersion());
    }

    private void appendOn(StringBuilder message) {
        long startTime = System.currentTimeMillis();
        append(message, "on ", () -> InetAddress.getLocalHost().getHostName());
        long resolveTime = System.currentTimeMillis() - startTime;
        if (resolveTime > HOST_NAME_RESOLVE_THRESHOLD && logger.isWarnEnabled()) {
            StringBuilder warning = new StringBuilder();
            warning.append("InetAddress.getLocalHost().getHostName() took ");
            warning.append(resolveTime);
            warning.append(" milliseconds to respond.");
            warning.append(" Please verify your network configuration");
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                warning.append(" (macOS machines may need to add entries to /etc/hosts)");
            }
            warning.append(".");
            logger.warn(warning.toString());
        }
    }

    private void appendPid(StringBuilder message) {
        append(message, "with PID ", ApplicationPid::new);
    }

    private void appendContext(StringBuilder message) {
        StringBuilder context = new StringBuilder();
        ApplicationHome home = new ApplicationHome(this.sourceClass);
        if (home.getSource() != null) {
            context.append(home.getSource().getAbsolutePath());
        }
        append(context, "started by ", () -> System.getProperty("user.name"));
        append(context, "in ", () -> System.getProperty("user.dir"));
        if (!context.isEmpty()) {
            message.append(" (");
            message.append(context);
            message.append(")");
        }
    }

    private void appendJavaVersion(StringBuilder message) {
        append(message, "using Java ", () -> System.getProperty("java.version"));
    }

    private void append(StringBuilder message, String prefix, Callable<Object> call) {
        append(message, prefix, call, "");
    }

    @SuppressWarnings("SameParameterValue")
    private void append(StringBuilder message, String prefix, Callable<Object> call, String defaultValue) {
        Object result = callIfPossible(call);
        String value = (result != null) ? result.toString() : null;
        if (!StringUtils.hasLength(value)) {
            value = defaultValue;
        }
        if (StringUtils.hasLength(value)) {
            message.append(!message.isEmpty() ? " " : "");
            message.append(prefix);
            message.append(value);
        }
    }

    private Object callIfPossible(Callable<Object> call) {
        try {
            return call.call();
        } catch (Exception ex) {
            return null;
        }
    }
}
