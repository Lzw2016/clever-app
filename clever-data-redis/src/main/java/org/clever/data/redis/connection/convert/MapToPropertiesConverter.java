package org.clever.data.redis.connection.convert;

import org.clever.core.convert.converter.Converter;

import java.util.Map;
import java.util.Properties;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:27 <br/>
 */
public enum MapToPropertiesConverter implements Converter<Map<?, ?>, Properties> {
    INSTANCE;

    @Override
    public Properties convert(Map<?, ?> source) {
        Properties target = new Properties();
        target.putAll(source);
        return target;
    }
}
