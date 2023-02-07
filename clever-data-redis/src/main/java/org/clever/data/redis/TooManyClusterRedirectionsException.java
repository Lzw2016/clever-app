package org.clever.data.redis;

import org.clever.dao.DataRetrievalFailureException;

/**
 * {@link DataRetrievalFailureException} 在以下集群重定向超过最大边数时抛出
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:49 <br/>
 */
public class TooManyClusterRedirectionsException extends DataRetrievalFailureException {
    public TooManyClusterRedirectionsException(String msg) {
        super(msg);
    }

    public TooManyClusterRedirectionsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
