package org.clever.js.javet.support;

import com.caoccao.javet.interfaces.IJavetLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/19 11:48 <br/>
 */
public class JavetLogger implements IJavetLogger {
    public static final JavetLogger Instance = new JavetLogger(JavetLogger.class.getName());
    /**
     * 日志记录器
     */
    protected final org.slf4j.Logger logger;

    public JavetLogger(String name) {
        logger = LoggerFactory.getLogger(StringUtils.trimToEmpty(name));
    }

    @Override
    public void debug(String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    @Override
    public void error(String message) {
        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
    }

    @Override
    public void error(String message, Throwable cause) {
        if (logger.isErrorEnabled()) {
            logger.error(message, cause);
        }
    }

    @Override
    public void info(String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    @Override
    public void warn(String message) {
        if (logger.isWarnEnabled()) {
            logger.warn(message);
        }
    }
}
