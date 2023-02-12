package org.clever.data.redis.connection.convert;

import org.clever.core.convert.converter.Converter;

/**
 * 转换 Longs 到 Booleans
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:39 <br/>
 */
public class LongToBooleanConverter implements Converter<Long, Boolean> {
    public static final LongToBooleanConverter INSTANCE = new LongToBooleanConverter();

    @Override
    public Boolean convert(Long result) {
        return result == 1;
    }
}
