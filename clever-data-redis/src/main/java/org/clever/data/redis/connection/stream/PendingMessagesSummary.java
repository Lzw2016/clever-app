package org.clever.data.redis.connection.stream;

import org.clever.data.domain.Range;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.Map;

/**
 * 值对象汇总了 {@literal consumer group} 中的未决消息。
 * 它包含此消费者组的待处理消息的总数和 ID 范围，以及每个消费者的待处理消息总数的集合
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:19 <br/>
 */
public class PendingMessagesSummary {
    private final String groupName;
    private final Long totalPendingMessages;
    private final Range<String> idRange;
    private final Map<String, Long> pendingMessagesPerConsumer;

    public PendingMessagesSummary(String groupName, long totalPendingMessages, Range<String> idRange, Map<String, Long> pendingMessagesPerConsumer) {
        Assert.notNull(idRange, "ID Range must not be null");
        Assert.notNull(pendingMessagesPerConsumer, "Pending Messages must not be null");
        this.groupName = groupName;
        this.totalPendingMessages = totalPendingMessages;
        this.idRange = idRange;
        this.pendingMessagesPerConsumer = pendingMessagesPerConsumer;
    }

    /**
     * 获取待处理消息中最小和最大 ID 之间的范围
     *
     * @return 从不为 {@literal null}
     */
    public Range<String> getIdRange() {
        return idRange;
    }

    /**
     * 获取待处理消息中最小的 ID
     *
     * @return 从不为 {@literal null}
     */
    public RecordId minRecordId() {
        return RecordId.of(minMessageId());
    }

    /**
     * 获取待处理消息中最大的 ID
     *
     * @return 从不为 {@literal null}
     */
    public RecordId maxRecordId() {
        return RecordId.of(maxMessageId());
    }

    /**
     * 获取待处理消息中最小的 ID 作为 {@link String}
     *
     * @return 从不为 {@literal null}
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String minMessageId() {
        return idRange.getLowerBound().getValue().get();
    }

    /**
     * 获取未决消息中最大的 ID 作为 {@link String}
     *
     * @return 从不为 {@literal null}
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String maxMessageId() {
        return idRange.getUpperBound().getValue().get();
    }

    /**
     * 获取 {@literal consumer group} 中的待处理消息总数
     *
     * @return 从不为 {@literal null}
     */
    public long getTotalPendingMessages() {
        return totalPendingMessages;
    }

    /**
     * @return {@literal consumer group} 名称
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 获取 {@literal consumer group} 中每个 {@literal consumer} 的映射，其中至少有一条待处理消息，以及待处理消息的数量
     *
     * @return 从不为 {@literal null}
     */
    public Map<String, Long> getPendingMessagesPerConsumer() {
        return Collections.unmodifiableMap(pendingMessagesPerConsumer);
    }

    @Override
    public String toString() {
        return "PendingMessagesSummary{" +
                "groupName='" + groupName + '\'' + ", " +
                "totalPendingMessages='" + getTotalPendingMessages() + '\'' + ", " +
                "minMessageId='" + minMessageId() + '\'' + ", " +
                "maxMessageId='" + maxMessageId() + '\'' + ", " +
                "pendingMessagesPerConsumer=" + pendingMessagesPerConsumer +
                '}';
    }
}
