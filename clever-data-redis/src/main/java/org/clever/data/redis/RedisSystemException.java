package org.clever.data.redis;

import org.clever.core.NestedRuntimeException;

/**
 * 当我们无法将Redis异常分类为Spring通用数据访问异常时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:22 <br/>
 */
public class RedisSystemException extends NestedRuntimeException {
    /**
     * @param msg   详细信息
     * @param cause 使用中的数据访问API的根本原因
     */
    public RedisSystemException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
