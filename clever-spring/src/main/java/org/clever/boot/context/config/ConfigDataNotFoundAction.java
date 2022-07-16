package org.clever.boot.context.config;

import org.slf4j.Logger;

/**
 * 抛出未捕获的 {@link ConfigDataNotFoundException} 时要采取的操作。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:42 <br/>
 */
public enum ConfigDataNotFoundAction {
    /**
     * 抛出异常导致启动失败。
     */
    FAIL {
        @Override
        void handle(Logger logger, ConfigDataNotFoundException ex) {
            throw ex;
        }
    },

    /**
     * 忽略异常并继续处理剩余位置。
     */
    IGNORE {
        @Override
        void handle(Logger logger, ConfigDataNotFoundException ex) {
            logger.trace(String.format("Ignoring missing config data %s", ex.getReferenceDescription()));
        }
    };

    /**
     * 处理给定的异常。
     *
     * @param logger 用于输出 {@code ConfigDataLocation} 的记录器
     * @param ex     要处理的异常
     */
    abstract void handle(Logger logger, ConfigDataNotFoundException ex);
}
