package org.clever.data.redis.core.script;

import org.clever.core.io.Resource;
import org.clever.util.Assert;

/**
 * 使用 <a href="https://redis.io/commands/eval">Redis 脚本支持</a> 执行的脚本
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:17 <br/>
 *
 * @param <T> 脚本结果类型。应该是 Long、Boolean、List 或反序列化值类型之一。如果脚本返回一次性状态（即“OK”），则可以是 {@literal null}
 */
public interface RedisScript<T> {
    /**
     * @return 脚本的SHA1，用于执行Redis evalsha命令
     */
    String getSha1();

    /**
     * @return 脚本结果类型。应该是 Long、Boolean、List 或反序列化值类型之一。 {@literal null} 如果脚本返回一次性状态（即“OK”）
     */
    Class<T> getResultType();

    /**
     * @return 脚本内容
     */
    String getScriptAsString();

    /**
     * @return {@literal true} 如果结果类型是 {@literal null} 并且不需要任何进一步的反序列化
     */
    default boolean returnsRawValue() {
        return getResultType() == null;
    }

    /**
     * 从 {@code script} 创建新的 {@link RedisScript} 作为 {@link String}
     *
     * @param script 不得为 {@literal null}
     * @return {@link RedisScript} 的新实例
     */
    static <T> RedisScript<T> of(String script) {
        return new DefaultRedisScript<>(script);
    }

    /**
     * 从 {@code script} 创建新的 {@link RedisScript} 作为 {@link String}
     *
     * @param script     不得为 {@literal null}
     * @param resultType 不得为 {@literal null}
     * @return {@link RedisScript} 的新实例
     */
    static <T> RedisScript<T> of(String script, Class<T> resultType) {
        Assert.notNull(script, "Script must not be null!");
        Assert.notNull(resultType, "ResultType must not be null!");
        return new DefaultRedisScript<>(script, resultType);
    }

    /**
     * 从给定的 {@link Resource} 创建新的 {@link RedisScript}（带有丢弃结果）
     *
     * @param resource 不得为 {@literal null}
     * @return {@link RedisScript} 的新实例
     * @throws IllegalArgumentException 如果必需的参数是 {@literal null}
     */
    static <T> RedisScript<T> of(Resource resource) {
        Assert.notNull(resource, "Resource must not be null!");
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(resource);
        return script;
    }

    /**
     * 从 {@link Resource} 创建新的 {@link RedisScript}
     *
     * @param resource   不得为 {@literal null}
     * @param resultType 不得为 {@literal null}
     * @return {@link RedisScript} 的新实例
     * @throws IllegalArgumentException 如果任何必需的参数是 {@literal null}
     */
    static <T> RedisScript<T> of(Resource resource, Class<T> resultType) {
        Assert.notNull(resource, "Resource must not be null!");
        Assert.notNull(resultType, "ResultType must not be null!");
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setResultType(resultType);
        script.setLocation(resource);
        return script;
    }
}
