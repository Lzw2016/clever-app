package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.*;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import org.clever.core.convert.converter.Converter;
import org.clever.dao.DataAccessException;
import org.clever.data.geo.*;
import org.clever.data.redis.connection.*;
import org.clever.data.redis.connection.BitFieldSubCommands.BitFieldGet;
import org.clever.data.redis.connection.BitFieldSubCommands.BitFieldIncrBy;
import org.clever.data.redis.connection.BitFieldSubCommands.BitFieldIncrBy.Overflow;
import org.clever.data.redis.connection.BitFieldSubCommands.BitFieldSet;
import org.clever.data.redis.connection.BitFieldSubCommands.BitFieldSubCommand;
import org.clever.data.redis.connection.RedisClusterNode.Flag;
import org.clever.data.redis.connection.RedisClusterNode.LinkState;
import org.clever.data.redis.connection.RedisClusterNode.SlotRange;
import org.clever.data.redis.connection.RedisListCommands.Direction;
import org.clever.data.redis.connection.RedisListCommands.Position;
import org.clever.data.redis.connection.RedisNode.NodeType;
import org.clever.data.redis.connection.RedisStringCommands.SetOption;
import org.clever.data.redis.connection.RedisZSetCommands.Range.Boundary;
import org.clever.data.redis.connection.RedisZSetCommands.Tuple;
import org.clever.data.redis.connection.SortParameters.Order;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.data.redis.connection.convert.LongToBooleanConverter;
import org.clever.data.redis.connection.convert.StringToRedisClientInfoConverter;
import org.clever.data.redis.core.KeyScanOptions;
import org.clever.data.redis.core.ScanOptions;
import org.clever.data.redis.core.types.Expiration;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.data.redis.domain.geo.*;
import org.clever.data.redis.util.ByteUtils;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.clever.data.redis.connection.RedisGeoCommands.GeoLocation;
import static org.clever.data.redis.connection.RedisGeoCommands.*;
import static org.clever.data.redis.domain.geo.GeoReference.GeoCoordinateReference;
import static org.clever.data.redis.domain.geo.GeoReference.GeoMemberReference;

/**
 * Lettuce型转换器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:37 <br/>
 */
public abstract class LettuceConverters extends Converters {
    private static final Converter<Exception, DataAccessException> EXCEPTION_CONVERTER = new LettuceExceptionConverter();

    public static final byte[] PLUS_BYTES;
    public static final byte[] MINUS_BYTES;
    public static final byte[] POSITIVE_INFINITY_BYTES;
    public static final byte[] NEGATIVE_INFINITY_BYTES;

    private static final long INDEXED_RANGE_START = 0;
    private static final long INDEXED_RANGE_END = -1;

    static {
        PLUS_BYTES = toBytes("+");
        MINUS_BYTES = toBytes("-");
        POSITIVE_INFINITY_BYTES = toBytes("+inf");
        NEGATIVE_INFINITY_BYTES = toBytes("-inf");
    }

    public static Point geoCoordinatesToPoint(GeoCoordinates geoCoordinate) {
        return geoCoordinate != null ? new Point(geoCoordinate.getX().doubleValue(), geoCoordinate.getY().doubleValue()) : null;
    }

    public static Converter<String, List<RedisClientInfo>> stringToRedisClientListConverter() {
        return LettuceConverters::toListOfRedisClientInformation;
    }

    public static Converter<List<ScoredValue<byte[]>>, List<Tuple>> scoredValuesToTupleList() {
        return source -> {
            if (source == null) {
                return null;
            }
            List<Tuple> tuples = new ArrayList<>(source.size());
            for (ScoredValue<byte[]> value : source) {
                tuples.add(LettuceConverters.toTuple(value));
            }
            return tuples;
        };
    }

    // @Deprecated
    public static Converter<Exception, DataAccessException> exceptionConverter() {
        return EXCEPTION_CONVERTER;
    }

    public static Converter<Long, Boolean> longToBooleanConverter() {
        return LongToBooleanConverter.INSTANCE;
    }

    public static Long toLong(Date source) {
        return source != null ? source.getTime() : null;
    }

    public static Set<byte[]> toBytesSet(List<byte[]> source) {
        return source != null ? new LinkedHashSet<>(source) : null;
    }

    public static List<byte[]> toBytesList(KeyValue<byte[], byte[]> source) {
        if (source == null) {
            return null;
        }
        List<byte[]> list = new ArrayList<>(2);
        list.add(source.getKey());
        list.add(source.getValue());
        return list;
    }

    public static List<byte[]> toBytesList(Collection<byte[]> source) {
        if (source instanceof List) {
            return (List<byte[]>) source;
        }
        return source != null ? new ArrayList<>(source) : null;
    }

    public static Tuple toTuple(ScoredValue<byte[]> source) {
        return source != null && source.hasValue() ? new DefaultTuple(source.getValue(), source.getScore()) : null;
    }

    public static String toString(byte[] source) {
        if (source == null || Arrays.equals(source, new byte[0])) {
            return null;
        }
        return new String(source);
    }

    public static ScriptOutputType toScriptOutputType(ReturnType returnType) {
        switch (returnType) {
            case BOOLEAN:
                return ScriptOutputType.BOOLEAN;
            case MULTI:
                return ScriptOutputType.MULTI;
            case VALUE:
                return ScriptOutputType.VALUE;
            case INTEGER:
                return ScriptOutputType.INTEGER;
            case STATUS:
                return ScriptOutputType.STATUS;
            default:
                throw new IllegalArgumentException("Return type " + returnType + " is not a supported script output type");
        }
    }

    public static boolean toBoolean(Position where) {
        Assert.notNull(where, "list positions are mandatory");
        return (!Position.AFTER.equals(where));
    }

    public static int toInt(boolean value) {
        return (value ? 1 : 0);
    }

    public static Map<byte[], byte[]> toMap(List<byte[]> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyMap();
        }
        Map<byte[], byte[]> target = new LinkedHashMap<>();
        Iterator<byte[]> kv = source.iterator();
        while (kv.hasNext()) {
            target.put(kv.next(), kv.hasNext() ? kv.next() : null);
        }
        return target;
    }

    public static SortArgs toSortArgs(SortParameters params) {
        SortArgs args = new SortArgs();
        if (params == null) {
            return args;
        }
        if (params.getByPattern() != null) {
            args.by(new String(params.getByPattern(), StandardCharsets.US_ASCII));
        }
        if (params.getLimit() != null) {
            args.limit(params.getLimit().getStart(), params.getLimit().getCount());
        }
        if (params.getGetPattern() != null) {
            byte[][] pattern = params.getGetPattern();
            for (byte[] bs : pattern) {
                args.get(new String(bs, StandardCharsets.US_ASCII));
            }
        }
        if (params.getOrder() != null) {
            if (params.getOrder() == Order.ASC) {
                args.asc();
            } else {
                args.desc();
            }
        }
        Boolean isAlpha = params.isAlphabetic();
        if (isAlpha != null && isAlpha) {
            args.alpha();
        }
        return args;
    }

    public static List<RedisClientInfo> toListOfRedisClientInformation(String clientList) {
        if (!StringUtils.hasText(clientList)) {
            return Collections.emptyList();
        }
        return StringToRedisClientInfoConverter.INSTANCE.convert(clientList.split("\\r?\\n"));
    }

    private static String boundaryToString(Boundary boundary, String inclPrefix, String exclPrefix) {
        String prefix = boundary.isIncluding() ? inclPrefix : exclPrefix;
        String value = null;
        if (boundary.getValue() instanceof byte[]) {
            value = toString((byte[]) boundary.getValue());
        } else {
            value = boundary.getValue().toString();
        }
        return prefix + value;
    }

    /**
     * 将 {@link RedisZSetCommands.Limit} 转换为 Lettuce {@link io.lettuce.core.Limit}
     *
     * @return Lettuce {@link io.lettuce.core.Limit}
     */
    public static io.lettuce.core.Limit toLimit(RedisZSetCommands.Limit limit) {
        return limit.isUnlimited() ? Limit.unlimited() : Limit.create(limit.getOffset(), limit.getCount());
    }

    /**
     * 将 {@link RedisZSetCommands.Range} 转换为Lettuce {@link Range}
     */
    public static <T> Range<T> toRange(RedisZSetCommands.Range range) {
        return Range.from(lowerBoundaryOf(range, false), upperBoundaryOf(range, false));
    }

    /**
     * 将 {@link RedisZSetCommands.Range} 转换为Lettuce {@link Range}
     */
    public static <T> Range<T> toRange(RedisZSetCommands.Range range, boolean convertNumberToBytes) {
        return Range.from(lowerBoundaryOf(range, convertNumberToBytes), upperBoundaryOf(range, convertNumberToBytes));
    }

    /**
     * 将 {@link RedisZSetCommands.Range} 转换为Lettuce {@link Range} 并反转边界
     */
    public static <T> Range<T> toRevRange(RedisZSetCommands.Range range) {
        return Range.from(upperBoundaryOf(range, false), lowerBoundaryOf(range, false));
    }

    @SuppressWarnings("unchecked")
    private static <T> Range.Boundary<T> lowerBoundaryOf(RedisZSetCommands.Range range, boolean convertNumberToBytes) {
        return (Range.Boundary<T>) rangeToBoundaryArgumentConverter(false, convertNumberToBytes).convert(range);
    }

    @SuppressWarnings("unchecked")
    private static <T> Range.Boundary<T> upperBoundaryOf(RedisZSetCommands.Range range, boolean convertNumberToBytes) {
        return (Range.Boundary<T>) rangeToBoundaryArgumentConverter(true, convertNumberToBytes).convert(range);
    }

    private static Converter<RedisZSetCommands.Range, Range.Boundary<?>> rangeToBoundaryArgumentConverter(boolean upper, boolean convertNumberToBytes) {
        return (source) -> {
            Boundary sourceBoundary = upper ? source.getMax() : source.getMin();
            if (sourceBoundary == null || sourceBoundary.getValue() == null) {
                return Range.Boundary.unbounded();
            }
            boolean inclusive = sourceBoundary.isIncluding();
            Object value = sourceBoundary.getValue();
            if (value instanceof Number) {
                if (convertNumberToBytes) {
                    value = value.toString();
                } else {
                    return inclusive ? Range.Boundary.including((Number) value) : Range.Boundary.excluding((Number) value);
                }
            }
            if (value instanceof String) {
                if (!StringUtils.hasText((String) value) || ObjectUtils.nullSafeEquals(value, "+") || ObjectUtils.nullSafeEquals(value, "-")) {
                    return Range.Boundary.unbounded();
                }
                return inclusive ? Range.Boundary.including(value.toString().getBytes(StandardCharsets.UTF_8)) : Range.Boundary.excluding(value.toString().getBytes(StandardCharsets.UTF_8));
            }
            return inclusive ? Range.Boundary.including((byte[]) value) : Range.Boundary.excluding((byte[]) value);
        };
    }

    /**
     * @param source 包含来自 SENTINEL SLAVES 或 SENTINEL MASTERS 的节点详细信息的映射列表。可能为空或 {@literal null}
     * @return {@link RedisServer} 的列表。如果地图列表为空，则列表为空
     */
    public static List<RedisServer> toListOfRedisServer(List<Map<String, String>> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        List<RedisServer> sentinels = new ArrayList<>();
        for (Map<String, String> info : source) {
            sentinels.add(RedisServer.newServerFrom(Converters.toProperties(info)));
        }
        return sentinels;
    }

    /**
     * @param sentinelConfiguration 包含一个或多个哨兵和主名称的哨兵配置。不能是 {@literal null}
     * @return 包含 {@link RedisSentinelConfiguration} 的 Redis Sentinel 地址的 {@link RedisURI}
     */
    public static RedisURI sentinelConfigurationToRedisURI(RedisSentinelConfiguration sentinelConfiguration) {
        Assert.notNull(sentinelConfiguration, "RedisSentinelConfiguration is required");
        Set<RedisNode> sentinels = sentinelConfiguration.getSentinels();
        RedisPassword sentinelPassword = sentinelConfiguration.getSentinelPassword();
        RedisURI.Builder builder = RedisURI.builder();
        for (RedisNode sentinel : sentinels) {
            RedisURI.Builder sentinelBuilder = RedisURI.Builder.redis(sentinel.getHost(), sentinel.getPort());
            sentinelPassword.toOptional().ifPresent(sentinelBuilder::withPassword);
            builder.withSentinel(sentinelBuilder.build());
        }
        String username = sentinelConfiguration.getUsername();
        RedisPassword password = sentinelConfiguration.getPassword();
        if (StringUtils.hasText(username)) {
            // See https://github.com/lettuce-io/lettuce-core/issues/1404
            builder.withAuthentication(username, new String(password.toOptional().orElse(new char[0])));
        } else {
            password.toOptional().ifPresent(builder::withPassword);
        }
        builder.withSentinelMasterId(sentinelConfiguration.getMaster().getName());
        return builder.build();
    }

    /**
     * 将 {@link RedisURI} 转换为其对应的 {@link RedisStandaloneConfiguration}
     *
     * @param redisURI 包含 Redis 连接信息的 uri
     * @return {@link RedisStandaloneConfiguration} 表示 Redis URI 中的连接信息
     */
    static RedisStandaloneConfiguration createRedisStandaloneConfiguration(RedisURI redisURI) {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(redisURI.getHost());
        standaloneConfiguration.setPort(redisURI.getPort());
        standaloneConfiguration.setDatabase(redisURI.getDatabase());
        applyAuthentication(redisURI, standaloneConfiguration);
        return standaloneConfiguration;
    }

    /**
     * 将 {@link RedisURI} 转换为其对应的 {@link RedisSocketConfiguration}
     *
     * @param redisURI 包含使用本地 unix 域套接字的 Redis 连接信息的 uri
     * @return {@link RedisSocketConfiguration} 表示 Redis URI 中的连接信息
     */
    static RedisSocketConfiguration createRedisSocketConfiguration(RedisURI redisURI) {
        RedisSocketConfiguration socketConfiguration = new RedisSocketConfiguration();
        socketConfiguration.setSocket(redisURI.getSocket());
        socketConfiguration.setDatabase(redisURI.getDatabase());
        applyAuthentication(redisURI, socketConfiguration);
        return socketConfiguration;
    }

    /**
     * 将 {@link RedisURI} 转换为其对应的 {@link RedisSentinelConfiguration}
     *
     * @param redisURI 包含 Redis Sentinel 连接信息的 uri
     * @return {@link RedisSentinelConfiguration} 表示 Redis URI 中的 Redis Sentinel 信息
     */
    static RedisSentinelConfiguration createRedisSentinelConfiguration(RedisURI redisURI) {
        RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration();
        if (!ObjectUtils.isEmpty(redisURI.getSentinelMasterId())) {
            sentinelConfiguration.setMaster(redisURI.getSentinelMasterId());
        }
        sentinelConfiguration.setDatabase(redisURI.getDatabase());
        for (RedisURI sentinelNodeRedisUri : redisURI.getSentinels()) {
            RedisNode sentinelNode = new RedisNode(sentinelNodeRedisUri.getHost(), sentinelNodeRedisUri.getPort());
            if (sentinelNodeRedisUri.getPassword() != null) {
                sentinelConfiguration.setSentinelPassword(sentinelNodeRedisUri.getPassword());
            }
            sentinelConfiguration.addSentinel(sentinelNode);
        }
        applyAuthentication(redisURI, sentinelConfiguration);
        return sentinelConfiguration;
    }

    private static void applyAuthentication(RedisURI redisURI, RedisConfiguration.WithAuthentication redisConfiguration) {
        if (StringUtils.hasText(redisURI.getUsername())) {
            redisConfiguration.setUsername(redisURI.getUsername());
        }
        if (redisURI.getPassword() != null) {
            redisConfiguration.setPassword(redisURI.getPassword());
        }
    }

    public static byte[] toBytes(String source) {
        if (source == null) {
            return null;
        }
        return source.getBytes();
    }

    public static byte[] toBytes(Integer source) {
        return String.valueOf(source).getBytes();
    }

    public static byte[] toBytes(Long source) {
        return String.valueOf(source).getBytes();
    }

    public static byte[] toBytes(Double source) {
        return toBytes(String.valueOf(source));
    }

    private static String boundaryToBytes(Boundary boundary, byte[] inclPrefix, byte[] exclPrefix) {
        byte[] prefix = boundary.isIncluding() ? inclPrefix : exclPrefix;
        byte[] value;
        if (boundary.getValue() instanceof byte[]) {
            value = (byte[]) boundary.getValue();
        } else if (boundary.getValue() instanceof Double) {
            value = toBytes((Double) boundary.getValue());
        } else if (boundary.getValue() instanceof Long) {
            value = toBytes((Long) boundary.getValue());
        } else if (boundary.getValue() instanceof Integer) {
            value = toBytes((Integer) boundary.getValue());
        } else if (boundary.getValue() instanceof String) {
            value = toBytes((String) boundary.getValue());
        } else {
            throw new IllegalArgumentException(String.format("Cannot convert %s to binary format", boundary.getValue()));
        }
        ByteBuffer buffer = ByteBuffer.allocate(prefix.length + value.length);
        buffer.put(prefix);
        buffer.put(value);
        return toString(ByteUtils.getBytes(buffer));
    }

    public static List<RedisClusterNode> partitionsToClusterNodes(Partitions source) {
        if (source == null) {
            return Collections.emptyList();
        }
        List<RedisClusterNode> nodes = new ArrayList<>();
        for (io.lettuce.core.cluster.models.partitions.RedisClusterNode node : source) {
            nodes.add(toRedisClusterNode(node));
        }
        return nodes;
    }

    public static RedisClusterNode toRedisClusterNode(io.lettuce.core.cluster.models.partitions.RedisClusterNode source) {
        Set<Flag> flags = parseFlags(source.getFlags());
        return RedisClusterNode.newRedisClusterNode()
                .listeningAt(source.getUri().getHost(), source.getUri().getPort())
                .withId(source.getNodeId()).promotedAs(flags.contains(Flag.MASTER) ? NodeType.MASTER : NodeType.SLAVE)
                .serving(new SlotRange(source.getSlots())).withFlags(flags)
                .linkState(source.isConnected() ? LinkState.CONNECTED : LinkState.DISCONNECTED).slaveOf(source.getSlaveOf())
                .build();
    }

    private static Set<Flag> parseFlags(Set<NodeFlag> source) {
        Set<Flag> flags = new LinkedHashSet<>(source != null ? source.size() : 8, 1);
        assert source != null;
        for (NodeFlag flag : source) {
            switch (flag) {
                case NOFLAGS:
                    flags.add(Flag.NOFLAGS);
                    break;
                case EVENTUAL_FAIL:
                    flags.add(Flag.PFAIL);
                    break;
                case FAIL:
                    flags.add(Flag.FAIL);
                    break;
                case HANDSHAKE:
                    flags.add(Flag.HANDSHAKE);
                    break;
                case UPSTREAM:
                    flags.add(Flag.MASTER);
                    break;
                case MYSELF:
                    flags.add(Flag.MYSELF);
                    break;
                case NOADDR:
                    flags.add(Flag.NOADDR);
                    break;
                case REPLICA:
                    flags.add(Flag.SLAVE);
                    break;
            }
        }
        return flags;
    }

    /**
     * 将给定的 {@link Expiration} 和 {@link SetOption} 转换为相应的 {@link SetArgs} <br />
     *
     * @param expiration 可以是 {@literal null}。
     * @param option     可以是 {@literal null}。
     */
    public static SetArgs toSetArgs(Expiration expiration, SetOption option) {
        SetArgs args = new SetArgs();
        if (expiration != null) {

            if (expiration.isKeepTtl()) {
                args.keepttl();
            } else if (!expiration.isPersistent()) {
                // noinspection SwitchStatementWithTooFewBranches
                switch (expiration.getTimeUnit()) {
                    case MILLISECONDS:
                        if (expiration.isUnixTimestamp()) {
                            args.pxAt(expiration.getConverted(TimeUnit.MILLISECONDS));
                        } else {
                            args.px(expiration.getConverted(TimeUnit.MILLISECONDS));
                        }
                        break;
                    default:
                        if (expiration.isUnixTimestamp()) {
                            args.exAt(expiration.getConverted(TimeUnit.SECONDS));
                        } else {
                            args.ex(expiration.getConverted(TimeUnit.SECONDS));
                        }
                        break;
                }
            }
        }
        if (option != null) {
            switch (option) {
                case SET_IF_ABSENT:
                    args.nx();
                    break;
                case SET_IF_PRESENT:
                    args.xx();
                    break;
                default:
                    break;
            }
        }
        return args;
    }

    /**
     * 将 {@link Expiration} 转换为 {@link GetExArgs}
     *
     * @param expiration 可以是 {@literal null}。
     */
    static GetExArgs toGetExArgs(Expiration expiration) {
        GetExArgs args = new GetExArgs();
        if (expiration == null) {
            return args;
        }
        if (expiration.isPersistent()) {
            return args.persist();
        }
        if (expiration.getTimeUnit() == TimeUnit.MILLISECONDS) {
            if (expiration.isUnixTimestamp()) {
                return args.pxAt(expiration.getExpirationTime());
            }
            return args.px(expiration.getExpirationTime());
        }
        return expiration.isUnixTimestamp() ? args.exAt(expiration.getConverted(TimeUnit.SECONDS)) : args.ex(expiration.getConverted(TimeUnit.SECONDS));
    }

    static Converter<List<byte[]>, Long> toTimeConverter(TimeUnit timeUnit) {
        return source -> {
            Assert.notEmpty(source, "Received invalid result from server. Expected 2 items in collection.");
            Assert.isTrue(source.size() == 2, "Received invalid nr of arguments from redis server. Expected 2 received " + source.size());
            return toTimeMillis(toString(source.get(0)), toString(source.get(1)), timeUnit);
        };
    }

    /**
     * 将 {@link Metric} 转换为 {@link GeoArgs.Unit}
     */
    public static GeoArgs.Unit toGeoArgsUnit(Metric metric) {
        Metric metricToUse = metric == null || ObjectUtils.nullSafeEquals(Metrics.NEUTRAL, metric) ? DistanceUnit.METERS : metric;
        return ObjectUtils.caseInsensitiveValueOf(GeoArgs.Unit.values(), metricToUse.getAbbreviation());
    }

    /**
     * 将 {@link GeoRadiusCommandArgs} 转换为 {@link GeoArgs}
     */
    public static GeoArgs toGeoArgs(GeoRadiusCommandArgs args) {
        return toGeoArgs((GeoCommandArgs) args);
    }

    /**
     * 将 {@link GeoCommandArgs} 转换为 {@link GeoArgs}
     */
    public static GeoArgs toGeoArgs(GeoCommandArgs args) {
        GeoArgs geoArgs = new GeoArgs();
        if (args.hasFlags()) {
            for (GeoCommandArgs.GeoCommandFlag flag : args.getFlags()) {
                if (flag.equals(GeoRadiusCommandArgs.Flag.WITHCOORD)) {
                    geoArgs.withCoordinates();
                } else if (flag.equals(GeoRadiusCommandArgs.Flag.WITHDIST)) {
                    geoArgs.withDistance();
                }
            }
        }
        if (args.hasSortDirection()) {
            switch (args.getSortDirection()) {
                case ASC:
                    geoArgs.asc();
                    break;
                case DESC:
                    geoArgs.desc();
                    break;
            }
        }
        if (args.hasLimit()) {
            geoArgs.withCount(args.getLimit(), args.getFlags().contains(GeoRadiusCommandArgs.Flag.ANY));
        }
        return geoArgs;
    }

    /**
     * 将 {@link BitFieldSubCommands} 转换为 {@link BitFieldArgs}
     */
    public static BitFieldArgs toBitFieldArgs(BitFieldSubCommands subCommands) {
        BitFieldArgs args = new BitFieldArgs();
        for (BitFieldSubCommand subCommand : subCommands) {
            BitFieldArgs.BitFieldType bft = subCommand.getType().isSigned() ? BitFieldArgs.signed(subCommand.getType().getBits()) : BitFieldArgs.unsigned(subCommand.getType().getBits());
            BitFieldArgs.Offset offset;
            if (subCommand.getOffset().isZeroBased()) {
                offset = BitFieldArgs.offset((int) subCommand.getOffset().getValue());
            } else {
                offset = BitFieldArgs.typeWidthBasedOffset((int) subCommand.getOffset().getValue());
            }
            if (subCommand instanceof BitFieldGet) {
                args = args.get(bft, offset);
            } else if (subCommand instanceof BitFieldSet) {
                args = args.set(bft, offset, ((BitFieldSet) subCommand).getValue());
            } else if (subCommand instanceof BitFieldIncrBy) {
                BitFieldIncrBy.Overflow overflow = ((BitFieldIncrBy) subCommand).getOverflow();
                if (overflow != null) {
                    BitFieldArgs.OverflowType type;
                    switch (overflow) {
                        case SAT:
                            type = BitFieldArgs.OverflowType.SAT;
                            break;
                        case FAIL:
                            type = BitFieldArgs.OverflowType.FAIL;
                            break;
                        case WRAP:
                            type = BitFieldArgs.OverflowType.WRAP;
                            break;
                        default:
                            throw new IllegalArgumentException(String.format(
                                    "Invalid OVERFLOW. Expected one the following %s but got %s.",
                                    Arrays.toString(Overflow.values()), overflow
                            ));
                    }
                    args = args.overflow(type);
                }
                args = args.incrBy(bft, (int) subCommand.getOffset().getValue(), ((BitFieldIncrBy) subCommand).getValue());
            }
        }
        return args;
    }

    /**
     * 将 {@link ScanOptions} 转换为 {@link ScanArgs}
     *
     * @param options 要转换的 {@link ScanOptions}，可能是 {@literal null}
     * @return 转换后的 {@link ScanArgs}。如果 {@link ScanOptions} 为 {@literal null}，则返回 {@literal null}
     */
    static ScanArgs toScanArgs(ScanOptions options) {
        if (options == null) {
            return null;
        }
        KeyScanArgs scanArgs = new KeyScanArgs();
        byte[] pattern = options.getBytePattern();
        if (pattern != null) {
            scanArgs.match(pattern);
        }
        if (options.getCount() != null) {
            scanArgs.limit(options.getCount());
        }
        if (options instanceof KeyScanOptions) {
            scanArgs.type(((KeyScanOptions) options).getType());
        }
        return scanArgs;
    }

    /**
     * 获取能够将 {@link Byte} 的 {@link Set} 转换为 {@link GeoResults} 的 {@link Converter}
     */
    public static Converter<Set<byte[]>, GeoResults<GeoLocation<byte[]>>> bytesSetToGeoResultsConverter() {
        return source -> {
            if (CollectionUtils.isEmpty(source)) {
                return new GeoResults<>(Collections.<GeoResult<GeoLocation<byte[]>>>emptyList());
            }
            List<GeoResult<GeoLocation<byte[]>>> results = new ArrayList<>(source.size());
            for (byte[] bytes : source) {
                results.add(new GeoResult<>(new GeoLocation<>(bytes, null), new Distance(0D)));
            }
            return new GeoResults<>(results);
        };
    }

    /**
     * 获取能够将 {@link GeoWithin} 转换为 {@link GeoResults} 的 {@link Converter}
     */
    public static Converter<List<GeoWithin<byte[]>>, GeoResults<GeoLocation<byte[]>>> geoRadiusResponseToGeoResultsConverter(Metric metric) {
        return GeoResultsConverterFactory.INSTANCE.forMetric(metric);
    }

    public static Converter<TransactionResult, List<Object>> transactionResultUnwrapper() {
        return transactionResult -> transactionResult.stream().collect(Collectors.toList());
    }

    /**
     * 从 {@link Range} 返回 {@link Optional} 下界
     */
    static <T extends Comparable<T>> Optional<T> getLowerBound(org.clever.data.domain.Range<T> range) {
        return range.getLowerBound().getValue();
    }

    /**
     * 从 {@link Range} 返回 {@link Optional} 上限
     */
    static <T extends Comparable<T>> Optional<T> getUpperBound(org.clever.data.domain.Range<T> range) {
        return range.getUpperBound().getValue();
    }

    /**
     * 如果下限未绑定为指向第一个元素，则从 {@link Range} 或 {@literal 0} （零）返回下限索引。
     * 与基于索引的命令一起使用，例如 {@code LRANGE}、{@code GETRANGE}。
     *
     * @return 第一个元素的索引下限值或 {@literal 0} 如果没有限制
     */
    static long getLowerBoundIndex(org.clever.data.domain.Range<Long> range) {
        return getLowerBound(range).orElse(INDEXED_RANGE_START);
    }

    /**
     * 如果上限没有指向最后一个元素，则返回 {@link Range} 或 {@literal -1} （减一）的上限索引。
     * 与基于索引的命令一起使用，例如 {@code LRANGE}、{@code GETRANGE}。
     *
     * @return 最后一个元素的索引上限值或 {@literal -1} 如果没有限制
     */
    static long getUpperBoundIndex(org.clever.data.domain.Range<Long> range) {
        return getUpperBound(range).orElse(INDEXED_RANGE_END);
    }

    static LMoveArgs toLmoveArgs(Enum<?> from, Enum<?> to) {
        if (from.name().equals(Direction.LEFT.name())) {
            if (to.name().equals(Direction.LEFT.name())) {
                return LMoveArgs.Builder.leftLeft();
            }
            return LMoveArgs.Builder.leftRight();
        }
        if (to.name().equals(Direction.LEFT.name())) {
            return LMoveArgs.Builder.rightLeft();
        }
        return LMoveArgs.Builder.rightRight();
    }

    static GeoSearch.GeoPredicate toGeoPredicate(GeoShape predicate) {
        if (predicate instanceof RadiusShape) {
            Distance radius = ((RadiusShape) predicate).getRadius();
            return GeoSearch.byRadius(radius.getValue(), toGeoArgsUnit(radius.getMetric()));
        }
        if (predicate instanceof BoxShape) {
            BoxShape boxPredicate = (BoxShape) predicate;
            BoundingBox boundingBox = boxPredicate.getBoundingBox();
            return GeoSearch.byBox(boundingBox.getWidth().getValue(), boundingBox.getHeight().getValue(), toGeoArgsUnit(boxPredicate.getMetric()));
        }
        throw new IllegalArgumentException(String.format("Cannot convert %s to Lettuce GeoPredicate", predicate));
    }

    static <T> GeoSearch.GeoRef<T> toGeoRef(GeoReference<T> reference) {
        if (reference instanceof GeoReference.GeoMemberReference) {
            return GeoSearch.fromMember(((GeoMemberReference<T>) reference).getMember());
        }
        if (reference instanceof GeoReference.GeoCoordinateReference) {
            GeoCoordinateReference<?> coordinates = (GeoCoordinateReference<?>) reference;
            return GeoSearch.fromCoordinates(coordinates.getLongitude(), coordinates.getLatitude());
        }
        throw new IllegalArgumentException(String.format("Cannot convert %s to Lettuce GeoRef", reference));
    }

    enum GeoResultsConverterFactory {
        INSTANCE;

        Converter<List<GeoWithin<byte[]>>, GeoResults<GeoLocation<byte[]>>> forMetric(Metric metric) {
            return new GeoResultsConverter(metric == null || ObjectUtils.nullSafeEquals(Metrics.NEUTRAL, metric) ? DistanceUnit.METERS : metric);
        }

        private static class GeoResultsConverter implements Converter<List<GeoWithin<byte[]>>, GeoResults<GeoLocation<byte[]>>> {
            private final Metric metric;

            public GeoResultsConverter(Metric metric) {
                this.metric = metric;
            }

            @Override
            public GeoResults<GeoLocation<byte[]>> convert(List<GeoWithin<byte[]>> source) {
                List<GeoResult<GeoLocation<byte[]>>> results = new ArrayList<>(source.size());
                Converter<GeoWithin<byte[]>, GeoResult<GeoLocation<byte[]>>> converter = GeoResultConverterFactory.INSTANCE.forMetric(metric);
                for (GeoWithin<byte[]> result : source) {
                    results.add(converter.convert(result));
                }
                return new GeoResults<>(results, metric);
            }
        }
    }

    enum GeoResultConverterFactory {
        INSTANCE;

        Converter<GeoWithin<byte[]>, GeoResult<GeoLocation<byte[]>>> forMetric(Metric metric) {
            return new GeoResultConverter(metric);
        }

        private static class GeoResultConverter implements Converter<GeoWithin<byte[]>, GeoResult<GeoLocation<byte[]>>> {
            private final Metric metric;

            public GeoResultConverter(Metric metric) {
                this.metric = metric;
            }

            @Override
            public GeoResult<GeoLocation<byte[]>> convert(GeoWithin<byte[]> source) {
                Point point = geoCoordinatesToPoint(source.getCoordinates());
                return new GeoResult<>(
                        new GeoLocation<>(source.getMember(), point),
                        new Distance(source.getDistance() != null ? source.getDistance() : 0D, metric)
                );
            }
        }
    }
}
