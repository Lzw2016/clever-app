package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.api.async.RedisHLLAsyncCommands;
import org.clever.data.redis.connection.RedisHyperLogLogCommands;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 18:11 <br/>
 */
class LettuceHyperLogLogCommands implements RedisHyperLogLogCommands {
    private final LettuceConnection connection;

    LettuceHyperLogLogCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public Long pfAdd(byte[] key, byte[]... values) {
        Assert.notEmpty(values, "PFADD requires at least one non 'null' value.");
        Assert.noNullElements(values, "Values for PFADD must not contain 'null'.");
        return connection.invoke().just(RedisHLLAsyncCommands::pfadd, key, values);
    }

    @Override
    public Long pfCount(byte[]... keys) {
        Assert.notEmpty(keys, "PFCOUNT requires at least one non 'null' key.");
        Assert.noNullElements(keys, "Keys for PFCOUNT must not contain 'null'.");
        return connection.invoke().just(RedisHLLAsyncCommands::pfcount, keys);
    }

    @Override
    public void pfMerge(byte[] destinationKey, byte[]... sourceKeys) {
        Assert.notNull(destinationKey, "Destination key must not be null");
        Assert.notNull(sourceKeys, "Source keys must not be null");
        Assert.noNullElements(sourceKeys, "Keys for PFMERGE must not contain 'null'.");
        connection.invoke().just(RedisHLLAsyncCommands::pfmerge, destinationKey, sourceKeys);
    }
}
