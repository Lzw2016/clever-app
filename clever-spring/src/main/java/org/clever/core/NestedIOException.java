package org.clever.core;

import java.io.IOException;

/**
 * IOException的子类，它正确地处理根本原因，并像NestedCheckedRuntimeException一样公开根本原因
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:26 <br/>
 *
 * @see org.clever.core.NestedCheckedException
 * @see org.clever.core.NestedRuntimeException
 */
public class NestedIOException extends IOException {
    static {
        // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
        // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
        // noinspection ResultOfMethodCallIgnored
        NestedExceptionUtils.class.getName();
    }

    public NestedIOException(String msg) {
        super(msg);
    }

    public NestedIOException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }
}
