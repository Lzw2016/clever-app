package org.clever.core.flow;

/**
 * 任务定义
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:14 <br/>
 */
@FunctionalInterface
public interface Worker {
    Object execute(WorkerContext context);
}
