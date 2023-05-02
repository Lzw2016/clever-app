package org.clever.task.core.exception;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:06 <br/>
 */
public class JobExecutorException extends RuntimeException {
    public JobExecutorException(String message) {
        super(message);
    }

    public JobExecutorException(String message, Throwable cause) {
        super(message, cause);
    }
}
