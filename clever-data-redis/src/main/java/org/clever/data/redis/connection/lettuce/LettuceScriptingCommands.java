package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.api.async.RedisScriptingAsyncCommands;
import org.clever.core.convert.converter.Converter;
import org.clever.data.redis.connection.RedisScriptingCommands;
import org.clever.data.redis.connection.ReturnType;
import org.clever.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:34 <br/>
 */
class LettuceScriptingCommands implements RedisScriptingCommands {
    private final LettuceConnection connection;

    LettuceScriptingCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public void scriptFlush() {
        connection.invoke().just(RedisScriptingAsyncCommands::scriptFlush);
    }

    @Override
    public void scriptKill() {
        if (connection.isQueueing()) {
            throw new UnsupportedOperationException("Script kill not permitted in a transaction");
        }
        connection.invoke().just(RedisScriptingAsyncCommands::scriptKill);
    }

    @Override
    public String scriptLoad(byte[] script) {
        Assert.notNull(script, "Script must not be null!");
        return connection.invoke().just(RedisScriptingAsyncCommands::scriptLoad, script);
    }

    @Override
    public List<Boolean> scriptExists(String... scriptSha1) {
        Assert.notNull(scriptSha1, "Script digests must not be null!");
        Assert.noNullElements(scriptSha1, "Script digests must not contain null elements!");
        return connection.invoke().just(RedisScriptingAsyncCommands::scriptExists, scriptSha1);
    }

    @Override
    public <T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        Assert.notNull(script, "Script must not be null!");
        byte[][] keys = extractScriptKeys(numKeys, keysAndArgs);
        byte[][] args = extractScriptArgs(numKeys, keysAndArgs);
        String convertedScript = LettuceConverters.toString(script);
        return connection.invoke().from(
                RedisScriptingAsyncCommands::eval, convertedScript, LettuceConverters.toScriptOutputType(returnType), keys, args
        ).get(new LettuceEvalResultsConverter<T>(returnType));
    }

    @Override
    public <T> T evalSha(String scriptSha1, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        Assert.notNull(scriptSha1, "Script digest must not be null!");
        byte[][] keys = extractScriptKeys(numKeys, keysAndArgs);
        byte[][] args = extractScriptArgs(numKeys, keysAndArgs);
        return connection.invoke().from(
                RedisScriptingAsyncCommands::evalsha, scriptSha1, LettuceConverters.toScriptOutputType(returnType), keys, args
        ).get(new LettuceEvalResultsConverter<T>(returnType));
    }

    @Override
    public <T> T evalSha(byte[] scriptSha1, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        Assert.notNull(scriptSha1, "Script digest must not be null!");
        return evalSha(LettuceConverters.toString(scriptSha1), returnType, numKeys, keysAndArgs);
    }

    private static byte[][] extractScriptKeys(int numKeys, byte[]... keysAndArgs) {
        if (numKeys > 0) {
            return Arrays.copyOfRange(keysAndArgs, 0, numKeys);
        }
        return new byte[0][0];
    }

    private static byte[][] extractScriptArgs(int numKeys, byte[]... keysAndArgs) {
        if (keysAndArgs.length > numKeys) {
            return Arrays.copyOfRange(keysAndArgs, numKeys, keysAndArgs.length);
        }
        return new byte[0][0];
    }

    private class LettuceEvalResultsConverter<T> implements Converter<Object, T> {
        private final ReturnType returnType;

        public LettuceEvalResultsConverter(ReturnType returnType) {
            this.returnType = returnType;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public T convert(Object source) {
            if (returnType == ReturnType.MULTI) {
                List resultList = (List) source;
                for (Object obj : resultList) {
                    if (obj instanceof Exception) {
                        throw connection.convertLettuceAccessException((Exception) obj);
                    }
                }
            }
            return (T) source;
        }
    }
}
