package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.api.async.RedisSetAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.clever.data.redis.connection.RedisSetCommands;
import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.KeyBoundCursor;
import org.clever.data.redis.core.ScanIteration;
import org.clever.data.redis.core.ScanOptions;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:15 <br/>
 */
class LettuceSetCommands implements RedisSetCommands {
    private final LettuceConnection connection;

    LettuceSetCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public Long sAdd(byte[] key, byte[]... values) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(values, "Values must not be null!");
        Assert.noNullElements(values, "Values must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sadd, key, values);
    }

    @Override
    public Long sCard(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::scard, key);
    }

    @Override
    public Set<byte[]> sDiff(byte[]... keys) {
        Assert.notNull(keys, "Keys must not be null!");
        Assert.noNullElements(keys, "Keys must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sdiff, keys);
    }

    @Override
    public Long sDiffStore(byte[] destKey, byte[]... keys) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(keys, "Source keys must not be null!");
        Assert.noNullElements(keys, "Source keys must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sdiffstore, destKey, keys);
    }

    @Override
    public Set<byte[]> sInter(byte[]... keys) {
        Assert.notNull(keys, "Keys must not be null!");
        Assert.noNullElements(keys, "Keys must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sinter, keys);
    }

    @Override
    public Long sInterStore(byte[] destKey, byte[]... keys) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(keys, "Source keys must not be null!");
        Assert.noNullElements(keys, "Source keys must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sinterstore, destKey, keys);
    }

    @Override
    public Boolean sIsMember(byte[] key, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::sismember, key, value);
    }

    @Override
    public List<Boolean> sMIsMember(byte[] key, byte[]... values) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(values, "Values must not be null!");
        Assert.noNullElements(values, "Values must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::smismember, key, values);
    }

    @Override
    public Set<byte[]> sMembers(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::smembers, key);
    }

    @Override
    public Boolean sMove(byte[] srcKey, byte[] destKey, byte[] value) {
        Assert.notNull(srcKey, "Source key must not be null!");
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::smove, srcKey, destKey, value);
    }

    @Override
    public byte[] sPop(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::spop, key);
    }

    @Override
    public List<byte[]> sPop(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().from(RedisSetAsyncCommands::spop, key, count).get(ArrayList::new);
    }

    @Override
    public byte[] sRandMember(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::srandmember, key);
    }

    @Override
    public List<byte[]> sRandMember(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSetAsyncCommands::srandmember, key, count);
    }

    @Override
    public Long sRem(byte[] key, byte[]... values) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(values, "Values must not be null!");
        Assert.noNullElements(values, "Values must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::srem, key, values);
    }

    @Override
    public Set<byte[]> sUnion(byte[]... keys) {
        Assert.notNull(keys, "Keys must not be null!");
        Assert.noNullElements(keys, "Keys must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sunion, keys);
    }

    @Override
    public Long sUnionStore(byte[] destKey, byte[]... keys) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(keys, "Source keys must not be null!");
        Assert.noNullElements(keys, "Source keys must not contain null elements!");
        return connection.invoke().just(RedisSetAsyncCommands::sunionstore, destKey, keys);
    }

    @Override
    public Cursor<byte[]> sScan(byte[] key, ScanOptions options) {
        return sScan(key, 0, options);
    }

    public Cursor<byte[]> sScan(byte[] key, long cursorId, ScanOptions options) {
        Assert.notNull(key, "Key must not be null!");
        // noinspection resource
        return new KeyBoundCursor<byte[]>(key, cursorId, options) {
            @Override
            protected ScanIteration<byte[]> doScan(byte[] key, long cursorId, ScanOptions options) {
                if (connection.isQueueing() || connection.isPipelined()) {
                    throw new UnsupportedOperationException("'SSCAN' cannot be called in pipeline / transaction mode.");
                }
                io.lettuce.core.ScanCursor scanCursor = connection.getScanCursor(cursorId);
                ScanArgs scanArgs = LettuceConverters.toScanArgs(options);
                ValueScanCursor<byte[]> valueScanCursor = connection.invoke().just(RedisSetAsyncCommands::sscan, key, scanCursor, scanArgs);
                String nextCursorId = valueScanCursor.getCursor();
                List<byte[]> values = connection.failsafeReadScanValues(valueScanCursor.getValues(), null);
                return new ScanIteration<>(Long.parseLong(nextCursorId), values);
            }

            protected void doClose() {
                LettuceSetCommands.this.connection.close();
            }
        }.open();
    }

    public RedisClusterCommands<byte[], byte[]> getCommands() {
        return connection.getConnection();
    }
}
