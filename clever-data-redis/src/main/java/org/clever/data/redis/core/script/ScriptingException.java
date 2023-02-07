package org.clever.data.redis.core.script;

import org.clever.core.NestedRuntimeException;

/**
 * {@link RedisScript} 出现问题时抛出 {@link RuntimeException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:25 <br/>
 */
public class ScriptingException extends NestedRuntimeException {
    public ScriptingException(String msg) {
        super(msg);
    }

    public ScriptingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
