package org.clever.data.redis.connection.stream;

import java.time.Duration;

/**
 * 表示单个待处理消息的值对象，其中包含其 {@literal ID}、获取消息但仍需确认消息的 {@literal consumer}、自上次传递消息以来经过的时间以及传递的总次数。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:23 <br/>
 */
public class PendingMessage {
    private final RecordId id;
    private final Consumer consumer;
    private final Duration elapsedTimeSinceLastDelivery;
    private final Long totalDeliveryCount;

    public PendingMessage(RecordId id, Consumer consumer, Duration elapsedTimeSinceLastDelivery, long totalDeliveryCount) {
        this.id = id;
        this.consumer = consumer;
        this.elapsedTimeSinceLastDelivery = elapsedTimeSinceLastDelivery;
        this.totalDeliveryCount = totalDeliveryCount;
    }

    /**
     * @return 消息 ID
     */
    public RecordId getId() {
        return id;
    }

    /**
     * @return 消息 ID 为 {@link String}
     */
    public String getIdAsString() {
        return id.getValue();
    }

    /**
     * {@link Consumer} 确认消息
     *
     * @return 从不为 {@literal null}
     */
    public Consumer getConsumer() {
        return consumer;
    }

    /**
     * {@literal consumer name} 确认消息
     *
     * @return 从不为 {@literal null}
     */
    public String getConsumerName() {
        return consumer.getName();
    }

    /**
     * 获取 {@literal consumer name}
     *
     * @return 从不为 {@literal null}
     */
    public String getGroupName() {
        return consumer.getGroup();
    }

    /**
     * 获取自上次将消息传递给 {@link #getConsumer() consumer}以来经过的时间（精确到毫秒）
     *
     * @return 从不为 {@literal null}
     */
    public Duration getElapsedTimeSinceLastDelivery() {
        return elapsedTimeSinceLastDelivery;
    }

    /**
     * 获取消息已传递给 {@link #getConsumer() consumer} 的总次数
     *
     * @return 从不为 {@literal null}
     */
    public long getTotalDeliveryCount() {
        return totalDeliveryCount;
    }

    @Override
    public String toString() {
        return "PendingMessage{" +
                "id=" + id + ", " +
                "consumer=" + consumer + ", " +
                "elapsedTimeSinceLastDeliveryMS=" + elapsedTimeSinceLastDelivery.toMillis() + ", " +
                "totalDeliveryCount=" + totalDeliveryCount +
                '}';
    }
}
