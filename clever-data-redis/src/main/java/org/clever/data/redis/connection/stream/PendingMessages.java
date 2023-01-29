package org.clever.data.redis.connection.stream;

import org.clever.data.domain.Range;
import org.clever.data.util.Streamable;
import org.clever.util.Assert;

import java.util.Iterator;
import java.util.List;


/**
 * 对于给定的 {@link Range} 和偏移量，值对象保存有关 {@literal consumer group} 中未决消息的详细信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:21 <br/>
 */
public class PendingMessages implements Streamable<PendingMessage> {
    private final String groupName;
    private final Range<?> range;
    private final List<PendingMessage> pendingMessages;

    public PendingMessages(String groupName, List<PendingMessage> pendingMessages) {
        this(groupName, Range.unbounded(), pendingMessages);
    }

    public PendingMessages(String groupName, Range<?> range, List<PendingMessage> pendingMessages) {
        Assert.notNull(range, "Range must not be null");
        Assert.notNull(pendingMessages, "Pending Messages must not be null");
        this.groupName = groupName;
        this.range = range;
        this.pendingMessages = pendingMessages;
    }

    /**
     * 将范围添加到当前的 {@link PendingMessages}
     *
     * @param range 不能是 {@literal null}
     * @return {@link PendingMessages} 的新实例
     */
    public PendingMessages withinRange(Range<?> range) {
        return new PendingMessages(groupName, range, pendingMessages);
    }

    /**
     * {@literal consumer group} 名称
     *
     * @return 从不为 {@literal null}
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * {@link Range} 待处理消息已加载
     *
     * @return 从不为 {@literal null}
     */
    public Range<?> getRange() {
        return range;
    }

    /**
     * @return {@literal true} 如果范围内没有待处理的消息
     */
    public boolean isEmpty() {
        return pendingMessages.isEmpty();
    }

    /**
     * @return 范围内的未决消息数
     */
    public int size() {
        return pendingMessages.size();
    }

    /**
     * 在给定位置获取 {@link PendingMessage}
     *
     * @return {@link PendingMessage} 给定的索引
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    public PendingMessage get(int index) {
        return pendingMessages.get(index);
    }

    @Override
    public Iterator<PendingMessage> iterator() {
        return pendingMessages.iterator();
    }

    @Override
    public String toString() {
        return "PendingMessages{" +
                "groupName='" + groupName + '\'' + ", " +
                "range=" + range + ", " +
                "pendingMessages=" + pendingMessages +
                '}';
    }
}
