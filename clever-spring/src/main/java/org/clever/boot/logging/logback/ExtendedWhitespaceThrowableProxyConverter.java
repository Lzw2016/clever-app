package org.clever.boot.logging.logback;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;

/**
 * {@link ExtendedThrowableProxyConverter}，它在堆栈跟踪周围添加了一些额外的空白
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:11 <br/>
 */
public class ExtendedWhitespaceThrowableProxyConverter extends ExtendedThrowableProxyConverter {
    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        return CoreConstants.LINE_SEPARATOR + super.throwableProxyToString(tp) + CoreConstants.LINE_SEPARATOR;
    }
}
