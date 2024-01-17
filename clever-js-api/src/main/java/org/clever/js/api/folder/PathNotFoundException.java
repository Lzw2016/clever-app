package org.clever.js.api.folder;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 10:48 <br/>
 */
public class PathNotFoundException extends RuntimeException {

    public PathNotFoundException() {
    }

    public PathNotFoundException(String message) {
        super(message);
    }

    public PathNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PathNotFoundException(Throwable cause) {
        super(cause);
    }
}
