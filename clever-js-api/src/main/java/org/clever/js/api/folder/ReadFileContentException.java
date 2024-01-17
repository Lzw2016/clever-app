package org.clever.js.api.folder;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 18:24 <br/>
 */
public class ReadFileContentException extends RuntimeException {

    public ReadFileContentException() {
    }

    public ReadFileContentException(String message) {
        super(message);
    }

    public ReadFileContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadFileContentException(Throwable cause) {
        super(cause);
    }
}
