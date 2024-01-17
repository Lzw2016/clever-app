package org.clever.js.api.module;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 09:35 <br/>
 */
public class UnSupportModuleException extends RuntimeException {
    public UnSupportModuleException() {
    }

    public UnSupportModuleException(String message) {
        super(message);
    }

    public UnSupportModuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnSupportModuleException(Throwable cause) {
        super(cause);
    }
}
