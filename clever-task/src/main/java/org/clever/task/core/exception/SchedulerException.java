package org.clever.task.core.exception;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:46 <br/>
 */
public class SchedulerException extends RuntimeException {
    public SchedulerException(String message) {
        super(message);
    }

    public SchedulerException(String message, Throwable cause) {
        super(message, cause);
    }
}
