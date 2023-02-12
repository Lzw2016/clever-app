package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.*;
import io.netty.channel.ChannelException;
import org.clever.core.convert.converter.Converter;
import org.clever.dao.DataAccessException;
import org.clever.dao.QueryTimeoutException;
import org.clever.data.redis.RedisConnectionFailureException;
import org.clever.data.redis.RedisSystemException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 将生菜异常转换为 {@link DataAccessException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:41 <br/>
 */
public class LettuceExceptionConverter implements Converter<Exception, DataAccessException> {
    public DataAccessException convert(Exception ex) {
        if (ex instanceof ExecutionException || ex instanceof RedisCommandExecutionException) {
            if (ex.getCause() != ex && ex.getCause() instanceof Exception) {
                return convert((Exception) ex.getCause());
            }
            return new RedisSystemException("Error in execution", ex);
        }
        if (ex instanceof DataAccessException) {
            return (DataAccessException) ex;
        }
        if (ex instanceof RedisCommandInterruptedException) {
            return new RedisSystemException("Redis command interrupted", ex);
        }
        if (ex instanceof ChannelException || ex instanceof RedisConnectionException) {
            return new RedisConnectionFailureException("Redis connection failed", ex);
        }
        if (ex instanceof TimeoutException || ex instanceof RedisCommandTimeoutException) {
            return new QueryTimeoutException("Redis command timed out", ex);
        }
        if (ex instanceof RedisException) {
            return new RedisSystemException("Redis exception", ex);
        }
        return null;
    }
}
