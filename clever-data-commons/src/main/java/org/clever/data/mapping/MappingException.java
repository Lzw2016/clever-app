package org.clever.data.mapping;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/14 22:23 <br/>
 */
public class MappingException extends RuntimeException {
    public MappingException(String s) {
        super(s);
    }

    public MappingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
