package org.clever.js.api.module;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 08:26 <br/>
 */
public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException() {
    }

    public ModuleNotFoundException(String message) {
        super(message);
    }

    public ModuleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModuleNotFoundException(Throwable cause) {
        super(cause);
    }
}
