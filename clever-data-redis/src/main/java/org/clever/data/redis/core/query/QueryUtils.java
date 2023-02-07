package org.clever.data.redis.core.query;

import org.clever.data.redis.connection.DefaultSortParameters;
import org.clever.data.redis.connection.SortParameters;
import org.clever.data.redis.serializer.RedisSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link SortQuery} 实现的实用程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:15 <br/>
 */
public abstract class QueryUtils {
    public static <K> SortParameters convertQuery(SortQuery<K> query, RedisSerializer<String> stringSerializer) {
        return new DefaultSortParameters(
                stringSerializer.serialize(query.getBy()),
                query.getLimit(),
                serialize(query.getGetPattern(), stringSerializer),
                query.getOrder(),
                query.isAlphabetic()
        );
    }

    private static byte[][] serialize(List<String> strings, RedisSerializer<String> stringSerializer) {
        List<byte[]> raw;
        if (strings == null) {
            raw = Collections.emptyList();
        } else {
            raw = new ArrayList<>(strings.size());
            for (String key : strings) {
                raw.add(stringSerializer.serialize(key));
            }
        }
        return raw.toArray(new byte[raw.size()][]);
    }
}
