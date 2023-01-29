package org.clever.data.redis.connection;

import org.clever.data.domain.Range;
import org.clever.data.redis.connection.stream.*;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 特定于流的 Redis 命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:24 <br/>
 */
public interface RedisStreamCommands {
    /**
     * 确认已处理的一个或多个记录(通过其id标识)
     *
     * @param key       流存储在 {@literal key}
     * @param group     消费组名称
     * @param recordIds 要确认的记录的 {@literal id's} 的字符串表示形式
     * @return 已确认消息的长度。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xack">Redis 文档: XACK</a>
     */
    default Long xAck(byte[] key, String group, String... recordIds) {
        return xAck(key, group, Arrays.stream(recordIds).map(RecordId::of).toArray(RecordId[]::new));
    }

    /**
     * 确认已处理的一个或多个记录(通过其id标识)
     *
     * @param key       流存储在 {@literal key}
     * @param group     消费组名称
     * @param recordIds 要确认的记录的 {@literal id's}
     * @return 已确认消息的长度。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xack">Redis 文档: XACK</a>
     */
    Long xAck(byte[] key, String group, RecordId... recordIds);

    /**
     * 将具有给定{@link Map 字段/值对}的新记录作为内容追加到存储在 {@code key} 的流中
     *
     * @param key     流存储在 {@literal key}
     * @param content 记录内容建模为{@link Map 字段/值对}。
     * @return the server generated {@link RecordId id}. {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xadd">Redis 文档: XADD</a>
     */
    default RecordId xAdd(byte[] key, Map<byte[], byte[]> content) {
        return xAdd(StreamRecords.newRecord().in(key).ofMap(content));
    }

    /**
     * 将给定的 {@link MapRecord record} 追加到存储在 {@code Record#getStream} 的流中。
     * 如果你更喜欢手动分配id而不是服务器生成的id，请确保通过 {@code Record#withId} 提供一个id
     *
     * @param record {@link MapRecord record}来追加
     * @return 保存后的 {@link RecordId id}。 {@literal null} 在管道/事务中使用时
     */
    default RecordId xAdd(MapRecord<byte[], byte[], byte[]> record) {
        return xAdd(record, XAddOptions.none());
    }

    /**
     * 将给定的 {@link MapRecord record} 追加到存储在 {@code Record#getStream} 的流中。
     * 如果你更喜欢手动分配id而不是服务器生成的id，请确保通过 {@code Record#withId} 提供一个id
     *
     * @param record  要追加的 {@link MapRecord record}
     * @param options 其他选项（例如 {@literal MAXLEN} ）。不能为 {@literal null} ，请改用 {@link XAddOptions#none()} 。
     * @return 保存后的 {@link RecordId id} 。 {@literal null} 在管道/事务中使用时
     */
    RecordId xAdd(MapRecord<byte[], byte[], byte[]> record, XAddOptions options);

    /**
     * 适用于 {@literal XADD} 命令的其他选项
     */
    class XAddOptions {
        private static final XAddOptions NONE = new XAddOptions(null, false);

        private final Long maxlen;
        private final boolean nomkstream;

        private XAddOptions(Long maxlen, boolean nomkstream) {
            this.maxlen = maxlen;
            this.nomkstream = nomkstream;
        }

        public static XAddOptions none() {
            return NONE;
        }

        /**
         * 将流的大小限制为给定的最大元素数
         *
         * @return {@link XAddOptions} 的新实例
         */
        public static XAddOptions maxlen(long maxlen) {
            return new XAddOptions(maxlen, false);
        }

        /**
         * 如果流不存在，则禁用创建
         *
         * @return {@link XAddOptions} 的新实例
         */
        public static XAddOptions makeNoStream() {
            return new XAddOptions(null, true);
        }

        /**
         * 如果流不存在，则禁用创建
         *
         * @param makeNoStream {@code true} 如果流不存在，则不创建流
         * @return {@link XAddOptions} 的新实例
         */
        public static XAddOptions makeNoStream(boolean makeNoStream) {
            return new XAddOptions(null, makeNoStream);
        }

        /**
         * 将流的大小限制为给定的最大元素数
         *
         * @return 可以是 {@literal null}
         */
        public Long getMaxlen() {
            return maxlen;
        }

        /**
         * @return 如果设置了 {@literal MAXLEN} ，则为 {@literal true}
         */
        public boolean hasMaxlen() {
            return maxlen != null && maxlen > 0;
        }

        /**
         * @return 如果设置了 {@literal NOMKSTREAM} ，则为 {@literal true}
         */
        public boolean isNoMkStream() {
            return nomkstream;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            XAddOptions that = (XAddOptions) o;
            if (this.nomkstream != that.nomkstream) return false;
            return ObjectUtils.nullSafeEquals(this.maxlen, that.maxlen);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(this.maxlen);
            result = 31 * result + ObjectUtils.nullSafeHashCode(this.nomkstream);
            return result;
        }
    }

    /**
     * 将挂起消息的所有权更改为给定的新 {@literal consumer} ，而不增加传递的计数
     *
     * @param key      存储流的 {@literal key}
     * @param group    {@literal consumer group} 的名称
     * @param newOwner 新的 {@literal consumer} 的名称
     * @param options  不能是 {@literal null}
     * @return 更改用户的 {@link RecordId id} 列表
     * @see <a href="https://redis.io/commands/xclaim">Redis 文档: XCLAIM</a>
     */
    List<RecordId> xClaimJustId(byte[] key, String group, String newOwner, XClaimOptions options);

    /**
     * 将挂起消息的所有权更改为给定的新 {@literal consumer}
     *
     * @param key         存储流的 {@literal key}
     * @param group       {@literal consumer group} 的名称
     * @param newOwner    新的 {@literal consumer} 的名
     * @param minIdleTime 不能是 {@literal null}
     * @param recordIds   不能是 {@literal null}
     * @return 更改用户的 {@link ByteRecord} 列表
     * @see <a href="https://redis.io/commands/xclaim">Redis 文档: XCLAIM</a>
     */
    default List<ByteRecord> xClaim(byte[] key, String group, String newOwner, Duration minIdleTime, RecordId... recordIds) {
        return xClaim(key, group, newOwner, XClaimOptions.minIdle(minIdleTime).ids(recordIds));
    }

    /**
     * 将挂起消息的所有权更改为给定的新 {@literal consumer}
     *
     * @param key      存储流的 {@literal key}
     * @param group    {@literal consumer group} 的名称
     * @param newOwner 新的 {@literal consumer} 的名
     * @param options  不能是 {@literal null}
     * @return 更改用户的 {@link ByteRecord} 列表
     * @see <a href="https://redis.io/commands/xclaim">Redis 文档: XCLAIM</a>
     */
    List<ByteRecord> xClaim(byte[] key, String group, String newOwner, XClaimOptions options);

    class XClaimOptions {
        private final List<RecordId> ids;
        private final Duration minIdleTime;
        private final Duration idleTime;
        private final Instant unixTime;
        private final Long retryCount;
        private final boolean force;

        private XClaimOptions(List<RecordId> ids, Duration minIdleTime, Duration idleTime, Instant unixTime, Long retryCount, boolean force) {
            this.ids = new ArrayList<>(ids);
            this.minIdleTime = minIdleTime;
            this.idleTime = idleTime;
            this.unixTime = unixTime;
            this.retryCount = retryCount;
            this.force = force;
        }

        /**
         * 设置 {@literal min-idle-time} 以将命令限制为至少在给定的 {@link Duration} 内处于空闲状态的消息
         *
         * @param minIdleTime 不能是 {@literal null}
         * @return {@link XClaimOptions} 的新实例
         */
        public static XClaimOptionsBuilder minIdle(Duration minIdleTime) {
            return new XClaimOptionsBuilder(minIdleTime);
        }

        /**
         * 设置 {@literal min-idle-time} 以将命令限制为至少在给定的 {@literal milliseconds} 内处于空闲状态的消息
         *
         * @return new instance of {@link XClaimOptions}.
         */
        public static XClaimOptionsBuilder minIdleMs(long millis) {
            return minIdle(Duration.ofMillis(millis));
        }

        /**
         * 设置自上次传递邮件以来的空闲时间。要指定特定的时间点，请使用 {@link #time(Instant)}
         *
         * @param idleTime 空闲时间
         * @return {@code this}
         */
        public XClaimOptions idle(Duration idleTime) {
            return new XClaimOptions(ids, minIdleTime, idleTime, unixTime, retryCount, force);
        }

        /**
         * 将空闲时间设置为特定的unix时间（以毫秒为单位）。要定义相对空闲时间，请使用 {@link #idle(Duration)}
         *
         * @param unixTime 空闲时间
         * @return {@code this}
         */
        public XClaimOptions time(Instant unixTime) {
            return new XClaimOptions(ids, minIdleTime, idleTime, unixTime, retryCount, force);
        }

        /**
         * 将重试计数器设置为指定值
         *
         * @param retryCount 可以是 {@literal null} 。如果 {@literal null} ，则不会更改重试计数器
         * @return {@link XClaimOptions} 的新实例
         */
        public XClaimOptions retryCount(long retryCount) {
            return new XClaimOptions(ids, minIdleTime, idleTime, unixTime, retryCount, force);
        }

        /**
         * 强制在PEL中创建挂起的消息条目，即使只要给定的流记录id有效，该条目就不存在
         *
         * @return {@link XClaimOptions} 的新实例
         */
        public XClaimOptions force() {
            return new XClaimOptions(ids, minIdleTime, idleTime, unixTime, retryCount, true);
        }

        /**
         * 获取 {@literal ID} 的 {@link List}
         *
         * @return 从不 {@literal null}
         */
        public List<RecordId> getIds() {
            return ids;
        }

        /**
         * 获取 {@literal ID} 数组作为 {@link String String}
         *
         * @return 从不 {@literal null}
         */
        public String[] getIdsAsStringArray() {
            return getIds().stream().map(RecordId::getValue).toArray(String[]::new);
        }

        /**
         * 获取 {@literal min-idle-time}
         *
         * @return 从不 {@literal null}
         */
        public Duration getMinIdleTime() {
            return minIdleTime;
        }

        /**
         * 获取 {@literal IDLE ms} 时间
         *
         * @return 可以是 {@literal null}
         */
        public Duration getIdleTime() {
            return idleTime;
        }

        /**
         * Get the {@literal TIME ms-unix-time}
         */
        public Instant getUnixTime() {
            return unixTime;
        }

        /**
         * 获取 {@literal RETRYCOUNT count}
         */
        public Long getRetryCount() {
            return retryCount;
        }

        /**
         * 获取 {@literal FORCE} 标志
         */
        public boolean isForce() {
            return force;
        }

        public static class XClaimOptionsBuilder {
            private final Duration minIdleTime;

            XClaimOptionsBuilder(Duration minIdleTime) {
                Assert.notNull(minIdleTime, "Min idle time must not be null!");
                this.minIdleTime = minIdleTime;
            }

            /**
             * 设置要声明的 {@literal ID}
             *
             * @param ids 不能是 {@literal null}
             */
            public XClaimOptions ids(List<?> ids) {
                List<RecordId> idList = ids.stream().map(it -> it instanceof RecordId ? (RecordId) it : RecordId.of(it.toString())).collect(Collectors.toList());
                return new XClaimOptions(idList, minIdleTime, null, null, null, false);
            }

            /**
             * 设置要声明的 {@literal ID}
             *
             * @param ids 不能是 {@literal null}
             */
            public XClaimOptions ids(RecordId... ids) {
                return ids(Arrays.asList(ids));
            }

            /**
             * 设置要声明的 {@literal ID}
             *
             * @param ids 不能是 {@literal null}
             */
            public XClaimOptions ids(String... ids) {
                return ids(Arrays.asList(ids));
            }
        }
    }

    /**
     * 从流中删除具有给定id的记录。返回删除的项目数，如果某些id不存在，则可能与传递的id数不同
     *
     * @param key       存储流的 {@literal key}
     * @param recordIds 要删除的记录的id
     * @return 删除的条目数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xdel">Redis 文档: XDEL</a>
     */
    default Long xDel(byte[] key, String... recordIds) {
        return xDel(key, Arrays.stream(recordIds).map(RecordId::of).toArray(RecordId[]::new));
    }

    /**
     * 从流中删除具有给定id的记录。返回删除的项目数，如果某些id不存在，则可能与传递的id数不同
     *
     * @param key       存储流的 {@literal key}
     * @param recordIds 要删除的记录的id
     * @return 删除的条目数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xdel">Redis 文档: XDEL</a>
     */
    Long xDel(byte[] key, RecordId... recordIds);

    /**
     * 创建消费者组
     *
     * @param key        存储流的 {@literal key}
     * @param groupName  要创建的使用者组的名称
     * @param readOffset 开始的偏移量
     * @return {@literal ok} 如果成功。 {@literal null} 在管道/事务中使用时
     */
    String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset);

    /**
     * 创建消费者组
     *
     * @param key        存储流的 {@literal key}
     * @param groupName  要创建的使用者组的名称
     * @param readOffset 开始的偏移量
     * @param mkStream   如果为true，则组将创建流（如果尚未存在）（MKSTREAM）
     * @return {@literal ok} 如果成功。 {@literal null} 在管道/事务中使用时
     */
    String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset, boolean mkStream);

    /**
     * 从消费者组中删除消费者
     *
     * @param key          存储流的 {@literal key}
     * @param groupName    要从中删除使用者的组的名称
     * @param consumerName 要从组中删除的使用者的名称
     * @return {@literal true} 如果成功。 {@literal null} 在管道/事务中使用时
     */
    default Boolean xGroupDelConsumer(byte[] key, String groupName, String consumerName) {
        return xGroupDelConsumer(key, Consumer.from(groupName, consumerName));
    }

    /**
     * 从消费者组中删除消费者
     *
     * @param key      存储流的 {@literal key}
     * @param consumer 通过组名和使用者名称标识的使用者
     * @return {@literal true} 如果成功。 {@literal null} 在管道/事务中使用时
     */
    Boolean xGroupDelConsumer(byte[] key, Consumer consumer);

    /**
     * 销毁消费者组
     *
     * @param key       存储流的 {@literal key}
     * @param groupName 消费者组的名称
     * @return {@literal true} 如果成功。 {@literal null} 在管道/事务中使用时
     */
    Boolean xGroupDestroy(byte[] key, String groupName);

    /**
     * 获取存储在指定 {@literal key} 处的流的一般信息
     *
     * @param key 存储流的 {@literal key}
     * @return {@literal null} 在管道/事务中使用时
     */
    StreamInfo.XInfoStream xInfo(byte[] key);

    /**
     * 获取与存储在指定 {@literal key} 处的流相关联的 {@literal consumer groups} 的信息
     *
     * @param key 存储流的 {@literal key}
     * @return {@literal null} 在管道/事务中使用时
     */
    StreamInfo.XInfoGroups xInfoGroups(byte[] key);

    /**
     * 获取存储在指定 {@literal key} 处的流的特定 {@literal consumer groups} 中每个使用者的信息
     *
     * @param key       存储流的 {@literal key}
     * @param groupName {@literal consumer groups} 的名称
     * @return {@literal null} 在管道/事务中使用时
     */
    StreamInfo.XInfoConsumers xInfoConsumers(byte[] key, String groupName);

    /**
     * 获取流的长度
     *
     * @param key 存储流的 {@literal key}
     * @return 流长度 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xlen">Redis 文档: XLEN</a>
     */
    Long xLen(byte[] key);

    /**
     * 获取给定 {@literal consumer groups} 的 {@link PendingMessagesSummary}
     *
     * @param key       存储流的 {@literal key} 。 不能是 {@literal null}
     * @param groupName {@literal consumer groups}的名称。 不能是 {@literal null}
     * @return 在管道/事务中使用时，给定 {@literal consumer groups} 或 {@literal null} 中挂起消息的摘要。
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    PendingMessagesSummary xPending(byte[] key, String groupName);

    /**
     * 获取了给定 {@link Consumer} 的所有挂起消息的详细信息
     *
     * @param key      存储流的 {@literal key} 。 不能是 {@literal null}
     * @param consumer 要为其获取 {@link PendingMessages} 的使用者。 不能是 {@literal null}
     * @return 给定 {@link Consumer} 或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    default PendingMessages xPending(byte[] key, Consumer consumer) {
        return xPending(key, consumer.getGroup(), consumer.getName());
    }

    /**
     * 获取了给定 {@literal consumer} 的所有挂起消息的详细信息
     *
     * @param key          存储流的 {@literal key} 。 不能是 {@literal null}
     * @param groupName    {@literal consumer groups} 的名称。 不能是 {@literal null}
     * @param consumerName 要为其获取 {@link PendingMessages} 的使用者。 不能是 {@literal null}
     * @return 给定 {@link Consumer} 或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    default PendingMessages xPending(byte[] key, String groupName, String consumerName) {
        return xPending(key, groupName, XPendingOptions.unbounded().consumer(consumerName));
    }

    /**
     * 获取有关 {@literal consumer groups} 中给定 {@link Range} 的挂起 {@link PendingMessage messages} 的详细信息。
     *
     * @param key       存储流的 {@literal key} 。 不能是 {@literal null}
     * @param groupName {@literal consumer groups} 的名称。 不能是 {@literal null}
     * @param range     要搜索的邮件id的范围。 不能是 {@literal null}
     * @param count     限制结果的数量。 不能是 {@literal null}
     * @return 在管道/事务中使用时，给定 {@literal consumer groups} 或 {@literal null} 的挂起消息。
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    default PendingMessages xPending(byte[] key, String groupName, Range<?> range, Long count) {
        return xPending(key, groupName, XPendingOptions.range(range, count));
    }

    /**
     * 获取有关 {@literal consumer groups} 中给定 {@link Range} 和 {@link Consumer} 的挂起 {@link PendingMessage messages} 的详细信息。
     *
     * @param key      存储流的 {@literal key} 。 不能是 {@literal null}
     * @param consumer {@link Consumer} 的名称。 不能是 {@literal null}
     * @param range    要搜索的邮件id的范围。 不能是 {@literal null}
     * @param count    限制结果的数量。 不能是 {@literal null}
     * @return 给定 {@link Consumer} 或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    default PendingMessages xPending(byte[] key, Consumer consumer, Range<?> range, Long count) {
        return xPending(key, consumer.getGroup(), consumer.getName(), range, count);
    }

    /**
     * 获取有关 {@literal consumer groups} 中给定 {@link Range} 和 {@literal consumer} 的挂起 {@link PendingMessage messages} 的详细信息。
     *
     * @param key          存储流的 {@literal key} 。 不能是 {@literal null}
     * @param groupName    {@literal consumer groups} 的名称。 不能是 {@literal null}
     * @param consumerName {@literal consumer} 的名称。 不能是 {@literal null}
     * @param range        要搜索的邮件id的范围。 不能是 {@literal null}
     * @param count        限制结果的数量。 不能是 {@literal null}
     * @return 当在管道/事务中使用时，给定 {@literal consumer groups} 或 {@literal null} 中给定 {@literal consumer} 的挂起消息。
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    default PendingMessages xPending(byte[] key, String groupName, String consumerName, Range<?> range, Long count) {
        return xPending(key, groupName, XPendingOptions.range(range, count).consumer(consumerName));
    }

    /**
     * 获取有关应用给定 {@link XPendingOptions options} 的挂起 {@link PendingMessage messages} 的详细信息。
     *
     * @param key       存储流的 {@literal key} 。 不能是 {@literal null}
     * @param groupName {@literal consumer groups}的名称。 不能是 {@literal null}
     * @param options   包含 {@literal range} 、 {@literal consumer} 和 {@code count} 的选项。不能为 {@literal null} 。
     * @return 符合给定条件的挂起消息或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xpending">Redis 文档: xpending</a>
     */
    PendingMessages xPending(byte[] key, String groupName, XPendingOptions options);

    /**
     * 保存用于获取挂起消息的参数的值对象
     */
    class XPendingOptions {
        private final String consumerName;
        private final Range<?> range;
        private final Long count;

        private XPendingOptions(String consumerName, Range<?> range, Long count) {
            this.range = range;
            this.count = count;
            this.consumerName = consumerName;
        }

        /**
         * 使用无界的 {@link Range} （ {@literal - +} ）创建新的 {@link XPendingOptions}
         *
         * @return {@link XPendingOptions} 的新实例
         */
        public static XPendingOptions unbounded() {
            return new XPendingOptions(null, Range.unbounded(), null);
        }

        /**
         * 使用无界的 {@link Range} （ {@literal - +} ）创建新的 {@link XPendingOptions}
         *
         * @param count 要返回的最大 messages 数。 不能是 {@literal null}
         * @return {@link XPendingOptions} 的新实例
         */
        public static XPendingOptions unbounded(Long count) {
            return new XPendingOptions(null, Range.unbounded(), count);
        }

        /**
         * 使用给定的 {@link Range} 和限制创建新的 {@link XPendingOptions}
         *
         * @return {@link XPendingOptions} 的新实例
         */
        public static XPendingOptions range(Range<?> range, Long count) {
            return new XPendingOptions(null, range, count);
        }

        /**
         * 附加给定消费者
         *
         * @param consumerName 不能是 {@literal null}
         * @return {@link XPendingOptions} 的新实例
         */
        public XPendingOptions consumer(String consumerName) {
            return new XPendingOptions(consumerName, range, count);
        }

        /**
         * @return 从不 {@literal null}
         */
        public Range<?> getRange() {
            return range;
        }

        /**
         * @return 可以是 {@literal null}
         */

        public Long getCount() {
            return count;
        }

        /**
         * @return 可以是 {@literal null}
         */

        public String getConsumerName() {
            return consumerName;
        }

        /**
         * @return {@literal true} 如果存在使用者名称
         */
        public boolean hasConsumer() {
            return StringUtils.hasText(consumerName);
        }

        /**
         * @return {@literal true} 计数已设置
         */
        public boolean isLimited() {
            return count != null && count > -1;
        }
    }

    /**
     * 从存储在 {@literal key} 的流中检索特定 {@link Range} 内的所有 {@link ByteRecord record} 。<br />
     * 使用 {@link Range#unbounded()} 读取可能的最小和最大ID。
     *
     * @param key   存储流的 {@literal key}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    default List<ByteRecord> xRange(byte[] key, Range<String> range) {
        return xRange(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 从存储在 {@literal key} 中的流中检索 {@link RedisZSetCommands.Limit limited number} {@link Range} 内的 {@link ByteRecord records} 。<br />
     * 使用 {@link Range#unbounded（）} 读取可能的最小和最大ID。 <br />
     * 使用 {@link RedisZSetCommands.Limit#unlimited()} 读取所有记录。
     *
     * @param key   存储流的 {@literal key}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xrange">Redis 文档: XRANGE</a>
     */
    List<ByteRecord> xRange(byte[] key, Range<String> range, RedisZSetCommands.Limit limit);

    /**
     * 从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param streams 要读取的流
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    default List<ByteRecord> xRead(StreamOffset<byte[]>... streams) {
        return xRead(StreamReadOptions.empty(), streams);
    }

    /**
     * 从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param readOptions 读取参数
     * @param streams     要读取的流
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xread">Redis 文档: XREAD</a>
     */
    List<ByteRecord> xRead(StreamReadOptions readOptions, StreamOffset<byte[]>... streams);

    /**
     * 使用使用者组从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param consumer consumer/group
     * @param streams  要读取的流。
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    default List<ByteRecord> xReadGroup(Consumer consumer, StreamOffset<byte[]>... streams) {
        return xReadGroup(consumer, StreamReadOptions.empty(), streams);
    }

    /**
     * 使用使用者组从一个或多个 {@link StreamOffset} 读取记录
     *
     * @param consumer    consumer/group.
     * @param readOptions 读取参数
     * @param streams     要读取的流
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xreadgroup">Redis 文档: XREADGROUP</a>
     */
    List<ByteRecord> xReadGroup(Consumer consumer, StreamReadOptions readOptions, StreamOffset<byte[]>... streams);

    /**
     * 从特定 {@link Range} 内的流中以相反顺序读取记录
     *
     * @param key   stream key
     * @param range 不能是 {@literal null}
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    default List<ByteRecord> xRevRange(byte[] key, Range<String> range) {
        return xRevRange(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 从特定 {@link Range} 内的流中读取记录，并按相反顺序应用 {@link RedisZSetCommands.Limit}
     *
     * @param key   stream key
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return 列出结果流的成员。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xrevrange">Redis 文档: XREVRANGE</a>
     */
    List<ByteRecord> xRevRange(byte[] key, Range<String> range, RedisZSetCommands.Limit limit);

    /**
     * 将流修剪为 {@code count} 个元素
     *
     * @param key   stream key
     * @param count 流长度
     * @return 删除的条目数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xtrim">Redis 文档: XTRIM</a>
     */
    Long xTrim(byte[] key, long count);

    /**
     * 将流修剪为 {@code count} 个元素。
     *
     * @param key                 stream key
     * @param count               流长度
     * @param approximateTrimming 为了使性能最大化，必须以近似的方式进行修整
     * @return 删除的条目数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/xtrim">Redis 文档: XTRIM</a>
     */
    Long xTrim(byte[] key, long count, boolean approximateTrimming);
}
