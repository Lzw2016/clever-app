package org.clever.util;

/**
 * 处理错误的策略。
 * 这对于处理异步执行已提交给 TaskScheduler 的任务期间发生的错误特别有用。
 * 在这种情况下，可能无法将错误抛给原始调用者。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 10:24 <br/>
 */
@FunctionalInterface
public interface ErrorHandler {
    /**
     * 处理给定的错误，可能将其作为致命异常重新抛出
     */
    void handleError(Throwable t);
}
