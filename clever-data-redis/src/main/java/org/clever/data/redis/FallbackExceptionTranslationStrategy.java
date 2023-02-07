package org.clever.data.redis;

import org.clever.core.convert.converter.Converter;
import org.clever.dao.DataAccessException;

/**
 * {@link FallbackExceptionTranslationStrategy} 为未知的 {@link Exception} 返回 {@link RedisSystemException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:32 <br/>
 */
public class FallbackExceptionTranslationStrategy extends PassThroughExceptionTranslationStrategy {
    public FallbackExceptionTranslationStrategy(Converter<Exception, DataAccessException> converter) {
        super(converter);
    }

    @Override
    public DataAccessException translate(Exception e) {
        DataAccessException translated = super.translate(e);
        return translated != null ? translated : getFallback(e);
    }

    /**
     * 返回一个新的 {@link RedisSystemException} 包装给定的 {@link Exception}
     */
    protected RedisSystemException getFallback(Exception e) {
        return new RedisSystemException("Unknown redis exception", e);
    }
}
