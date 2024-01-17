package org.clever.js.api.module;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 10:34 <br/>
 */
public class LoadModuleException extends RuntimeException {
    public LoadModuleException() {
    }

    public LoadModuleException(String message) {
        super(message);
    }

    public LoadModuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadModuleException(Throwable cause) {
        super(cause);
    }
}
