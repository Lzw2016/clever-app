package org.clever.data.redis.connection;

import org.clever.data.geo.*;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoConsumers;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoGroups;
import org.clever.data.redis.connection.stream.StreamInfo.XInfoStream;
import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanOptions;
import org.clever.data.redis.core.types.Expiration;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.data.redis.domain.geo.GeoReference;
import org.clever.data.redis.domain.geo.GeoShape;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * {@link DefaultedRedisConnection} 为可通过 {@link RedisConnection} 访问的 {@code RedisCommand} 接口提供方法委托。
 * 这使我们能够在移动实际实现的同时保持向后兼容性。展望未来，{@link RedisCommands} 扩展可能会从 {@link RedisConnection} 中删除。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:18 <br/>
 */
public interface DefaultedRedisConnection extends RedisConnection {
    // KEY COMMANDS

    @Deprecated
    @Override
    default Boolean copy(byte[] sourceKey, byte[] targetKey, boolean replace) {
        return keyCommands().copy(sourceKey, targetKey, replace);
    }

    @Deprecated
    @Override
    default Boolean exists(byte[] key) {
        return keyCommands().exists(key);
    }

    @Deprecated
    @Override
    default Long exists(byte[]... keys) {
        return keyCommands().exists(keys);
    }

    @Deprecated
    @Override
    default Long del(byte[]... keys) {
        return keyCommands().del(keys);
    }

    @Deprecated
    @Override
    default Long unlink(byte[]... keys) {
        return keyCommands().unlink(keys);
    }

    @Deprecated
    @Override
    default DataType type(byte[] pattern) {
        return keyCommands().type(pattern);
    }

    @Deprecated
    @Override
    default Long touch(byte[]... keys) {
        return keyCommands().touch(keys);
    }

    @Deprecated
    @Override
    default Set<byte[]> keys(byte[] pattern) {
        return keyCommands().keys(pattern);
    }

    @Deprecated
    @Override
    default Cursor<byte[]> scan(ScanOptions options) {
        return keyCommands().scan(options);
    }

    @Deprecated
    @Override
    default byte[] randomKey() {
        return keyCommands().randomKey();
    }

    @Deprecated
    @Override
    default void rename(byte[] oldKey, byte[] newKey) {
        keyCommands().rename(oldKey, newKey);
    }

    @Deprecated
    @Override
    default Boolean renameNX(byte[] sourceKey, byte[] targetKey) {
        return keyCommands().renameNX(sourceKey, targetKey);
    }

    @Deprecated
    @Override
    default Boolean expire(byte[] key, long seconds) {
        return keyCommands().expire(key, seconds);
    }

    @Deprecated
    @Override
    default Boolean persist(byte[] key) {
        return keyCommands().persist(key);
    }

    @Deprecated
    @Override
    default Boolean move(byte[] key, int dbIndex) {
        return keyCommands().move(key, dbIndex);
    }

    @Deprecated
    @Override
    default void restore(byte[] key, long ttlInMillis, byte[] serializedValue, boolean replace) {
        keyCommands().restore(key, ttlInMillis, serializedValue, replace);
    }

    @Deprecated
    @Override
    default Long pTtl(byte[] key) {
        return keyCommands().pTtl(key);
    }

    @Deprecated
    @Override
    default Long pTtl(byte[] key, TimeUnit timeUnit) {
        return keyCommands().pTtl(key, timeUnit);
    }

    @Deprecated
    @Override
    default Boolean pExpire(byte[] key, long millis) {
        return keyCommands().pExpire(key, millis);
    }

    @Deprecated
    @Override
    default Boolean pExpireAt(byte[] key, long unixTimeInMillis) {
        return keyCommands().pExpireAt(key, unixTimeInMillis);
    }

    @Deprecated
    @Override
    default Boolean expireAt(byte[] key, long unixTime) {
        return keyCommands().expireAt(key, unixTime);
    }

    @Deprecated
    @Override
    default Long ttl(byte[] key) {
        return keyCommands().ttl(key);
    }

    @Deprecated
    @Override
    default Long ttl(byte[] key, TimeUnit timeUnit) {
        return keyCommands().ttl(key, timeUnit);
    }

    @Deprecated
    @Override
    default byte[] dump(byte[] key) {
        return keyCommands().dump(key);
    }

    @Deprecated
    @Override
    default List<byte[]> sort(byte[] key, SortParameters params) {
        return keyCommands().sort(key, params);
    }

    @Deprecated
    @Override
    default Long sort(byte[] key, SortParameters params, byte[] sortKey) {
        return keyCommands().sort(key, params, sortKey);
    }

    @Deprecated
    @Override
    default ValueEncoding encodingOf(byte[] key) {
        return keyCommands().encodingOf(key);
    }

    @Deprecated
    @Override
    default Duration idletime(byte[] key) {
        return keyCommands().idletime(key);
    }

    @Deprecated
    @Override
    default Long refcount(byte[] key) {
        return keyCommands().refcount(key);
    }

    // STRING COMMANDS

    @Deprecated
    @Override
    default byte[] get(byte[] key) {
        return stringCommands().get(key);
    }

    @Deprecated
    @Override
    default byte[] getEx(byte[] key, Expiration expiration) {
        return stringCommands().getEx(key, expiration);
    }

    @Deprecated
    @Override
    default byte[] getDel(byte[] key) {
        return stringCommands().getDel(key);
    }

    @Deprecated
    @Override
    default byte[] getSet(byte[] key, byte[] value) {
        return stringCommands().getSet(key, value);
    }

    @Deprecated
    @Override
    default List<byte[]> mGet(byte[]... keys) {
        return stringCommands().mGet(keys);
    }

    @Deprecated
    @Override
    default Boolean set(byte[] key, byte[] value) {
        return stringCommands().set(key, value);
    }

    @Deprecated
    @Override
    default Boolean set(byte[] key, byte[] value, Expiration expiration, SetOption option) {
        return stringCommands().set(key, value, expiration, option);
    }

    @Deprecated
    @Override
    default Boolean setNX(byte[] key, byte[] value) {
        return stringCommands().setNX(key, value);
    }

    @Deprecated
    @Override
    default Boolean setEx(byte[] key, long seconds, byte[] value) {
        return stringCommands().setEx(key, seconds, value);
    }

    @Deprecated
    @Override
    default Boolean pSetEx(byte[] key, long milliseconds, byte[] value) {
        return stringCommands().pSetEx(key, milliseconds, value);
    }

    @Deprecated
    @Override
    default Boolean mSet(Map<byte[], byte[]> tuple) {
        return stringCommands().mSet(tuple);
    }

    @Deprecated
    @Override
    default Boolean mSetNX(Map<byte[], byte[]> tuple) {
        return stringCommands().mSetNX(tuple);
    }

    @Deprecated
    @Override
    default Long incr(byte[] key) {
        return stringCommands().incr(key);
    }

    @Deprecated
    @Override
    default Double incrBy(byte[] key, double value) {
        return stringCommands().incrBy(key, value);
    }

    @Deprecated
    @Override
    default Long incrBy(byte[] key, long value) {
        return stringCommands().incrBy(key, value);
    }

    @Deprecated
    @Override
    default Long decr(byte[] key) {
        return stringCommands().decr(key);
    }

    @Deprecated
    @Override
    default Long decrBy(byte[] key, long value) {
        return stringCommands().decrBy(key, value);
    }

    @Deprecated
    @Override
    default Long append(byte[] key, byte[] value) {
        return stringCommands().append(key, value);
    }

    @Deprecated
    @Override
    default byte[] getRange(byte[] key, long start, long end) {
        return stringCommands().getRange(key, start, end);
    }

    @Deprecated
    @Override
    default void setRange(byte[] key, byte[] value, long offset) {
        stringCommands().setRange(key, value, offset);
    }

    @Deprecated
    @Override
    default Boolean getBit(byte[] key, long offset) {
        return stringCommands().getBit(key, offset);
    }

    @Deprecated
    @Override
    default Boolean setBit(byte[] key, long offset, boolean value) {
        return stringCommands().setBit(key, offset, value);
    }

    @Deprecated
    @Override
    default Long bitCount(byte[] key) {
        return stringCommands().bitCount(key);
    }

    @Deprecated
    @Override
    default Long bitCount(byte[] key, long start, long end) {
        return stringCommands().bitCount(key, start, end);
    }

    @Deprecated
    @Override
    default List<Long> bitField(byte[] key, BitFieldSubCommands subCommands) {
        return stringCommands().bitField(key, subCommands);
    }

    @Deprecated
    @Override
    default Long bitOp(BitOperation op, byte[] destination, byte[]... keys) {
        return stringCommands().bitOp(op, destination, keys);
    }

    @Deprecated
    @Override
    default Long bitPos(byte[] key, boolean bit, org.clever.data.domain.Range<Long> range) {
        return stringCommands().bitPos(key, bit, range);
    }

    @Deprecated
    @Override
    default Long strLen(byte[] key) {
        return stringCommands().strLen(key);
    }

    // STREAM COMMANDS

    @Deprecated
    @Override
    default Long xAck(byte[] key, String group, RecordId... messageIds) {
        return streamCommands().xAck(key, group, messageIds);
    }

    @Deprecated
    @Override
    default RecordId xAdd(MapRecord<byte[], byte[], byte[]> record, XAddOptions options) {
        return streamCommands().xAdd(record, options);
    }

    @Deprecated
    @Override
    default List<RecordId> xClaimJustId(byte[] key, String group, String newOwner, XClaimOptions options) {
        return streamCommands().xClaimJustId(key, group, newOwner, options);
    }

    @Deprecated
    @Override
    default List<ByteRecord> xClaim(byte[] key, String group, String newOwner, XClaimOptions options) {
        return streamCommands().xClaim(key, group, newOwner, options);
    }

    @Deprecated
    @Override
    default Long xDel(byte[] key, RecordId... recordIds) {
        return streamCommands().xDel(key, recordIds);
    }

    @Deprecated
    @Override
    default String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset) {
        return streamCommands().xGroupCreate(key, groupName, readOffset);
    }

    @Deprecated
    @Override
    default String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset, boolean mkStream) {
        return streamCommands().xGroupCreate(key, groupName, readOffset, mkStream);
    }

    @Deprecated
    @Override
    default Boolean xGroupDelConsumer(byte[] key, Consumer consumer) {
        return streamCommands().xGroupDelConsumer(key, consumer);
    }

    @Deprecated
    @Override
    default Boolean xGroupDestroy(byte[] key, String groupName) {
        return streamCommands().xGroupDestroy(key, groupName);
    }

    @Deprecated
    @Override
    default XInfoStream xInfo(byte[] key) {
        return streamCommands().xInfo(key);
    }

    @Deprecated
    @Override
    default XInfoGroups xInfoGroups(byte[] key) {
        return streamCommands().xInfoGroups(key);
    }

    @Deprecated
    @Override
    default XInfoConsumers xInfoConsumers(byte[] key, String groupName) {
        return streamCommands().xInfoConsumers(key, groupName);
    }

    @Deprecated
    @Override
    default Long xLen(byte[] key) {
        return streamCommands().xLen(key);
    }

    @Deprecated
    @Override
    default PendingMessagesSummary xPending(byte[] key, String groupName) {
        return streamCommands().xPending(key, groupName);
    }

    @Deprecated
    @Override
    default PendingMessages xPending(byte[] key, String groupName, XPendingOptions options) {
        return streamCommands().xPending(key, groupName, options);
    }

    @Deprecated
    @Override
    default List<ByteRecord> xRange(byte[] key, org.clever.data.domain.Range<String> range) {
        return streamCommands().xRange(key, range);
    }

    @Deprecated
    @Override
    default List<ByteRecord> xRange(byte[] key, org.clever.data.domain.Range<String> range, Limit limit) {
        return streamCommands().xRange(key, range, limit);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Override
    default List<ByteRecord> xRead(StreamOffset<byte[]>... streams) {
        return streamCommands().xRead(streams);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Override
    default List<ByteRecord> xRead(StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
        return streamCommands().xRead(readOptions, streams);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Override
    default List<ByteRecord> xReadGroup(Consumer consumer, StreamOffset<byte[]>... streams) {
        return streamCommands().xReadGroup(consumer, streams);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Override
    default List<ByteRecord> xReadGroup(Consumer consumer, StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
        return streamCommands().xReadGroup(consumer, readOptions, streams);
    }

    @Deprecated
    @Override
    default List<ByteRecord> xRevRange(byte[] key, org.clever.data.domain.Range<String> range) {
        return streamCommands().xRevRange(key, range);
    }

    @Deprecated
    @Override
    default List<ByteRecord> xRevRange(byte[] key, org.clever.data.domain.Range<String> range, Limit limit) {
        return streamCommands().xRevRange(key, range, limit);
    }

    @Deprecated
    @Override
    default Long xTrim(byte[] key, long count) {
        return xTrim(key, count, false);
    }

    @Deprecated
    @Override
    default Long xTrim(byte[] key, long count, boolean approximateTrimming) {
        return streamCommands().xTrim(key, count, approximateTrimming);
    }

    // LIST COMMANDS

    @Deprecated
    @Override
    default Long rPush(byte[] key, byte[]... values) {
        return listCommands().rPush(key, values);
    }

    @Deprecated
    @Override
    default List<Long> lPos(byte[] key, byte[] element, Integer rank, Integer count) {
        return listCommands().lPos(key, element, rank, count);
    }

    @Deprecated
    @Override
    default Long lPush(byte[] key, byte[]... values) {
        return listCommands().lPush(key, values);
    }

    @Deprecated
    @Override
    default Long rPushX(byte[] key, byte[] value) {
        return listCommands().rPushX(key, value);
    }

    @Deprecated
    @Override
    default Long lPushX(byte[] key, byte[] value) {
        return listCommands().lPushX(key, value);
    }

    @Deprecated
    @Override
    default Long lLen(byte[] key) {
        return listCommands().lLen(key);
    }

    @Deprecated
    @Override
    default List<byte[]> lRange(byte[] key, long start, long end) {
        return listCommands().lRange(key, start, end);
    }

    @Deprecated
    @Override
    default void lTrim(byte[] key, long start, long end) {
        listCommands().lTrim(key, start, end);
    }

    @Deprecated
    @Override
    default byte[] lIndex(byte[] key, long index) {
        return listCommands().lIndex(key, index);
    }

    @Deprecated
    @Override
    default Long lInsert(byte[] key, Position where, byte[] pivot, byte[] value) {
        return listCommands().lInsert(key, where, pivot, value);
    }

    @Deprecated
    @Override
    default byte[] lMove(byte[] sourceKey, byte[] destinationKey, Direction from, Direction to) {
        return listCommands().lMove(sourceKey, destinationKey, from, to);
    }

    @Deprecated
    @Override
    default byte[] bLMove(byte[] sourceKey, byte[] destinationKey, Direction from, Direction to, double timeout) {
        return listCommands().bLMove(sourceKey, destinationKey, from, to, timeout);
    }

    @Deprecated
    @Override
    default void lSet(byte[] key, long index, byte[] value) {
        listCommands().lSet(key, index, value);
    }

    @Deprecated
    @Override
    default Long lRem(byte[] key, long count, byte[] value) {
        return listCommands().lRem(key, count, value);
    }

    @Deprecated
    @Override
    default byte[] lPop(byte[] key) {
        return listCommands().lPop(key);
    }

    @Deprecated
    @Override
    default List<byte[]> lPop(byte[] key, long count) {
        return listCommands().lPop(key, count);
    }

    @Deprecated
    @Override
    default byte[] rPop(byte[] key) {
        return listCommands().rPop(key);
    }

    @Deprecated
    @Override
    default List<byte[]> rPop(byte[] key, long count) {
        return listCommands().rPop(key, count);
    }

    @Deprecated
    @Override
    default List<byte[]> bLPop(int timeout, byte[]... keys) {
        return listCommands().bLPop(timeout, keys);
    }

    @Deprecated
    @Override
    default List<byte[]> bRPop(int timeout, byte[]... keys) {
        return listCommands().bRPop(timeout, keys);
    }

    @Deprecated
    @Override
    default byte[] rPopLPush(byte[] srcKey, byte[] dstKey) {
        return listCommands().rPopLPush(srcKey, dstKey);
    }

    @Deprecated
    @Override
    default byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey) {
        return listCommands().bRPopLPush(timeout, srcKey, dstKey);
    }

    // SET COMMANDS

    @Deprecated
    @Override
    default Long sAdd(byte[] key, byte[]... values) {
        return setCommands().sAdd(key, values);
    }

    @Deprecated
    @Override
    default Long sCard(byte[] key) {
        return setCommands().sCard(key);
    }

    @Deprecated
    @Override
    default Set<byte[]> sDiff(byte[]... keys) {
        return setCommands().sDiff(keys);
    }

    @Deprecated
    @Override
    default Long sDiffStore(byte[] destKey, byte[]... keys) {
        return setCommands().sDiffStore(destKey, keys);
    }

    @Deprecated
    @Override
    default Set<byte[]> sInter(byte[]... keys) {
        return setCommands().sInter(keys);
    }

    @Deprecated
    @Override
    default Long sInterStore(byte[] destKey, byte[]... keys) {
        return setCommands().sInterStore(destKey, keys);
    }

    @Deprecated
    @Override
    default Boolean sIsMember(byte[] key, byte[] value) {
        return setCommands().sIsMember(key, value);
    }

    @Deprecated
    @Override
    default List<Boolean> sMIsMember(byte[] key, byte[]... value) {
        return setCommands().sMIsMember(key, value);
    }

    @Deprecated
    @Override
    default Set<byte[]> sMembers(byte[] key) {
        return setCommands().sMembers(key);
    }

    @Deprecated
    @Override
    default Boolean sMove(byte[] srcKey, byte[] destKey, byte[] value) {
        return setCommands().sMove(srcKey, destKey, value);
    }

    @Deprecated
    @Override
    default byte[] sPop(byte[] key) {
        return setCommands().sPop(key);
    }

    @Deprecated
    @Override
    default List<byte[]> sPop(byte[] key, long count) {
        return setCommands().sPop(key, count);
    }

    @Deprecated
    @Override
    default byte[] sRandMember(byte[] key) {
        return setCommands().sRandMember(key);
    }

    @Deprecated
    @Override
    default List<byte[]> sRandMember(byte[] key, long count) {
        return setCommands().sRandMember(key, count);
    }

    @Deprecated
    @Override
    default Long sRem(byte[] key, byte[]... values) {
        return setCommands().sRem(key, values);
    }

    @Deprecated
    @Override
    default Set<byte[]> sUnion(byte[]... keys) {
        return setCommands().sUnion(keys);
    }

    @Deprecated
    @Override
    default Long sUnionStore(byte[] destKey, byte[]... keys) {
        return setCommands().sUnionStore(destKey, keys);
    }

    @Deprecated
    @Override
    default Cursor<byte[]> sScan(byte[] key, ScanOptions options) {
        return setCommands().sScan(key, options);
    }

    // ZSET COMMANDS

    @Deprecated
    @Override
    default Boolean zAdd(byte[] key, double score, byte[] value, ZAddArgs args) {
        return zSetCommands().zAdd(key, score, value, args);
    }

    @Deprecated
    @Override
    default Long zAdd(byte[] key, Set<Tuple> tuples, ZAddArgs args) {
        return zSetCommands().zAdd(key, tuples, args);
    }

    @Deprecated
    @Override
    default Long zCard(byte[] key) {
        return zSetCommands().zCard(key);
    }

    @Deprecated
    @Override
    default Long zCount(byte[] key, double min, double max) {
        return zSetCommands().zCount(key, min, max);
    }

    @Deprecated
    @Override
    default Long zLexCount(byte[] key, Range range) {
        return zSetCommands().zLexCount(key, range);
    }

    @Deprecated
    @Override
    default Tuple zPopMin(byte[] key) {
        return zSetCommands().zPopMin(key);
    }

    @Deprecated
    @Override
    default Set<Tuple> zPopMin(byte[] key, long count) {
        return zSetCommands().zPopMin(key, count);
    }

    @Deprecated
    @Override
    default Tuple bZPopMin(byte[] key, long timeout, TimeUnit unit) {
        return zSetCommands().bZPopMin(key, timeout, unit);
    }

    @Deprecated
    @Override
    default Tuple zPopMax(byte[] key) {
        return zSetCommands().zPopMax(key);
    }

    @Deprecated
    @Override
    default Set<Tuple> zPopMax(byte[] key, long count) {
        return zSetCommands().zPopMax(key, count);
    }

    @Deprecated
    @Override
    default Tuple bZPopMax(byte[] key, long timeout, TimeUnit unit) {
        return zSetCommands().bZPopMax(key, timeout, unit);
    }

    @Deprecated
    @Override
    default Long zCount(byte[] key, Range range) {
        return zSetCommands().zCount(key, range);
    }

    @Deprecated
    @Override
    default Set<byte[]> zDiff(byte[]... sets) {
        return zSetCommands().zDiff(sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zDiffWithScores(byte[]... sets) {
        return zSetCommands().zDiffWithScores(sets);
    }

    @Deprecated
    @Override
    default Long zDiffStore(byte[] destKey, byte[]... sets) {
        return zSetCommands().zDiffStore(destKey, sets);
    }

    @Deprecated
    @Override
    default Double zIncrBy(byte[] key, double increment, byte[] value) {
        return zSetCommands().zIncrBy(key, increment, value);
    }

    @Deprecated
    @Override
    default Set<byte[]> zInter(byte[]... sets) {
        return zSetCommands().zInter(sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zInterWithScores(Aggregate aggregate, int[] weights, byte[]... sets) {
        return zSetCommands().zInterWithScores(aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zInterWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {
        return zSetCommands().zInterWithScores(aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zInterWithScores(byte[]... sets) {
        return zSetCommands().zInterWithScores(sets);
    }

    @Deprecated
    @Override
    default Long zInterStore(byte[] destKey, Aggregate aggregate, int[] weights, byte[]... sets) {
        return zSetCommands().zInterStore(destKey, aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Long zInterStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {
        return zSetCommands().zInterStore(destKey, aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Long zInterStore(byte[] destKey, byte[]... sets) {
        return zSetCommands().zInterStore(destKey, sets);
    }

    @Deprecated
    @Override
    default byte[] zRandMember(byte[] key) {
        return zSetCommands().zRandMember(key);
    }

    @Deprecated
    @Override
    default List<byte[]> zRandMember(byte[] key, long count) {
        return zSetCommands().zRandMember(key, count);
    }

    @Deprecated
    @Override
    default Tuple zRandMemberWithScore(byte[] key) {
        return zSetCommands().zRandMemberWithScore(key);
    }

    @Deprecated
    @Override
    default List<Tuple> zRandMemberWithScore(byte[] key, long count) {
        return zSetCommands().zRandMemberWithScore(key, count);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRange(byte[] key, long start, long end) {
        return zSetCommands().zRange(key, start, end);
    }

    @Deprecated
    @Override
    default Set<Tuple> zRangeWithScores(byte[] key, long start, long end) {
        return zSetCommands().zRangeWithScores(key, start, end);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRangeByLex(byte[] key, Range range, Limit limit) {
        return zSetCommands().zRangeByLex(key, range, limit);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRevRangeByLex(byte[] key, Range range, Limit limit) {
        return zSetCommands().zRevRangeByLex(key, range, limit);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRangeByScore(byte[] key, Range range, Limit limit) {
        return zSetCommands().zRangeByScore(key, range, limit);
    }

    @Deprecated
    @Override
    default Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        return zSetCommands().zRangeByScoreWithScores(key, range, limit);
    }

    @Deprecated
    @Override
    default Set<Tuple> zRevRangeWithScores(byte[] key, long start, long end) {
        return zSetCommands().zRevRangeWithScores(key, start, end);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRevRangeByScore(byte[] key, Range range, Limit limit) {
        return zSetCommands().zRevRangeByScore(key, range, limit);
    }

    @Deprecated
    @Override
    default Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        return zSetCommands().zRevRangeByScoreWithScores(key, range, limit);
    }

    @Deprecated
    @Override
    default Long zRank(byte[] key, byte[] value) {
        return zSetCommands().zRank(key, value);
    }

    @Deprecated
    @Override
    default Long zRem(byte[] key, byte[]... values) {
        return zSetCommands().zRem(key, values);
    }

    @Deprecated
    @Override
    default Long zRemRange(byte[] key, long start, long end) {
        return zSetCommands().zRemRange(key, start, end);
    }

    @Deprecated
    @Override
    default Long zRemRangeByLex(byte[] key, Range range) {
        return zSetCommands().zRemRangeByLex(key, range);
    }

    @Deprecated
    @Override
    default Long zRemRangeByScore(byte[] key, Range range) {
        return zSetCommands().zRemRangeByScore(key, range);
    }

    @Deprecated
    @Override
    default Long zRemRangeByScore(byte[] key, double min, double max) {
        return zSetCommands().zRemRangeByScore(key, min, max);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRevRange(byte[] key, long start, long end) {
        return zSetCommands().zRevRange(key, start, end);
    }

    @Deprecated
    @Override
    default Long zRevRank(byte[] key, byte[] value) {
        return zSetCommands().zRevRank(key, value);
    }

    @Deprecated
    @Override
    default Double zScore(byte[] key, byte[] value) {
        return zSetCommands().zScore(key, value);
    }

    @Deprecated
    @Override
    default List<Double> zMScore(byte[] key, byte[]... values) {
        return zSetCommands().zMScore(key, values);
    }

    @Deprecated
    @Override
    default Set<byte[]> zUnion(byte[]... sets) {
        return zSetCommands().zUnion(sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zUnionWithScores(Aggregate aggregate, int[] weights, byte[]... sets) {
        return zSetCommands().zUnionWithScores(aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zUnionWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {
        return zSetCommands().zUnionWithScores(aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Set<Tuple> zUnionWithScores(byte[]... sets) {
        return zSetCommands().zUnionWithScores(sets);
    }

    @Deprecated
    @Override
    default Long zUnionStore(byte[] destKey, Aggregate aggregate, int[] weights, byte[]... sets) {
        return zSetCommands().zUnionStore(destKey, aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Long zUnionStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {
        return zSetCommands().zUnionStore(destKey, aggregate, weights, sets);
    }

    @Deprecated
    @Override
    default Long zUnionStore(byte[] destKey, byte[]... sets) {
        return zSetCommands().zUnionStore(destKey, sets);
    }

    @Deprecated
    @Override
    default Cursor<Tuple> zScan(byte[] key, ScanOptions options) {
        return zSetCommands().zScan(key, options);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRangeByScore(byte[] key, String min, String max) {
        return zSetCommands().zRangeByScore(key, min, max);
    }

    @Deprecated
    @Override
    default Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {
        return zSetCommands().zRangeByScore(key, min, max, offset, count);
    }

    // HASH COMMANDS

    @Deprecated
    @Override
    default Boolean hSet(byte[] key, byte[] field, byte[] value) {
        return hashCommands().hSet(key, field, value);
    }

    @Deprecated
    @Override
    default Boolean hSetNX(byte[] key, byte[] field, byte[] value) {
        return hashCommands().hSetNX(key, field, value);
    }

    @Deprecated
    @Override
    default Long hDel(byte[] key, byte[]... fields) {
        return hashCommands().hDel(key, fields);
    }

    @Deprecated
    @Override
    default Boolean hExists(byte[] key, byte[] field) {
        return hashCommands().hExists(key, field);
    }

    @Deprecated
    @Override
    default byte[] hGet(byte[] key, byte[] field) {
        return hashCommands().hGet(key, field);
    }

    @Deprecated
    @Override
    default Map<byte[], byte[]> hGetAll(byte[] key) {
        return hashCommands().hGetAll(key);
    }

    @Deprecated
    @Override
    default Double hIncrBy(byte[] key, byte[] field, double delta) {
        return hashCommands().hIncrBy(key, field, delta);
    }

    @Deprecated
    @Override
    default Long hIncrBy(byte[] key, byte[] field, long delta) {
        return hashCommands().hIncrBy(key, field, delta);
    }

    @Deprecated
    @Override
    default byte[] hRandField(byte[] key) {
        return hashCommands().hRandField(key);
    }

    @Deprecated
    @Override
    default Entry<byte[], byte[]> hRandFieldWithValues(byte[] key) {
        return hashCommands().hRandFieldWithValues(key);
    }

    @Deprecated
    @Override
    default List<byte[]> hRandField(byte[] key, long count) {
        return hashCommands().hRandField(key, count);
    }

    @Deprecated
    @Override
    default List<Entry<byte[], byte[]>> hRandFieldWithValues(byte[] key, long count) {
        return hashCommands().hRandFieldWithValues(key, count);
    }

    @Deprecated
    @Override
    default Set<byte[]> hKeys(byte[] key) {
        return hashCommands().hKeys(key);
    }

    @Override
    default Long hLen(byte[] key) {
        return hashCommands().hLen(key);
    }

    @Deprecated
    @Override
    default List<byte[]> hMGet(byte[] key, byte[]... fields) {
        return hashCommands().hMGet(key, fields);
    }

    @Deprecated
    @Override
    default void hMSet(byte[] key, Map<byte[], byte[]> hashes) {
        hashCommands().hMSet(key, hashes);
    }

    @Deprecated
    @Override
    default List<byte[]> hVals(byte[] key) {
        return hashCommands().hVals(key);
    }

    @Deprecated
    @Override
    default Cursor<Entry<byte[], byte[]>> hScan(byte[] key, ScanOptions options) {
        return hashCommands().hScan(key, options);
    }

    @Deprecated
    @Override
    default Long hStrLen(byte[] key, byte[] field) {
        return hashCommands().hStrLen(key, field);
    }

    // GEO COMMANDS

    @Deprecated
    @Override
    default Long geoAdd(byte[] key, Point point, byte[] member) {
        return geoCommands().geoAdd(key, point, member);
    }

    @Deprecated
    @Override
    default Long geoAdd(byte[] key, Map<byte[], Point> memberCoordinateMap) {
        return geoCommands().geoAdd(key, memberCoordinateMap);
    }

    @Deprecated
    @Override
    default Long geoAdd(byte[] key, Iterable<GeoLocation<byte[]>> locations) {
        return geoCommands().geoAdd(key, locations);
    }

    @Deprecated
    @Override
    default Distance geoDist(byte[] key, byte[] member1, byte[] member2) {
        return geoCommands().geoDist(key, member1, member2);
    }

    @Deprecated
    @Override
    default Distance geoDist(byte[] key, byte[] member1, byte[] member2, Metric metric) {
        return geoCommands().geoDist(key, member1, member2, metric);
    }

    @Deprecated
    @Override
    default List<String> geoHash(byte[] key, byte[]... members) {
        return geoCommands().geoHash(key, members);
    }

    @Deprecated
    @Override
    default List<Point> geoPos(byte[] key, byte[]... members) {
        return geoCommands().geoPos(key, members);
    }

    @Deprecated
    @Override
    default GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within) {
        return geoCommands().geoRadius(key, within);
    }

    @Deprecated
    @Override
    default GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within, GeoRadiusCommandArgs args) {
        return geoCommands().geoRadius(key, within, args);
    }

    @Deprecated
    @Override
    default GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius) {
        return geoCommands().geoRadiusByMember(key, member, radius);
    }

    @Deprecated
    @Override
    default GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius, GeoRadiusCommandArgs args) {
        return geoCommands().geoRadiusByMember(key, member, radius, args);
    }

    @Deprecated
    @Override
    default Long geoRemove(byte[] key, byte[]... members) {
        return geoCommands().geoRemove(key, members);
    }

    @Deprecated
    @Override
    default GeoResults<GeoLocation<byte[]>> geoSearch(byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchCommandArgs args) {
        return geoCommands().geoSearch(key, reference, predicate, args);
    }

    @Deprecated
    @Override
    default Long geoSearchStore(byte[] destKey, byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchStoreCommandArgs args) {
        return geoCommands().geoSearchStore(destKey, key, reference, predicate, args);
    }

    // HLL COMMANDS

    @Deprecated
    @Override
    default Long pfAdd(byte[] key, byte[]... values) {
        return hyperLogLogCommands().pfAdd(key, values);
    }

    @Deprecated
    @Override
    default Long pfCount(byte[]... keys) {
        return hyperLogLogCommands().pfCount(keys);
    }

    @Deprecated
    @Override
    default void pfMerge(byte[] destinationKey, byte[]... sourceKeys) {
        hyperLogLogCommands().pfMerge(destinationKey, sourceKeys);
    }

    // SERVER COMMANDS

    @Deprecated
    @Override
    default void bgReWriteAof() {
        serverCommands().bgReWriteAof();
    }

    @Deprecated
    @Override
    default void bgSave() {
        serverCommands().bgSave();
    }

    @Deprecated
    @Override
    default Long lastSave() {
        return serverCommands().lastSave();
    }

    @Deprecated
    @Override
    default void save() {
        serverCommands().save();
    }

    @Deprecated
    @Override
    default Long dbSize() {
        return serverCommands().dbSize();
    }

    @Deprecated
    @Override
    default void flushDb() {
        serverCommands().flushDb();
    }

    @Deprecated
    @Override
    default void flushAll() {
        serverCommands().flushAll();
    }

    @Deprecated
    @Override
    default Properties info() {
        return serverCommands().info();
    }

    @Deprecated
    @Override
    default Properties info(String section) {
        return serverCommands().info(section);
    }

    @Deprecated
    @Override
    default void shutdown() {
        serverCommands().shutdown();
    }

    @Deprecated
    @Override
    default void shutdown(ShutdownOption option) {
        serverCommands().shutdown(option);
    }

    @Deprecated
    @Override
    default Properties getConfig(String pattern) {
        return serverCommands().getConfig(pattern);
    }

    @Deprecated
    @Override
    default void setConfig(String param, String value) {
        serverCommands().setConfig(param, value);
    }

    @Deprecated
    @Override
    default void resetConfigStats() {
        serverCommands().resetConfigStats();
    }

    @Deprecated
    @Override
    default void rewriteConfig() {
        serverCommands().rewriteConfig();
    }

    @Deprecated
    @Override
    default Long time() {
        return serverCommands().time();
    }

    @Deprecated
    @Override
    default Long time(TimeUnit timeUnit) {
        return serverCommands().time(timeUnit);
    }

    @Deprecated
    @Override
    default void killClient(String host, int port) {
        serverCommands().killClient(host, port);
    }

    @Deprecated
    @Override
    default void setClientName(byte[] name) {
        serverCommands().setClientName(name);
    }

    @Deprecated
    @Override
    default String getClientName() {
        return serverCommands().getClientName();
    }

    @Deprecated
    @Override
    default List<RedisClientInfo> getClientList() {
        return serverCommands().getClientList();
    }

    @Deprecated
    @Override
    default void slaveOf(String host, int port) {
        serverCommands().slaveOf(host, port);
    }

    @Deprecated
    @Override
    default void slaveOfNoOne() {
        serverCommands().slaveOfNoOne();
    }

    @Deprecated
    @Override
    default void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option) {
        serverCommands().migrate(key, target, dbIndex, option);
    }

    @Deprecated
    @Override
    default void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option, long timeout) {
        serverCommands().migrate(key, target, dbIndex, option, timeout);
    }

    // SCRIPTING COMMANDS

    @Deprecated
    @Override
    default void scriptFlush() {
        scriptingCommands().scriptFlush();
    }

    @Deprecated
    @Override
    default void scriptKill() {
        scriptingCommands().scriptKill();
    }

    @Deprecated
    @Override
    default String scriptLoad(byte[] script) {
        return scriptingCommands().scriptLoad(script);
    }

    @Deprecated
    @Override
    default List<Boolean> scriptExists(String... scriptShas) {
        return scriptingCommands().scriptExists(scriptShas);
    }

    @Deprecated
    @Override
    default <T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        return scriptingCommands().eval(script, returnType, numKeys, keysAndArgs);
    }

    @Deprecated
    @Override
    default <T> T evalSha(String scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        return scriptingCommands().evalSha(scriptSha, returnType, numKeys, keysAndArgs);
    }

    @Deprecated
    @Override
    default <T> T evalSha(byte[] scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        return scriptingCommands().evalSha(scriptSha, returnType, numKeys, keysAndArgs);
    }
}
