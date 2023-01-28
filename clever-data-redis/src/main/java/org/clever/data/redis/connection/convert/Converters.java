package org.clever.data.redis.connection.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.data.geo.*;
import org.clever.data.redis.RedisSystemException;
import org.clever.data.redis.connection.DataType;
import org.clever.data.redis.connection.RedisClusterNode;
import org.clever.data.redis.connection.RedisClusterNode.Flag;
import org.clever.data.redis.connection.RedisClusterNode.LinkState;
import org.clever.data.redis.connection.RedisClusterNode.RedisClusterNodeBuilder;
import org.clever.data.redis.connection.RedisClusterNode.SlotRange;
import org.clever.data.redis.connection.RedisGeoCommands.DistanceUnit;
import org.clever.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.clever.data.redis.connection.RedisNode.NodeType;
import org.clever.data.redis.connection.RedisZSetCommands.Tuple;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.data.redis.util.ByteUtils;
import org.clever.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 普通型转换器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:16 <br/>
 */
abstract public class Converters {
    private static final Logger LOGGER = LoggerFactory.getLogger(Converters.class);
    private static final byte[] ONE = new byte[]{'1'};
    private static final byte[] ZERO = new byte[]{'0'};
    private static final String CLUSTER_NODES_LINE_SEPARATOR = "\n";

    /**
     * 返回始终返回其输入参数的 {@link Converter}
     *
     * @param <T> 函数的输入和输出对象的类型
     * @return 始终返回其输入参数的函数
     */
    public static <T> Converter<T, T> identityConverter() {
        return t -> t;
    }

    public static Boolean stringToBoolean(String source) {
        return ObjectUtils.nullSafeEquals("OK", source);
    }

    public static Converter<String, Boolean> stringToBooleanConverter() {
        return Converters::stringToBoolean;
    }

    public static Converter<String, Properties> stringToProps() {
        return Converters::toProperties;
    }

    public static Converter<Long, Boolean> longToBoolean() {
        return Converters::toBoolean;
    }

    public static Converter<String, DataType> stringToDataType() {
        return Converters::toDataType;
    }

    public static Properties toProperties(String source) {
        Properties info = new Properties();
        try (StringReader stringReader = new StringReader(source)) {
            info.load(stringReader);
        } catch (Exception ex) {
            throw new RedisSystemException("Cannot read Redis info", ex);
        }
        return info;
    }

    public static Properties toProperties(Map<?, ?> source) {
        Properties target = new Properties();
        target.putAll(source);
        return target;
    }

    public static Boolean toBoolean(Long source) {
        return source != null && source == 1L;
    }

    public static DataType toDataType(String source) {
        return DataType.fromCode(source);
    }

    public static byte[] toBit(Boolean source) {
        return (source ? ONE : ZERO);
    }

    /**
     * 将单行 {@code CLUSTER NODES} 的结果转换为 {@link RedisClusterNode}
     */
    protected static RedisClusterNode toClusterNode(String clusterNodesLine) {
        return ClusterNodesConverter.INSTANCE.convert(clusterNodesLine);
    }

    /**
     * 将 {@code CLUSTER NODES} 结果的行转换为 {@link RedisClusterNode}
     */
    public static Set<RedisClusterNode> toSetOfRedisClusterNodes(Collection<String> lines) {
        if (CollectionUtils.isEmpty(lines)) {
            return Collections.emptySet();
        }
        Set<RedisClusterNode> nodes = new LinkedHashSet<>(lines.size());
        for (String line : lines) {
            nodes.add(toClusterNode(line));
        }
        return nodes;
    }

    /**
     * 将 {@code CLUSTER NODES} 的结果转换为 {@link RedisClusterNode}
     */
    public static Set<RedisClusterNode> toSetOfRedisClusterNodes(String clusterNodes) {
        if (!StringUtils.hasText(clusterNodes)) {
            return Collections.emptySet();
        }
        String[] lines = clusterNodes.split(CLUSTER_NODES_LINE_SEPARATOR);
        return toSetOfRedisClusterNodes(Arrays.asList(lines));
    }

    public static List<Object> toObjects(Set<Tuple> tuples) {
        List<Object> tupleArgs = new ArrayList<>(tuples.size() * 2);
        for (Tuple tuple : tuples) {
            tupleArgs.add(tuple.getScore());
            tupleArgs.add(tuple.getValue());
        }
        return tupleArgs;
    }

    /**
     * 返回根据给定的 {@code seconds} 和 {@code microseconds} 构造的时间戳
     *
     * @param seconds      服务器时间（秒）
     * @param microseconds 以当前秒为单位的已用微秒
     */
    public static Long toTimeMillis(String seconds, String microseconds) {
        return NumberUtils.parseNumber(seconds, Long.class) * 1000L + NumberUtils.parseNumber(microseconds, Long.class) / 1000L;
    }

    /**
     * 返回根据给定的 {@code seconds} 和 {@code microseconds} 构造的时间戳
     *
     * @param seconds      服务器时间（秒）
     * @param microseconds 以当前秒为单位经过微秒
     * @param unit         目标单位
     */
    public static Long toTimeMillis(String seconds, String microseconds, TimeUnit unit) {
        long secondValue = TimeUnit.SECONDS.toMicros(NumberUtils.parseNumber(seconds, Long.class));
        long microValue = NumberUtils.parseNumber(microseconds, Long.class);
        return unit.convert(secondValue + microValue, TimeUnit.MICROSECONDS);
    }

    /**
     * 将 {@code seconds} 转换为给定的 {@link TimeUnit}
     *
     * @param seconds    不能是 {@literal null}
     * @param targetUnit 不能是 {@literal null}
     */
    public static long secondsToTimeUnit(long seconds, TimeUnit targetUnit) {
        Assert.notNull(targetUnit, "TimeUnit must not be null!");
        if (seconds > 0) {
            return targetUnit.convert(seconds, TimeUnit.SECONDS);
        }
        return seconds;
    }

    /**
     * 创建一个新的 {@link Converter} ，将秒转换为给定的 {@link TimeUnit}
     *
     * @param timeUnit 不能是 {@literal null}
     */
    public static Converter<Long, Long> secondsToTimeUnit(TimeUnit timeUnit) {
        return seconds -> secondsToTimeUnit(seconds, timeUnit);
    }

    /**
     * 将 {@code milliseconds} 转换为给定的 {@link TimeUnit}
     *
     * @param milliseconds 不能是 {@literal null}
     * @param targetUnit   不能是 {@literal null}
     */
    public static long millisecondsToTimeUnit(long milliseconds, TimeUnit targetUnit) {
        Assert.notNull(targetUnit, "TimeUnit must not be null!");
        if (milliseconds > 0) {
            return targetUnit.convert(milliseconds, TimeUnit.MILLISECONDS);
        }
        return milliseconds;
    }

    /**
     * 创建一个新的 {@link Converter} ，将毫秒转换为给定的 {@link TimeUnit}
     *
     * @param timeUnit 不能是 {@literal null}
     */
    public static Converter<Long, Long> millisecondsToTimeUnit(TimeUnit timeUnit) {
        return seconds -> millisecondsToTimeUnit(seconds, timeUnit);
    }

    /**
     * {@link Converter} 能够反序列化 {@link GeoResults}
     */
    public static <V> Converter<GeoResults<GeoLocation<byte[]>>, GeoResults<GeoLocation<V>>> deserializingGeoResultsConverter(RedisSerializer<V> serializer) {
        return new DeserializingGeoResultsConverter<>(serializer);
    }

    /**
     * {@link Converter} 能够使用给定的 {@link Metric} 将Double转换为 {@link Distance}
     */
    public static Converter<Double, Distance> distanceConverterForMetric(Metric metric) {
        return DistanceConverterFactory.INSTANCE.forMetric(metric);
    }

    /**
     * 将带有键值序列的数组输出（例如由 {@code CONFIG GET} 生成）从 {@link List} 转换为 {@link Properties}
     *
     * @param input 不能是 {@literal null}
     * @return 映射结果
     */
    public static Properties toProperties(List<String> input) {
        Assert.notNull(input, "Input list must not be null!");
        Assert.isTrue(input.size() % 2 == 0, "Input list must contain an even number of entries!");
        Properties properties = new Properties();
        for (int i = 0; i < input.size(); i += 2) {
            properties.setProperty(input.get(i), input.get(i + 1));
        }
        return properties;
    }

    /**
     * 返回一个转换器，用于将带有键值序列的数组输出（例如由 {@code CONFIG GET} 生成）从 {@link List} 转换为 {@link Properties}
     *
     * @return 转换器
     */
    public static Converter<List<String>, Properties> listToPropertiesConverter() {
        return Converters::toProperties;
    }

    /**
     * 返回要从 {@link Map} 转换为 {@link Properties} 的转换器
     *
     * @return 转换器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Converter<Map<K, V>, Properties> mapToPropertiesConverter() {
        return (Converter) MapToPropertiesConverter.INSTANCE;
    }

    /**
     * 将给定的秒转换为 {@link Duration} 或 {@literal null}
     *
     * @param seconds 可以是 {@literal null}
     * @return 将秒指定为 {@link Duration} 或 {@literal null}
     */
    public static Duration secondsToDuration(Long seconds) {
        return seconds != null ? Duration.ofSeconds(seconds) : null;
    }

    /**
     * 解析一个相当通用的Redis响应，例如一个列表，将其转换为一个有意义的结构，并尽最大努力转换 {@code byte[]} 和 {@link ByteBuffer}
     *
     * @param source     要分析的源
     * @param targetType 例如 {@link Map} 、 {@link String} ,...
     */
    public static <T> T parse(Object source, Class<T> targetType) {
        return targetType.cast(parse(source, "root", Collections.singletonMap("root", targetType)));
    }

    /**
     * 解析一个相当通用的Redis响应，例如一个列表，将其转换为一个有意义的结构，并根据 {@literal sourcePath} 和 {@literal typeHintMap} 对 {@code byte[]} 和{@link ByteBuffer} 进行尽力转换
     *
     * @param source      要分析的源
     * @param sourcePath  当前路径（使用“root”表示级别0）
     * @param typeHintMap 允许通配符({@literal *})的目标类型提示的源路径
     */
    @SuppressWarnings("unchecked")
    public static Object parse(Object source, String sourcePath, Map<String, Class<?>> typeHintMap) {
        String path = sourcePath;
        Class<?> targetType = typeHintMap.get(path);
        if (targetType == null) {
            String alternatePath = sourcePath.contains(".") ? sourcePath.substring(0, sourcePath.lastIndexOf(".")) + ".*" : sourcePath;
            targetType = typeHintMap.get(alternatePath);
            if (targetType == null) {
                if (sourcePath.endsWith("[]")) {
                    targetType = String.class;
                } else {
                    targetType = source.getClass();
                }
            } else {
                if (targetType == Map.class && sourcePath.endsWith("[]")) {
                    targetType = String.class;
                } else {
                    path = alternatePath;
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("parsing %s (%s) as %s.", sourcePath, path, targetType));
        }
        if (targetType == Object.class) {
            return source;
        }
        if (ClassUtils.isAssignable(String.class, targetType)) {
            if (source instanceof String) {
                return source.toString();
            }
            if (source instanceof byte[]) {
                return new String((byte[]) source);
            }
            if (source instanceof ByteBuffer) {
                return new String(ByteUtils.getBytes((ByteBuffer) source));
            }
        }
        if (ClassUtils.isAssignable(List.class, targetType) && source instanceof List) {
            List<Object> sourceCollection = (List<Object>) source;
            List<Object> targetList = new ArrayList<>();
            for (int i = 0; i < sourceCollection.size(); i++) {
                targetList.add(parse(sourceCollection.get(i), sourcePath + ".[" + i + "]", typeHintMap));
            }
            return targetList;
        }
        if (ClassUtils.isAssignable(Map.class, targetType) && source instanceof List) {
            List<Object> sourceCollection = ((List<Object>) source);
            Map<String, Object> targetMap = new LinkedHashMap<>();
            for (int i = 0; i < sourceCollection.size(); i = i + 2) {
                String key = parse(sourceCollection.get(i), path + ".[]", typeHintMap).toString();
                targetMap.put(key, parse(sourceCollection.get(i + 1), path + "." + key, typeHintMap));
            }
            return targetMap;
        }
        return source;
    }

    /**
     * 从 {@code key} 和 {@code value} 创建 {@link Map.Entry}
     */
    public static <K, V> Map.Entry<K, V> entryOf(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    enum DistanceConverterFactory {
        INSTANCE;

        /**
         * @param metric 可以是 {@literal null} 。默认值为 {@link DistanceUnit#METERS}
         * @return 从不 {@literal null}
         */
        DistanceConverter forMetric(Metric metric) {
            return new DistanceConverter(ObjectUtils.nullSafeEquals(Metrics.NEUTRAL, metric) ? DistanceUnit.METERS : metric);
        }

        static class DistanceConverter implements Converter<Double, Distance> {
            private final Metric metric;

            /**
             * @param metric 可以是 {@literal null} 。默认值为 {@link DistanceUnit#METERS}
             */
            DistanceConverter(Metric metric) {
                this.metric = ObjectUtils.nullSafeEquals(Metrics.NEUTRAL, metric) ? DistanceUnit.METERS : metric;
            }

            @Override
            public Distance convert(Double source) {
                return new Distance(source, metric);
            }
        }
    }

    static class DeserializingGeoResultsConverter<V> implements Converter<GeoResults<GeoLocation<byte[]>>, GeoResults<GeoLocation<V>>> {
        final RedisSerializer<V> serializer;

        public DeserializingGeoResultsConverter(RedisSerializer<V> serializer) {
            this.serializer = serializer;
        }

        @Override
        public GeoResults<GeoLocation<V>> convert(GeoResults<GeoLocation<byte[]>> source) {
            List<GeoResult<GeoLocation<V>>> values = new ArrayList<>(source.getContent().size());
            for (GeoResult<GeoLocation<byte[]>> value : source.getContent()) {
                values.add(new GeoResult<>(
                        new GeoLocation<>(serializer.deserialize(value.getContent().getName()), value.getContent().getPoint()),
                        value.getDistance()
                ));
            }
            return new GeoResults<>(values, source.getAverageDistance().getMetric());
        }
    }

    enum ClusterNodesConverter implements Converter<String, RedisClusterNode> {
        INSTANCE;
        private static final Map<String, Flag> flagLookupMap;

        static {
            flagLookupMap = new LinkedHashMap<>(Flag.values().length, 1);
            for (Flag flag : Flag.values()) {
                flagLookupMap.put(flag.getRaw(), flag);
            }
        }

        static final int ID_INDEX = 0;
        static final int HOST_PORT_INDEX = 1;
        static final int FLAGS_INDEX = 2;
        static final int MASTER_ID_INDEX = 3;
        static final int LINK_STATE_INDEX = 7;
        static final int SLOTS_INDEX = 8;

        @Override
        public RedisClusterNode convert(String source) {
            String[] args = source.split(" ");
            String[] hostAndPort = StringUtils.split(args[HOST_PORT_INDEX], ":");
            Assert.notNull(hostAndPort, "ClusterNode information does not define host and port!");
            SlotRange range = parseSlotRange(args);
            Set<Flag> flags = parseFlags(args);
            String portPart = hostAndPort[1];
            if (portPart.contains("@")) {
                portPart = portPart.substring(0, portPart.indexOf('@'));
            }
            RedisClusterNodeBuilder nodeBuilder = RedisClusterNode.newRedisClusterNode()
                    .listeningAt(hostAndPort[0], Integer.parseInt(portPart))
                    .withId(args[ID_INDEX])
                    .promotedAs(flags.contains(Flag.MASTER) ? NodeType.MASTER : NodeType.SLAVE)
                    .serving(range)
                    .withFlags(flags)
                    .linkState(parseLinkState(args));
            if (!args[MASTER_ID_INDEX].isEmpty() && !args[MASTER_ID_INDEX].startsWith("-")) {
                nodeBuilder.slaveOf(args[MASTER_ID_INDEX]);
            }
            return nodeBuilder.build();
        }

        private Set<Flag> parseFlags(String[] args) {
            String raw = args[FLAGS_INDEX];
            Set<Flag> flags = new LinkedHashSet<>(8, 1);
            if (StringUtils.hasText(raw)) {
                for (String flag : raw.split(",")) {
                    flags.add(flagLookupMap.get(flag));
                }
            }
            return flags;
        }

        private LinkState parseLinkState(String[] args) {
            String raw = args[LINK_STATE_INDEX];
            if (StringUtils.hasText(raw)) {
                return LinkState.valueOf(raw.toUpperCase());
            }
            return LinkState.DISCONNECTED;
        }

        private SlotRange parseSlotRange(String[] args) {
            Set<Integer> slots = new LinkedHashSet<>();
            for (int i = SLOTS_INDEX; i < args.length; i++) {
                String raw = args[i];
                if (raw.startsWith("[")) {
                    continue;
                }
                if (raw.contains("-")) {
                    String[] slotRange = StringUtils.split(raw, "-");
                    if (slotRange != null) {
                        int from = Integer.parseInt(slotRange[0]);
                        int to = Integer.parseInt(slotRange[1]);
                        for (int slot = from; slot <= to; slot++) {
                            slots.add(slot);
                        }
                    }
                } else {
                    slots.add(Integer.valueOf(raw));
                }
            }
            return new SlotRange(slots);
        }
    }
}
