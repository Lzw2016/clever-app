package org.clever.data.redis;

import org.clever.core.convert.converter.Converter;
import org.clever.dao.DataAccessException;

/**
 * {@link PassThroughExceptionTranslationStrategy} 为未知的 {@link Exception} 返回 {@literal null}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:33 <br/>
 */
public class PassThroughExceptionTranslationStrategy implements ExceptionTranslationStrategy {
    private final Converter<Exception, DataAccessException> converter;

    public PassThroughExceptionTranslationStrategy(Converter<Exception, DataAccessException> converter) {
        this.converter = converter;
    }

    @Override
    public DataAccessException translate(Exception e) {
        return this.converter.convert(e);
    }
}
