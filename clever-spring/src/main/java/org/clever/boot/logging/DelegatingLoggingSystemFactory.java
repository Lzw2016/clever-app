package org.clever.boot.logging;

import java.util.List;
import java.util.function.Function;

/**
 * {@link LoggingSystemFactory} 委托给其他工厂
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:33 <br/>
 */
class DelegatingLoggingSystemFactory implements LoggingSystemFactory {
    private final Function<ClassLoader, List<LoggingSystemFactory>> delegates;

    /**
     * 创建一个新的{@link DelegatingLoggingSystemFactory}实例
     */
    DelegatingLoggingSystemFactory(Function<ClassLoader, List<LoggingSystemFactory>> delegates) {
        this.delegates = delegates;
    }

    @Override
    public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
        List<LoggingSystemFactory> delegates = (this.delegates != null) ? this.delegates.apply(classLoader) : null;
        if (delegates != null) {
            for (LoggingSystemFactory delegate : delegates) {
                LoggingSystem loggingSystem = delegate.getLoggingSystem(classLoader);
                if (loggingSystem != null) {
                    return loggingSystem;
                }
            }
        }
        return null;
    }
}
