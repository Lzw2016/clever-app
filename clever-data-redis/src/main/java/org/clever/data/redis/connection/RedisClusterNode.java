package org.clever.data.redis.connection;

import org.clever.util.Assert;
import org.clever.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 集群中 Redis 服务器的表示
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:30 <br/>
 */
public class RedisClusterNode extends RedisNode {
    private SlotRange slotRange;
    private LinkState linkState;
    private Set<Flag> flags = Collections.emptySet();

    protected RedisClusterNode() {
        super();
    }

    /**
     * 使用空的 {@link SlotRange} 创建新的 {@link RedisClusterNode}
     *
     * @param host 不能是 {@literal null}
     * @param port 不能是 {@literal null}
     */
    public RedisClusterNode(String host, int port) {
        this(host, port, SlotRange.empty());
    }

    /**
     * 使用 id 和空 {@link SlotRange} 创建新的 {@link RedisClusterNode}
     *
     * @param id 不能是 {@literal null}
     */
    public RedisClusterNode(String id) {
        this(SlotRange.empty());
        Assert.notNull(id, "Id must not be null!");
        this.id = id;
    }

    /**
     * 使用给定的 {@link SlotRange} 创建新的 {@link RedisClusterNode}
     *
     * @param host      不能是 {@literal null}
     * @param port      不能是 {@literal null}
     * @param slotRange 不能是 {@literal null}
     */
    public RedisClusterNode(String host, int port, SlotRange slotRange) {
        super(host, port);
        Assert.notNull(slotRange, "SlotRange must not be null!");
        this.slotRange = slotRange;
    }

    /**
     * 使用给定的 {@link SlotRange} 创建新的 {@link RedisClusterNode}
     *
     * @param slotRange 不能是 {@literal null}
     */
    public RedisClusterNode(SlotRange slotRange) {
        super();
        Assert.notNull(slotRange, "SlotRange must not be null!");
        this.slotRange = slotRange;
    }

    /**
     * 获取服务的 {@link SlotRange}
     *
     * @return 从不为 {@literal null}
     */
    public SlotRange getSlotRange() {
        return slotRange;
    }

    /**
     * @return 如果插槽被覆盖，则为真
     */
    public boolean servesSlot(int slot) {
        return slotRange.contains(slot);
    }

    /**
     * @return 可以是 {@literal null}
     */
    public LinkState getLinkState() {
        return linkState;
    }

    /**
     * @return 如果节点连接到集群，则为真
     */
    public boolean isConnected() {
        return LinkState.CONNECTED.equals(linkState);
    }

    /**
     * @return 从不为 {@literal null}
     */
    public Set<Flag> getFlags() {
        return flags == null ? Collections.emptySet() : flags;
    }

    /**
     * @return 如果节点被标记为失败，则为真
     */
    public boolean isMarkedAsFail() {
        if (!CollectionUtils.isEmpty(flags)) {
            return flags.contains(Flag.FAIL) || flags.contains(Flag.PFAIL);
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * 获取 {@link RedisClusterNodeBuilder} 以创建新的 {@link RedisClusterNode}
     *
     * @return 从不为 {@literal null}
     */
    public static RedisClusterNodeBuilder newRedisClusterNode() {
        return new RedisClusterNodeBuilder();
    }

    public static class SlotRange {
        private final Set<Integer> range;

        /**
         * @param lowerBound 不能是 {@literal null}
         * @param upperBound 不能是 {@literal null}
         */
        public SlotRange(Integer lowerBound, Integer upperBound) {
            Assert.notNull(lowerBound, "LowerBound must not be null!");
            Assert.notNull(upperBound, "UpperBound must not be null!");
            this.range = new LinkedHashSet<>();
            for (int i = lowerBound; i <= upperBound; i++) {
                this.range.add(i);
            }
        }

        public SlotRange(Collection<Integer> range) {
            this.range = CollectionUtils.isEmpty(range) ? Collections.emptySet() : new LinkedHashSet<>(range);
        }

        @Override
        public String toString() {
            return range.toString();
        }

        /**
         * @return 当插槽是范围的一部分时为真
         */
        public boolean contains(int slot) {
            return range.contains(slot);
        }

        public Set<Integer> getSlots() {
            return Collections.unmodifiableSet(range);
        }

        public int[] getSlotsArray() {
            int[] slots = new int[range.size()];
            int pos = 0;
            for (Integer value : range) {
                slots[pos++] = value;
            }
            return slots;
        }

        public static SlotRange empty() {
            return new SlotRange(Collections.emptySet());
        }
    }

    public enum LinkState {
        CONNECTED, DISCONNECTED
    }

    public enum Flag {
        MYSELF("myself"),
        MASTER("master"),
        SLAVE("slave"),
        FAIL("fail"),
        PFAIL("fail?"),
        HANDSHAKE("handshake"),
        NOADDR("noaddr"),
        NOFLAGS("noflags");

        private final String raw;

        Flag(String raw) {
            this.raw = raw;
        }

        public String getRaw() {
            return raw;
        }
    }

    /**
     * 用于创建新的 {@link RedisClusterNode} 的构建器
     */
    public static class RedisClusterNodeBuilder extends RedisNodeBuilder {
        Set<Flag> flags;
        LinkState linkState;
        SlotRange slotRange;

        public RedisClusterNodeBuilder() {
            this.slotRange = SlotRange.empty();
        }

        @Override
        public RedisClusterNodeBuilder listeningAt(String host, int port) {
            super.listeningAt(host, port);
            return this;
        }

        @Override
        public RedisClusterNodeBuilder withName(String name) {
            super.withName(name);
            return this;
        }

        @Override
        public RedisClusterNodeBuilder withId(String id) {
            super.withId(id);
            return this;
        }

        @Override
        public RedisClusterNodeBuilder promotedAs(NodeType nodeType) {
            super.promotedAs(nodeType);
            return this;
        }

        public RedisClusterNodeBuilder slaveOf(String masterId) {
            super.slaveOf(masterId);
            return this;
        }

        @Override
        public RedisClusterNodeBuilder replicaOf(String masterId) {
            super.replicaOf(masterId);
            return this;
        }

        /**
         * 为节点设置标志
         */
        public RedisClusterNodeBuilder withFlags(Set<Flag> flags) {
            this.flags = flags;
            return this;
        }

        /**
         * 设置 {@link SlotRange}
         */
        public RedisClusterNodeBuilder serving(SlotRange range) {
            this.slotRange = range;
            return this;
        }

        /**
         * 设置 {@link LinkState}
         */
        public RedisClusterNodeBuilder linkState(LinkState linkState) {
            this.linkState = linkState;
            return this;
        }

        @Override
        public RedisClusterNode build() {
            RedisNode base = super.build();
            RedisClusterNode node;
            if (base.host != null) {
                node = new RedisClusterNode(base.getHost(), base.getPort(), slotRange);
            } else {
                node = new RedisClusterNode(slotRange);
            }
            node.id = base.id;
            node.type = base.type;
            node.masterId = base.masterId;
            node.name = base.name;
            node.flags = flags;
            node.linkState = linkState;
            return node;
        }
    }
}
