package org.clever.core.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于处理注释内省失败(尤其是{@code TypeNotPresentExceptions})的日志外观。
 * 允许注释处理继续进行，前提是当类属性值不可解析时，注释应该有效地消失
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 14:13 <br/>
 */
enum IntrospectionFailureLogger {
    DEBUG {
        @Override
        public boolean isEnabled() {
            return getLogger().isDebugEnabled();
        }

        @Override
        public void log(String message) {
            getLogger().debug(message);
        }
    },

    INFO {
        @Override
        public boolean isEnabled() {
            return getLogger().isInfoEnabled();
        }

        @Override
        public void log(String message) {
            getLogger().info(message);
        }
    };

    private static Logger logger;

    void log(String message, Object source, Exception ex) {
        String on = (source != null ? " on " + source : "");
        log(message + on + ": " + ex);
    }

    abstract boolean isEnabled();

    abstract void log(String message);

    private static Logger getLogger() {
        Logger logger = IntrospectionFailureLogger.logger;
        if (logger == null) {
            logger = LoggerFactory.getLogger(MergedAnnotation.class);
            IntrospectionFailureLogger.logger = logger;
        }
        return logger;
    }
}
