package org.clever.data.redis.connection.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.data.redis.core.types.RedisClientInfo.RedisClientInfoBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Converter} 实现，以在给定的 {@link String} 数组中为每个行条目创建一个 {@link RedisClientInfo}
 *
 * <pre>{@code
 * ## sample of single line
 * addr=127.0.0.1:60311 fd=6 name= age=4059 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=32768 obl=0 oll=0 omem=0 events=r cmd=client
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:40 <br/>
 */
public class StringToRedisClientInfoConverter implements Converter<String[], List<RedisClientInfo>> {
    public static final StringToRedisClientInfoConverter INSTANCE = new StringToRedisClientInfoConverter();

    @Override
    public List<RedisClientInfo> convert(String[] lines) {
        List<RedisClientInfo> clientInfoList = new ArrayList<>(lines.length);
        for (String line : lines) {
            clientInfoList.add(RedisClientInfoBuilder.fromString(line));
        }
        return clientInfoList;
    }
}
