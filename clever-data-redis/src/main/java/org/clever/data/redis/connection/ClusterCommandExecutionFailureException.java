package org.clever.data.redis.connection;

import org.clever.dao.UncategorizedDataAccessException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 当对集群 redis 环境的至少一次调用失败时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:50 <br/>
 */
public class ClusterCommandExecutionFailureException extends UncategorizedDataAccessException {
    private final Collection<? extends Throwable> causes;

    /**
     * 创建新的 {@link ClusterCommandExecutionFailureException}
     *
     * @param cause 不得为 {@literal null}
     */
    public ClusterCommandExecutionFailureException(Throwable cause) {
        this(Collections.singletonList(cause));
    }

    /**
     * 创建新的 {@link ClusterCommandExecutionFailureException}
     *
     * @param causes 不得为 {@literal empty}
     */
    public ClusterCommandExecutionFailureException(List<? extends Throwable> causes) {
        super(causes.get(0).getMessage(), causes.get(0));
        this.causes = causes;
        causes.forEach(this::addSuppressed);
    }

    /**
     * 获取收集的错误
     *
     * @return 从不为 {@literal null}.
     * @deprecated 请使用{@link #getSuppressed()}.
     */
    public Collection<? extends Throwable> getCauses() {
        return causes;
    }
}
