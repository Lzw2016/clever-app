package org.clever.boot.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;

/**
 * {@link ThrowableProxyConverter}，它在堆栈跟踪周围添加了一些额外的空白
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:10 <br/>
 */
public class WhitespaceThrowableProxyConverter extends ThrowableProxyConverter {
    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        return CoreConstants.LINE_SEPARATOR + super.throwableProxyToString(tp) + CoreConstants.LINE_SEPARATOR;
    }
}
