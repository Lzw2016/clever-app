package org.clever.data.redis.core.script;

import org.clever.data.redis.RedisSystemException;
import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.ReturnType;
import org.clever.data.redis.core.RedisCallback;
import org.clever.data.redis.core.RedisTemplate;
import org.clever.data.redis.serializer.RedisSerializer;

import java.util.List;

/**
 * {@link ScriptExecutor} 的默认实现。
 * 通过首先尝试使用 evalsha 执行脚本来优化性能，然后在 Redis 尚未缓存脚本时回退到 eval。
 * 如果脚本在管道或事务中执行，则不会尝试 Evalsha。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:58 <br/>
 *
 * @param <K> The type of keys that may be passed during script execution
 */
public class DefaultScriptExecutor<K> implements ScriptExecutor<K> {
    private final RedisTemplate<K, ?> template;

    /**
     * @param template The {@link RedisTemplate} to use
     */
    public DefaultScriptExecutor(RedisTemplate<K, ?> template) {
        this.template = template;
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(final RedisScript<T> script, final List<K> keys, final Object... args) {
        // use the Template's value serializer for args and result
        return execute(script, template.getValueSerializer(), (RedisSerializer<T>) template.getValueSerializer(), keys, args);
    }

    public <T> T execute(final RedisScript<T> script,
                         final RedisSerializer<?> argsSerializer,
                         final RedisSerializer<T> resultSerializer,
                         final List<K> keys,
                         final Object... args) {
        return template.execute((RedisCallback<T>) connection -> {
            final ReturnType returnType = ReturnType.fromJavaType(script.getResultType());
            final byte[][] keysAndArgs = keysAndArgs(argsSerializer, keys, args);
            final int keySize = keys != null ? keys.size() : 0;
            if (connection.isPipelined() || connection.isQueueing()) {
                // 我们可以先加载脚本，然后执行 evalsha 以确保存在 sha，但这会向 exec/closePipeline 结果添加 sha1。相反，只是评估
                connection.eval(scriptBytes(script), returnType, keySize, keysAndArgs);
                return null;
            }
            return eval(connection, script, returnType, keySize, keysAndArgs, resultSerializer);
        });
    }

    protected <T> T eval(RedisConnection connection,
                         RedisScript<T> script,
                         ReturnType returnType,
                         int numKeys,
                         byte[][] keysAndArgs,
                         RedisSerializer<T> resultSerializer) {
        Object result;
        try {
            result = connection.evalSha(script.getSha1(), returnType, numKeys, keysAndArgs);
        } catch (Exception e) {
            if (!ScriptUtils.exceptionContainsNoScriptError(e)) {
                // noinspection ConstantConditions
                throw e instanceof RuntimeException ? (RuntimeException) e : new RedisSystemException(e.getMessage(), e);
            }
            result = connection.eval(scriptBytes(script), returnType, numKeys, keysAndArgs);
        }
        if (script.getResultType() == null) {
            return null;
        }
        return deserializeResult(resultSerializer, result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected byte[][] keysAndArgs(RedisSerializer argsSerializer, List<K> keys, Object[] args) {
        final int keySize = keys != null ? keys.size() : 0;
        byte[][] keysAndArgs = new byte[args.length + keySize][];
        int i = 0;
        if (keys != null) {
            for (K key : keys) {
                if (keySerializer() == null && key instanceof byte[]) {
                    keysAndArgs[i++] = (byte[]) key;
                } else {
                    keysAndArgs[i++] = keySerializer().serialize(key);
                }
            }
        }
        for (Object arg : args) {
            if (argsSerializer == null && arg instanceof byte[]) {
                keysAndArgs[i++] = (byte[]) arg;
            } else {
                // noinspection ConstantConditions
                keysAndArgs[i++] = argsSerializer.serialize(arg);
            }
        }
        return keysAndArgs;
    }

    protected byte[] scriptBytes(RedisScript<?> script) {
        return template.getStringSerializer().serialize(script.getScriptAsString());
    }

    protected <T> T deserializeResult(RedisSerializer<T> resultSerializer, Object result) {
        return ScriptUtils.deserializeResult(resultSerializer, result);
    }

    @SuppressWarnings("rawtypes")
    protected RedisSerializer keySerializer() {
        return template.getKeySerializer();
    }
}
