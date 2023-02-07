package org.clever.data.redis.core.script;

import org.clever.dao.NonTransientDataAccessException;
import org.clever.data.redis.serializer.RedisElementReader;
import org.clever.data.redis.serializer.RedisSerializer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于 Lua 脚本执行和结果反序列化的实用程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:58 <br/>
 */
class ScriptUtils {
    private ScriptUtils() {
    }

    /**
     * 使用 {@link RedisSerializer} 将 {@code result} 反序列化为序列化程序类型。集合类型和中间集合元素被递归反序列化。
     *
     * @param resultSerializer 不得为 {@literal null}
     * @param result           不得为 {@literal null}
     * @return 反序列化的结果
     */
    @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
    static <T> T deserializeResult(RedisSerializer<T> resultSerializer, Object result) {
        if (result instanceof byte[]) {
            return resultSerializer.deserialize((byte[]) result);
        }
        if (result instanceof List) {
            List<Object> results = new ArrayList<>(((List) result).size());
            for (Object obj : (List) result) {
                results.add(deserializeResult(resultSerializer, obj));
            }
            return (T) results;
        }
        return (T) result;
    }

    /**
     * 使用 {@link RedisElementReader} 将 {@code result} 反序列化为阅读器类型。集合类型和中间集合元素被递归反序列化。
     *
     * @param reader 不得为 {@literal null}
     * @param result 不得为 {@literal null}
     * @return 反序列化的结果
     */
    @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
    static <T> T deserializeResult(RedisElementReader<T> reader, Object result) {
        if (result instanceof ByteBuffer) {
            return reader.read((ByteBuffer) result);
        }
        if (result instanceof List) {
            List<Object> results = new ArrayList<>(((List) result).size());
            for (Object obj : (List) result) {
                results.add(deserializeResult(reader, obj));
            }
            return (T) results;
        }
        return (T) result;
    }

    /**
     * 检查给定的 {@link Throwable} 是否包含 {@code NOSCRIPT} 错误。如果尝试使用 {@code EVALSHA} 执行脚本，则会报告 {@code NOSCRIPT}。
     *
     * @param e 异常
     * @return {@literal true} 如果异常或其原因之一包含 {@literal NOSCRIPT} 错误
     */
    static boolean exceptionContainsNoScriptError(Throwable e) {
        if (!(e instanceof NonTransientDataAccessException)) {
            return false;
        }
        Throwable current = e;
        while (current != null) {
            String exMessage = current.getMessage();
            if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
