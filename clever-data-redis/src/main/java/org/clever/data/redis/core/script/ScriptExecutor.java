package org.clever.data.redis.core.script;

import org.clever.data.redis.serializer.RedisSerializer;

import java.util.List;

/**
 * 执行 {@link RedisScript}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:57 <br/>
 *
 * @param <K> 脚本执行期间可能传递的keys类型
 */
public interface ScriptExecutor<K> {
    /**
     * 执行给定的 {@link RedisScript}
     *
     * @param script 要执行的脚本
     * @param keys   任何需要传递给脚本的键
     * @param args   任何需要传递给脚本的参数
     * @return 脚本的返回值，如果 {@link RedisScript#getResultType()} 为 null，则为 null，可能表示一次性状态回复（即“OK”）
     */
    <T> T execute(RedisScript<T> script, List<K> keys, Object... args);

    /**
     * 执行给定的 {@link RedisScript}，使用提供的 {@link RedisSerializer} 序列化脚本参数和结果。
     *
     * @param script           要执行的脚本
     * @param argsSerializer   用于序列化 args 的 {@link RedisSerializer}
     * @param resultSerializer 用于序列化脚本返回值的 {@link RedisSerializer}
     * @param keys             任何需要传递给脚本的keys
     * @param args             任何需要传递给脚本的参数
     * @return 脚本的返回值，如果 {@link RedisScript#getResultType()} 为 null，则为 null，可能表示一次性状态回复（即“OK”）
     */
    <T> T execute(RedisScript<T> script, RedisSerializer<?> argsSerializer, RedisSerializer<T> resultSerializer, List<K> keys, Object... args);
}
