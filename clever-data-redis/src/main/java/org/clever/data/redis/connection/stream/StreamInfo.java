package org.clever.data.redis.connection.stream;

import org.clever.data.redis.connection.convert.Converters;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:14 <br/>
 */
public class StreamInfo {
    public static class XInfoObject {
        protected static final Map<String, Class<?>> DEFAULT_TYPE_HINTS;

        static {
            Map<String, Class<?>> defaults = new HashMap<>(2);
            defaults.put("root", Map.class);
            defaults.put("root.*", String.class);
            DEFAULT_TYPE_HINTS = Collections.unmodifiableMap(defaults);
        }

        private final Map<String, Object> raw;

        @SuppressWarnings("unchecked")
        private XInfoObject(List<Object> raw, Map<String, Class<?>> typeHints) {
            this((Map<String, Object>) Converters.parse(raw, "root", typeHints));
        }

        private XInfoObject(Map<String, Object> raw) {
            this.raw = raw;
        }

        <T> T get(String entry, Class<T> type) {
            Object value = raw.get(entry);
            return value == null ? null : type.cast(value);
        }

        @SuppressWarnings("SameParameterValue")
        <I, T> T getAndMap(String entry, Class<I> type, Function<I, T> f) {
            I value = get(entry, type);
            return value == null ? null : f.apply(value);
        }

        public Map<String, Object> getRaw() {
            return raw;
        }

        @Override
        public String toString() {
            return "XInfoStream" + raw;
        }
    }

    /**
     * 保存有关 {@literal Redis Stream} 的一般信息的值对象
     */
    public static class XInfoStream extends XInfoObject {
        private static final Map<String, Class<?>> typeHints;

        static {
            typeHints = new HashMap<>(DEFAULT_TYPE_HINTS);
            typeHints.put("root.first-entry", Map.class);
            typeHints.put("root.first-entry.*", Map.class);
            typeHints.put("root.first-entry.*.*", Object.class);
            typeHints.put("root.last-entry", Map.class);
            typeHints.put("root.last-entry.*", Map.class);
            typeHints.put("root.last-entry.*.*", Object.class);
        }

        private XInfoStream(List<Object> raw) {
            super(raw, typeHints);
        }

        /**
         * 用于创建 {@link XInfoStream} 新实例的工厂方法
         *
         * @param source 原始值 source
         */
        public static XInfoStream fromList(List<Object> source) {
            return new XInfoStream(source);
        }

        /**
         * 流中的元素总数。对应于{@literal length}
         */
        public Long streamLength() {
            return get("length", Long.class);
        }

        /**
         * 流基数树密钥大小。对应于{@literal radix-tree-keys}
         */
        public Long radixTreeKeySize() {
            return get("radix-tree-keys", Long.class);
        }

        /**
         * 元素基数树节点的总数。对应于{@literal radix-tree-nodes}
         */
        public Long radixTreeNodesSize() {
            return get("radix-tree-nodes", Long.class);
        }

        /**
         * 关联的{@literal 消费者群体}的数量。对应于{@literal groups}
         */
        public Long groupCount() {
            return get("groups", Long.class);
        }

        /**
         * 最后生成的 id。可能与 {@link #lastEntryId()} 不同。对应于 {@literal last-generated-id}
         */
        public String lastGeneratedId() {
            return get("last-generated-id", String.class);
        }

        /**
         * 流第一个条目的 ID。对应于{@literal first-entry 1)}
         */
        public String firstEntryId() {
            return getAndMap("first-entry", Map.class, it -> it.keySet().iterator().next().toString());
        }

        /**
         * 流的第一个条目。对应于{@literal first-entry}
         */
        @SuppressWarnings("unchecked")
        public Map<Object, Object> getFirstEntry() {
            return getAndMap("first-entry", Map.class, Collections::unmodifiableMap);
        }

        /**
         * 流的最后一个条目的 ID。对应于{@literal last-entry 1)}
         */
        public String lastEntryId() {
            return getAndMap("last-entry", Map.class, it -> it.keySet().iterator().next().toString());
        }

        /**
         * 流的第一个条目。对应于{@literal last-entry}
         */
        @SuppressWarnings("unchecked")
        public Map<Object, Object> getLastEntry() {
            return getAndMap("last-entry", Map.class, Collections::unmodifiableMap);
        }

    }

    /**
     * 值对象保存有关与 {@literal Redis Stream} 关联的 {@literal consumer groups} 的一般信息
     */
    public static class XInfoGroups {
        private final List<XInfoGroup> groupInfoList;

        @SuppressWarnings("unchecked")
        private XInfoGroups(List<Object> raw) {
            groupInfoList = new ArrayList<>();
            for (Object entry : raw) {
                groupInfoList.add(new XInfoGroup((List<Object>) entry));
            }
        }

        /**
         * 创建 {@link XInfoGroups} 新实例的工厂方法
         *
         * @param source 原始值 source
         */
        public static XInfoGroups fromList(List<Object> source) {
            return new XInfoGroups(source);
        }

        /**
         * 关联的{@literal consumer groups} 总数
         *
         * @return zero if none available.
         */
        public int groupCount() {
            return size();
        }

        /**
         * 返回可用的 {@link XInfoGroup} 的数量
         *
         * @return 如果没有可用则为零
         * @see #groupCount()
         */
        public int size() {
            return groupInfoList.size();
        }

        /**
         * @return {@literal true} 如果没有关联的组
         */
        public boolean isEmpty() {
            return groupInfoList.isEmpty();
        }

        /**
         * 返回 {@link XInfoGroup} 元素的迭代器
         */
        public Iterator<XInfoGroup> iterator() {
            return groupInfoList.iterator();
        }

        /**
         * 返回给定 {@literal index} 处的 {@link XInfoGroup} 元素
         *
         * @return 指定位置的元素
         * @throws IndexOutOfBoundsException 如果索引超出范围
         */
        public XInfoGroup get(int index) {
            return groupInfoList.get(index);
        }

        /**
         * 返回 {@link XInfoGroup} 的顺序 {@code Stream}
         */
        public Stream<XInfoGroup> stream() {
            return groupInfoList.stream();
        }

        /**
         * 对此 {@link XInfoGroups} 的每个可用 {@link XInfoGroup} 执行给定的 {@literal action}
         */
        public void forEach(Consumer<? super XInfoGroup> action) {
            groupInfoList.forEach(action);
        }

        @Override
        public String toString() {
            return "XInfoGroups" + groupInfoList;
        }

    }

    public static class XInfoGroup extends XInfoObject {
        private XInfoGroup(List<Object> raw) {
            super(raw, DEFAULT_TYPE_HINTS);
        }

        public static XInfoGroup fromList(List<Object> raw) {
            return new XInfoGroup(raw);
        }

        /**
         * {@literal consumer group} 名称。对应于{@literal name}
         */
        public String groupName() {
            return get("name", String.class);
        }

        /**
         * {@literal consumer group} 中的消费者总数。对应于{@literal consumers}
         */
        public Long consumerCount() {
            return get("consumers", Long.class);
        }

        /**
         * {@literal consumer group} 中待处理消息的总数。对应于{@literal pending}
         */
        public Long pendingCount() {
            return get("pending", Long.class);
        }

        /**
         * 最后发送的消息的 ID。对应于 {@literal last-delivered-id}
         */
        public String lastDeliveredId() {
            return get("last-delivered-id", String.class);
        }
    }

    public static class XInfoConsumers {
        private final List<XInfoConsumer> consumerInfoList;

        @SuppressWarnings("unchecked")
        public XInfoConsumers(String groupName, List<Object> raw) {
            consumerInfoList = new ArrayList<>();
            for (Object entry : raw) {
                consumerInfoList.add(new XInfoConsumer(groupName, (List<Object>) entry));
            }
        }

        public static XInfoConsumers fromList(String groupName, List<Object> source) {
            return new XInfoConsumers(groupName, source);
        }

        /**
         * {@literal consumer group} 中的 {@literal 消费者} 总数
         *
         * @return 如果没有可用则为零
         */
        public int getConsumerCount() {
            return consumerInfoList.size();
        }

        /**
         * 返回可用的 {@link XInfoConsumer} 的数量
         *
         * @return 如果没有可用则为零
         * @see #getConsumerCount()
         */
        public int size() {
            return consumerInfoList.size();
        }

        /**
         * @return {@literal true} 如果没有关联的组
         */
        public boolean isEmpty() {
            return consumerInfoList.isEmpty();
        }

        /**
         * 返回 {@link XInfoConsumer} 元素的迭代器
         */
        public Iterator<XInfoConsumer> iterator() {
            return consumerInfoList.iterator();
        }

        /**
         * 返回给定 {@literal index} 处的 {@link XInfoConsumer} 元素
         *
         * @return 指定位置的元素
         * @throws IndexOutOfBoundsException 如果索引超出范围
         */
        public XInfoConsumer get(int index) {
            return consumerInfoList.get(index);
        }

        /**
         * 返回 {@link XInfoConsumer} 的顺序 {@code Stream}
         */
        public Stream<XInfoConsumer> stream() {
            return consumerInfoList.stream();
        }

        /**
         * 对此 {@link XInfoConsumers} 的每个可用 {@link XInfoConsumer} 执行给定的 {@literal action}
         */
        public void forEach(Consumer<? super XInfoConsumer> action) {
            consumerInfoList.forEach(action);
        }

        @Override
        public String toString() {
            return "XInfoConsumers" + consumerInfoList;
        }
    }

    public static class XInfoConsumer extends XInfoObject {
        private final String groupName;

        public XInfoConsumer(String groupName, List<Object> raw) {
            super(raw, DEFAULT_TYPE_HINTS);
            this.groupName = groupName;
        }

        /**
         * {@literal consumer group} 名称
         */
        public String groupName() {
            return groupName;
        }

        /**
         * {@literal consumer} 名称。对应于 {@literal name}
         */
        public String consumerName() {
            return get("name", String.class);
        }

        /**
         * 空闲时间（以毫秒为单位）。对应于 {@literal idle}
         */
        public Long idleTimeMs() {
            return get("idle", Long.class);
        }

        /**
         * 空闲时间。对应于 {@literal idle}
         */
        public Duration idleTime() {
            return Duration.ofMillis(idleTimeMs());
        }

        /**
         * 待处理消息的数量。对应于 {@literal pending}
         */
        public Long pendingCount() {
            return get("pending", Long.class);
        }
    }
}
