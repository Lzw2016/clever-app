package org.clever.boot.logging;

import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * 利用 SLF4J 的 {@link LoggingSystem} 实现的抽象基类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:52 <br/>
 */
public abstract class Slf4JLoggingSystem extends AbstractLoggingSystem {
    private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

    public Slf4JLoggingSystem(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public void beforeInitialize() {
        super.beforeInitialize();
        configureJdkLoggingBridgeHandler();
    }

    @Override
    public void cleanUp() {
        if (isBridgeHandlerAvailable()) {
            removeJdkLoggingBridgeHandler();
        }
    }

    @Override
    protected void loadConfiguration(LoggingInitializationContext initializationContext, String location, LogFile logFile) {
        Assert.notNull(location, "Location must not be null");
        if (initializationContext != null) {
            applySystemProperties(initializationContext.getEnvironment(), logFile);
        }
    }

    private void configureJdkLoggingBridgeHandler() {
        try {
            if (isBridgeJulIntoSlf4j()) {
                removeJdkLoggingBridgeHandler();
                SLF4JBridgeHandler.install();
            }
        } catch (Throwable ex) {
            // Ignore. No java.util.logging bridge is installed.
        }
    }

    /**
     * 返回是否将 JUL 桥接到 SLF4J。
     *
     * @return 是否将 JUL 桥接到 SLF4J
     */
    protected final boolean isBridgeJulIntoSlf4j() {
        return isBridgeHandlerAvailable() && isJulUsingASingleConsoleHandlerAtMost();
    }

    protected final boolean isBridgeHandlerAvailable() {
        return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
    }

    private boolean isJulUsingASingleConsoleHandlerAtMost() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
    }

    private void removeJdkLoggingBridgeHandler() {
        try {
            removeDefaultRootHandler();
            SLF4JBridgeHandler.uninstall();
        } catch (Throwable ex) {
            // Ignore and continue
        }
    }

    private void removeDefaultRootHandler() {
        try {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
                rootLogger.removeHandler(handlers[0]);
            }
        } catch (Throwable ex) {
            // Ignore and continue
        }
    }
}
