package org.clever.data.redis.connection;

import org.clever.core.NestedRuntimeException;

import java.util.Collections;
import java.util.List;

/**
 * 执行关闭包含一个或多个无效错误语句的管道时引发异常。
 * 异常还可能包含管道结果（如果驱动程序返回它），以便进行分析和跟踪。
 * <p>
 * 通常，管道返回的第一个异常用作此异常的<i>原因<i>，以便于调试。
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:47 <br/>
 */
public class RedisPipelineException extends NestedRuntimeException {
    private final List<Object> results;

    /**
     * 构造一个新的 RedisPipelineException 实例
     *
     * @param msg            消息
     * @param cause          原因
     * @param pipelineResult 管道结果
     */
    public RedisPipelineException(String msg, Throwable cause, List<Object> pipelineResult) {
        super(msg, cause);
        results = Collections.unmodifiableList(pipelineResult);
    }

    /**
     * 使用默认消息构造一个新的 RedisPipelineException 实例
     *
     * @param cause          原因
     * @param pipelineResult 管道结果
     */
    public RedisPipelineException(Exception cause, List<Object> pipelineResult) {
        this("Pipeline contained one or more invalid commands", cause, pipelineResult);
    }

    /**
     * 使用默认消息和空管道结果列表构造一个新的 RedisPipelineException 实例
     *
     * @param cause 原因
     */
    public RedisPipelineException(Exception cause) {
        this("Pipeline contained one or more invalid commands", cause, Collections.emptyList());
    }

    /**
     * 构造一个新的 RedisPipelineException 实例
     *
     * @param msg            消息
     * @param pipelineResult 管道部分结果
     */
    public RedisPipelineException(String msg, List<Object> pipelineResult) {
        super(msg);
        results = Collections.unmodifiableList(pipelineResult);
    }

    /**
     * （可选）返回导致异常的管道的结果。通常既包含成功语句的结果，也包含错误语句的异常。
     *
     * @return 管道的结果
     */
    public List<Object> getPipelineResult() {
        return results;
    }
}
