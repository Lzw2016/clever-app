package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.KeyValue;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.async.RedisHashAsyncCommands;
import org.clever.data.redis.connection.RedisHashCommands;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.KeyBoundCursor;
import org.clever.data.redis.core.ScanIteration;
import org.clever.data.redis.core.ScanOptions;
import org.clever.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:36 <br/>
 */
class LettuceHashCommands implements RedisHashCommands {
    private final LettuceConnection connection;

    LettuceHashCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public Boolean hSet(byte[] key, byte[] field, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Field must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hset, key, field, value);
    }

    @Override
    public Boolean hSetNX(byte[] key, byte[] field, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Field must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hsetnx, key, field, value);
    }

    @Override
    public Long hDel(byte[] key, byte[]... fields) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(fields, "Fields must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hdel, key, fields);
    }

    @Override
    public Boolean hExists(byte[] key, byte[] field) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Fields must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hexists, key, field);
    }

    @Override
    public byte[] hGet(byte[] key, byte[] field) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Field must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hget, key, field);
    }

    @Override
    public Map<byte[], byte[]> hGetAll(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hgetall, key);
    }

    @Override
    public byte[] hRandField(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hrandfield, key);
    }

    @Override
    public Entry<byte[], byte[]> hRandFieldWithValues(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().from(RedisHashAsyncCommands::hrandfieldWithvalues, key).get(LettuceHashCommands::toEntry);
    }

    @Override
    public List<byte[]> hRandField(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hrandfield, key, count);
    }

    @Override
    public List<Entry<byte[], byte[]>> hRandFieldWithValues(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisHashAsyncCommands::hrandfieldWithvalues, key, count).toList(LettuceHashCommands::toEntry);
    }

    @Override
    public Long hIncrBy(byte[] key, byte[] field, long delta) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Field must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hincrby, key, field, delta);
    }

    @Override
    public Double hIncrBy(byte[] key, byte[] field, double delta) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Field must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hincrbyfloat, key, field, delta);
    }

    @Override
    public Set<byte[]> hKeys(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisHashAsyncCommands::hkeys, key).toSet();
    }

    @Override
    public Long hLen(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hlen, key);
    }

    @Override
    public List<byte[]> hMGet(byte[] key, byte[]... fields) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(fields, "Fields must not be null!");
        return connection.invoke().fromMany(RedisHashAsyncCommands::hmget, key, fields).toList(source -> source.getValueOrElse(null));
    }

    @Override
    public void hMSet(byte[] key, Map<byte[], byte[]> hashes) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(hashes, "Hashes must not be null!");
        connection.invokeStatus().just(RedisHashAsyncCommands::hmset, key, hashes);
    }

    @Override
    public List<byte[]> hVals(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hvals, key);
    }

    @Override
    public Cursor<Entry<byte[], byte[]>> hScan(byte[] key, ScanOptions options) {
        return hScan(key, 0, options);
    }

    public Cursor<Entry<byte[], byte[]>> hScan(byte[] key, long cursorId, ScanOptions options) {
        Assert.notNull(key, "Key must not be null!");
        // noinspection resource
        return new KeyBoundCursor<Entry<byte[], byte[]>>(key, cursorId, options) {
            @Override
            protected ScanIteration<Entry<byte[], byte[]>> doScan(byte[] key, long cursorId, ScanOptions options) {
                if (connection.isQueueing() || connection.isPipelined()) {
                    throw new UnsupportedOperationException("'HSCAN' cannot be called in pipeline / transaction mode.");
                }
                io.lettuce.core.ScanCursor scanCursor = connection.getScanCursor(cursorId);
                ScanArgs scanArgs = LettuceConverters.toScanArgs(options);
                MapScanCursor<byte[], byte[]> mapScanCursor = connection.invoke().just(RedisHashAsyncCommands::hscan, key, scanCursor, scanArgs);
                String nextCursorId = mapScanCursor.getCursor();
                Map<byte[], byte[]> values = mapScanCursor.getMap();
                return new ScanIteration<>(Long.parseLong(nextCursorId), values.entrySet());
            }

            @Override
            protected void doClose() {
                LettuceHashCommands.this.connection.close();
            }
        }.open();
    }

    @Override
    public Long hStrLen(byte[] key, byte[] field) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(field, "Field must not be null!");
        return connection.invoke().just(RedisHashAsyncCommands::hstrlen, key, field);
    }

    private static Entry<byte[], byte[]> toEntry(KeyValue<byte[], byte[]> value) {
        return value.hasValue() ? Converters.entryOf(value.getKey(), value.getValue()) : null;
    }
}
