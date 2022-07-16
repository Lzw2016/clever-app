package org.clever.boot.logging;

import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.Environment;

/**
 * 在初始化期间传递给 {@link LoggingSystem} 的上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:39 <br/>
 */
public class LoggingInitializationContext {
    private final ConfigurableEnvironment environment;

    /**
     * 创建一个新的 {@link LoggingInitializationContext} 实例
     *
     * @param environment environment
     */
    public LoggingInitializationContext(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * 如果可用，返回 clever 环境
     *
     * @return {@link Environment} 或 null
     */
    public Environment getEnvironment() {
        return this.environment;
    }
}
