package org.clever.data.jdbc.support.sqlparser.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 18:44 <br/>
 */
public class FstSerialCaffeineJsqlParseCache extends AbstractCaffeineJsqlParseCache {
    public FstSerialCaffeineJsqlParseCache(Cache<String, byte[]> cache) {
        super(cache);
    }

    public FstSerialCaffeineJsqlParseCache(Consumer<Caffeine<Object, Object>> consumer) {
        super(consumer);
    }

    @Override
    public byte[] serialize(Object obj) {
        return FstFactory.getDefaultFactory().asByteArray(obj);
    }

    @Override
    public Object deserialize(String sql, byte[] bytes) {
        return FstFactory.getDefaultFactory().asObject(bytes);
    }
}
