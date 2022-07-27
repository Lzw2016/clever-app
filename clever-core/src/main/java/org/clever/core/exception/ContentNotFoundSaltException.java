package org.clever.core.exception;

public class ContentNotFoundSaltException extends RuntimeException {
    public ContentNotFoundSaltException() {
        super("Unable to find salt in content.");
    }

    public ContentNotFoundSaltException(String content, String salt) {
        super("Unable to find salt[" + salt + "] in content[" + content + "].");
    }
}
