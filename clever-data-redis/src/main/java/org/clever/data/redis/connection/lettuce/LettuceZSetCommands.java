package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisSortedSetAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.clever.data.redis.connection.RedisZSetCommands;
import org.clever.data.redis.connection.RedisZSetCommands.ZAddArgs.Flag;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.data.redis.core.*;
import org.clever.util.Assert;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:18 <br/>
 */
class LettuceZSetCommands implements RedisZSetCommands {
    private final LettuceConnection connection;

    LettuceZSetCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public Boolean zAdd(byte[] key, double score, byte[] value, ZAddArgs args) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().from(
                RedisSortedSetAsyncCommands::zadd, key, LettuceZSetCommands.toZAddArgs(args), score, value
        ).get(LettuceConverters.longToBoolean());
    }

    @Override
    public Long zAdd(byte[] key, Set<Tuple> tuples, ZAddArgs args) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(tuples, "Tuples must not be null!");
        return connection.invoke().just(
                RedisSortedSetAsyncCommands::zadd,
                key,
                LettuceZSetCommands.toZAddArgs(args),
                LettuceConverters.toObjects(tuples).toArray()
        );
    }

    @Override
    public Long zRem(byte[] key, byte[]... values) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(values, "Values must not be null!");
        Assert.noNullElements(values, "Values must not contain null elements!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zrem, key, values);
    }

    @Override
    public Double zIncrBy(byte[] key, double increment, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zincrby, key, increment, value);
    }

    @Override
    public byte[] zRandMember(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zrandmember, key);
    }

    @Override
    public List<byte[]> zRandMember(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zrandmember, key, count);
    }

    @Override
    public Tuple zRandMemberWithScore(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().from(
                RedisSortedSetAsyncCommands::zrandmemberWithScores, key
        ).get(LettuceConverters::toTuple);
    }

    @Override
    public List<Tuple> zRandMemberWithScore(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrandmemberWithScores, key, count
        ).toList(LettuceConverters::toTuple);
    }

    @Override
    public Long zRank(byte[] key, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zrank, key, value);
    }

    @Override
    public Long zRevRank(byte[] key, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zrevrank, key, value);
    }

    @Override
    public Set<byte[]> zRange(byte[] key, long start, long end) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrange, key, start, end).toSet();
    }

    @Override
    public Set<Tuple> zRangeWithScores(byte[] key, long start, long end) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrangeWithScores, key, start, end
        ).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZRANGEBYSCOREWITHSCORES must not be null!");
        Assert.notNull(limit, "Limit must not be null!");
        if (limit.isUnlimited()) {
            return connection.invoke().fromMany(
                    RedisSortedSetAsyncCommands::zrangebyscoreWithScores, key, LettuceConverters.<Number>toRange(range)
            ).toSet(LettuceConverters::toTuple);
        }
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrangebyscoreWithScores,
                key,
                LettuceConverters.<Number>toRange(range),
                LettuceConverters.toLimit(limit)
        ).toSet(LettuceConverters::toTuple);
    }

    @Override
    public Set<byte[]> zRevRange(byte[] key, long start, long end) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrange, key, start, end).toSet(Converters.identityConverter());
    }

    @Override
    public Set<Tuple> zRevRangeWithScores(byte[] key, long start, long end) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrangeWithScores, key, start, end).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<byte[]> zRevRangeByScore(byte[] key, Range range, Limit limit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZREVRANGEBYSCORE must not be null!");
        Assert.notNull(limit, "Limit must not be null!");
        if (limit.isUnlimited()) {
            return connection.invoke().fromMany(
                    RedisSortedSetAsyncCommands::zrevrangebyscore, key, LettuceConverters.<Number>toRange(range)
            ).toSet();
        }
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrevrangebyscore,
                key,
                LettuceConverters.<Number>toRange(range),
                LettuceConverters.toLimit(limit)
        ).toSet();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZREVRANGEBYSCOREWITHSCORES must not be null!");
        Assert.notNull(limit, "Limit must not be null!");
        if (limit.isUnlimited()) {
            return connection.invoke().fromMany(
                    RedisSortedSetAsyncCommands::zrevrangebyscoreWithScores,
                    key,
                    LettuceConverters.<Number>toRange(range)
            ).toSet(LettuceConverters::toTuple);
        }
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrevrangebyscoreWithScores,
                key,
                LettuceConverters.<Number>toRange(range),
                LettuceConverters.toLimit(limit)
        ).toSet(LettuceConverters::toTuple);
    }

    @Override
    public Long zCount(byte[] key, Range range) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zcount, key, LettuceConverters.<Number>toRange(range));
    }

    @Override
    public Long zLexCount(byte[] key, Range range) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range must not be null!");
        return connection.invoke().just(
                RedisSortedSetAsyncCommands::zlexcount, key, LettuceConverters.<byte[]>toRange(range, true)
        );
    }

    @Override
    public Tuple zPopMin(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().from(RedisSortedSetAsyncCommands::zpopmin, key).get(LettuceConverters::toTuple);
    }

    @Override
    public Set<Tuple> zPopMin(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zpopmin, key, count).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Tuple bZPopMin(byte[] key, long timeout, TimeUnit unit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(unit, "TimeUnit must not be null!");
        if (TimeUnit.MILLISECONDS == unit) {
            return connection.invoke(connection.getAsyncDedicatedConnection()).from(
                    RedisSortedSetAsyncCommands::bzpopmin, TimeoutUtils.toDoubleSeconds(timeout, unit), key
            ).get(it -> it.map(LettuceConverters::toTuple).getValueOrElse(null));
        }
        return connection.invoke(connection.getAsyncDedicatedConnection()).from(
                RedisSortedSetAsyncCommands::bzpopmin, unit.toSeconds(timeout), key
        ).get(it -> it.map(LettuceConverters::toTuple).getValueOrElse(null));
    }

    @Override
    public Tuple zPopMax(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().from(RedisSortedSetAsyncCommands::zpopmax, key).get(LettuceConverters::toTuple);
    }

    @Override
    public Set<Tuple> zPopMax(byte[] key, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zpopmax, key, count
        ).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Tuple bZPopMax(byte[] key, long timeout, TimeUnit unit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(unit, "TimeUnit must not be null!");
        if (TimeUnit.MILLISECONDS == unit) {
            return connection.invoke(connection.getAsyncDedicatedConnection()).from(
                    RedisSortedSetAsyncCommands::bzpopmax, TimeoutUtils.toDoubleSeconds(timeout, unit), key
            ).get(it -> it.map(LettuceConverters::toTuple).getValueOrElse(null));
        }
        return connection.invoke(connection.getAsyncDedicatedConnection()).from(
                RedisSortedSetAsyncCommands::bzpopmax, unit.toSeconds(timeout), key
        ).get(it -> it.map(LettuceConverters::toTuple).getValueOrElse(null));
    }

    @Override
    public Long zCard(byte[] key) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zcard, key);
    }

    @Override
    public Double zScore(byte[] key, byte[] value) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zscore, key, value);
    }

    @Override
    public List<Double> zMScore(byte[] key, byte[][] values) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(values, "Value must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zmscore, key, values);
    }

    @Override
    public Long zRemRange(byte[] key, long start, long end) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zremrangebyrank, key, start, end);
    }

    @Override
    public Long zRemRangeByLex(byte[] key, Range range) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range must not be null for ZREMRANGEBYLEX!");
        return connection.invoke().just(
                RedisSortedSetAsyncCommands::zremrangebylex,
                key,
                LettuceConverters.<byte[]>toRange(range, true)
        );
    }

    @Override
    public Long zRemRangeByScore(byte[] key, Range range) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZREMRANGEBYSCORE must not be null!");
        return connection.invoke().just(
                RedisSortedSetAsyncCommands::zremrangebyscore,
                key,
                LettuceConverters.<Number>toRange(range)
        );
    }

    @Override
    public Set<byte[]> zDiff(byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zdiff, sets).toSet();
    }

    @Override
    public Set<Tuple> zDiffWithScores(byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zdiffWithScores, sets
        ).toSet(LettuceConverters::toTuple);
    }

    @Override
    public Long zDiffStore(byte[] destKey, byte[]... sets) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(sets, "Source sets must not be null!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zdiffstore, destKey, sets);
    }

    @Override
    public Set<byte[]> zInter(byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zinter, sets).toSet();
    }

    @Override
    public Set<Tuple> zInterWithScores(byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zinterWithScores, sets
        ).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<Tuple> zInterWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        Assert.noNullElements(sets, "Source sets must not contain null elements!");
        Assert.isTrue(
                weights.size() == sets.length,
                () -> String.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length)
        );

        ZAggregateArgs zAggregateArgs = zAggregateArgs(aggregate, weights);
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zinterWithScores, zAggregateArgs, sets
        ).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Long zInterStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(sets, "Source sets must not be null!");
        Assert.noNullElements(sets, "Source sets must not contain null elements!");
        Assert.isTrue(
                weights.size() == sets.length,
                () -> String.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length)
        );
        ZStoreArgs storeArgs = zStoreArgs(aggregate, weights);
        return connection.invoke().just(RedisSortedSetAsyncCommands::zinterstore, destKey, storeArgs, sets);
    }

    @Override
    public Long zInterStore(byte[] destKey, byte[]... sets) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(sets, "Source sets must not be null!");
        Assert.noNullElements(sets, "Source sets must not contain null elements!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zinterstore, destKey, sets);
    }

    @Override
    public Set<byte[]> zUnion(byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zunion, sets).toSet();
    }

    @Override
    public Set<Tuple> zUnionWithScores(byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zunionWithScores, sets
        ).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<Tuple> zUnionWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {
        Assert.notNull(sets, "Sets must not be null!");
        Assert.noNullElements(sets, "Source sets must not contain null elements!");
        Assert.isTrue(
                weights.size() == sets.length,
                () -> String.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length)
        );
        ZAggregateArgs zAggregateArgs = zAggregateArgs(aggregate, weights);
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zunionWithScores, zAggregateArgs, sets).toSet(LettuceConverters::toTuple);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Long zUnionStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(sets, "Source sets must not be null!");
        Assert.noNullElements(sets, "Source sets must not contain null elements!");
        Assert.isTrue(
                weights.size() == sets.length,
                () -> String.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length)
        );
        ZStoreArgs storeArgs = zStoreArgs(aggregate, weights);
        return connection.invoke().just(RedisSortedSetAsyncCommands::zunionstore, destKey, storeArgs, sets);
    }

    @Override
    public Long zUnionStore(byte[] destKey, byte[]... sets) {
        Assert.notNull(destKey, "Destination key must not be null!");
        Assert.notNull(sets, "Source sets must not be null!");
        Assert.noNullElements(sets, "Source sets must not contain null elements!");
        return connection.invoke().just(RedisSortedSetAsyncCommands::zunionstore, destKey, sets);
    }

    @Override
    public Cursor<Tuple> zScan(byte[] key, ScanOptions options) {
        return zScan(key, 0L, options);
    }

    public Cursor<Tuple> zScan(byte[] key, long cursorId, ScanOptions options) {
        Assert.notNull(key, "Key must not be null!");
        // noinspection resource
        return new KeyBoundCursor<Tuple>(key, cursorId, options) {
            @Override
            protected ScanIteration<Tuple> doScan(byte[] key, long cursorId, ScanOptions options) {
                if (connection.isQueueing() || connection.isPipelined()) {
                    throw new UnsupportedOperationException("'ZSCAN' cannot be called in pipeline / transaction mode.");
                }
                io.lettuce.core.ScanCursor scanCursor = connection.getScanCursor(cursorId);
                ScanArgs scanArgs = LettuceConverters.toScanArgs(options);
                ScoredValueScanCursor<byte[]> scoredValueScanCursor = connection.invoke().just(
                        RedisSortedSetAsyncCommands::zscan, key, scanCursor, scanArgs
                );
                String nextCursorId = scoredValueScanCursor.getCursor();
                List<ScoredValue<byte[]>> result = scoredValueScanCursor.getValues();
                List<Tuple> values = connection.failsafeReadScanValues(result, LettuceConverters.scoredValuesToTupleList());
                return new ScanIteration<>(Long.parseLong(nextCursorId), values);
            }

            @Override
            protected void doClose() {
                LettuceZSetCommands.this.connection.close();
            }
        }.open();
    }

    @Override
    public Set<byte[]> zRangeByScore(byte[] key, String min, String max) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebyscore, key, min, max).toSet();
    }

    @Override
    public Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {
        Assert.notNull(key, "Key must not be null!");
        return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebyscore, key, min, max, offset, count).toSet();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<byte[]> zRangeByScore(byte[] key, Range range, Limit limit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZRANGEBYSCORE must not be null!");
        Assert.notNull(limit, "Limit must not be null!");
        if (limit.isUnlimited()) {
            return connection.invoke().fromMany(
                    RedisSortedSetAsyncCommands::zrangebyscore,
                    key,
                    LettuceConverters.<Number>toRange(range)
            ).toSet();
        }
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrangebyscore,
                key,
                LettuceConverters.<Number>toRange(range),
                LettuceConverters.toLimit(limit)
        ).toSet();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<byte[]> zRangeByLex(byte[] key, Range range, Limit limit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZRANGEBYLEX must not be null!");
        Assert.notNull(limit, "Limit must not be null!");
        if (limit.isUnlimited()) {
            return connection.invoke().fromMany(
                    RedisSortedSetAsyncCommands::zrangebylex,
                    key,
                    LettuceConverters.<byte[]>toRange(range, true)
            ).toSet();
        }
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrangebylex,
                key,
                LettuceConverters.<byte[]>toRange(range, true),
                LettuceConverters.toLimit(limit)
        ).toSet();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Set<byte[]> zRevRangeByLex(byte[] key, Range range, Limit limit) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(range, "Range for ZREVRANGEBYLEX must not be null!");
        Assert.notNull(limit, "Limit must not be null!");
        if (limit.isUnlimited()) {
            return connection.invoke().fromMany(
                    RedisSortedSetAsyncCommands::zrevrangebylex,
                    key,
                    LettuceConverters.<byte[]>toRange(range, true)
            ).toSet();
        }
        return connection.invoke().fromMany(
                RedisSortedSetAsyncCommands::zrevrangebylex,
                key,
                LettuceConverters.<byte[]>toRange(range, true),
                LettuceConverters.toLimit(limit)
        ).toSet();
    }

    public RedisClusterCommands<byte[], byte[]> getConnection() {
        return connection.getConnection();
    }

    @SuppressWarnings("DuplicatedCode")
    private static ZStoreArgs zStoreArgs(Aggregate aggregate, Weights weights) {
        ZStoreArgs args = new ZStoreArgs();
        if (aggregate != null) {
            switch (aggregate) {
                case MIN:
                    args.min();
                    break;
                case MAX:
                    args.max();
                    break;
                default:
                    args.sum();
                    break;
            }
        }
        args.weights(weights.toArray());
        return args;
    }

    @SuppressWarnings("DuplicatedCode")
    private static ZAggregateArgs zAggregateArgs(Aggregate aggregate, Weights weights) {
        ZAggregateArgs args = new ZAggregateArgs();
        if (aggregate != null) {
            switch (aggregate) {
                case MIN:
                    args.min();
                    break;
                case MAX:
                    args.max();
                    break;
                default:
                    args.sum();
                    break;
            }
        }
        args.weights(weights.toArray());
        return args;
    }

    /**
     * 转换 {@link ZAddArgs} 到 {@link io.lettuce.core.ZAddArgs}
     *
     * @param source 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    private static io.lettuce.core.ZAddArgs toZAddArgs(ZAddArgs source) {
        io.lettuce.core.ZAddArgs target = new io.lettuce.core.ZAddArgs();
        if (!source.isEmpty()) {
            return target;
        }
        if (source.contains(Flag.XX)) {
            target.xx();
        }
        if (source.contains(Flag.NX)) {
            target.nx();
        }
        if (source.contains(Flag.GT)) {
            target.gt();
        }
        if (source.contains(Flag.LT)) {
            target.lt();
        }
        if (source.contains(Flag.CH)) {
            target.ch();
        }
        return target;
    }
}
