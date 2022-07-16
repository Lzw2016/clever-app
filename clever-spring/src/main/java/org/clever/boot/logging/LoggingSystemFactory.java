package org.clever.boot.logging;

import org.clever.boot.logging.logback.LogbackLoggingSystem;

import java.util.Collections;

/**
 * {@link LoggingSystem#get(ClassLoader)} 用于查找实际实现的工厂类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:32 <br/>
 */
public interface LoggingSystemFactory {
    /**
     * 如果没有可用的日志系统，则返回日志系统实现或 null。
     *
     * @param classLoader the class loader to use
     * @return a logging system
     */
    LoggingSystem getLoggingSystem(ClassLoader classLoader);

    /**
     * 返回由 {@code clever.factories} 支持的 {@link LoggingSystemFactory}
     *
     * @return {@link LoggingSystemFactory} 实例
     */
    static LoggingSystemFactory fromSpringFactories() {
        return new DelegatingLoggingSystemFactory(
                (classLoader) -> Collections.singletonList(new LogbackLoggingSystem.Factory())
        );
    }
}
