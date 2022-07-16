package org.clever.boot.env;

import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.MutablePropertySources;
import org.clever.core.env.PropertySource;
import org.clever.core.env.StandardEnvironment;
import org.clever.util.Assert;
import org.clever.util.DigestUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 * {@link PropertySource}，为任何以{@literal "random."}开头的属性返回随机值。
 * 其中，“非限定属性名”是请求的属性名中“random.”之外的部分前缀，此{@link PropertySource}返回：
 * <ul>
 * <li>当 {@literal "int"}, 随机的 {@link Integer} 值,受可选指定范围的限制。</li>
 * <li>当 {@literal "long"}, 随机的 {@link Long} 值,受可选指定范围的限制。</li>
 * <li>当 {@literal "uuid"}, 随机的 {@link UUID} 值.</li>
 * <li>否则 {@code byte[]}.</li>
 * </ul>
 * <pre>{@code
 * ${random.value}          - 类似uuid的随机数，没有"-"连接
 * ${random.int}            - 随机取整型范围内的一个值
 * ${random.long}           - 随机取长整型范围内的一个值
 * ${random.long(100,200)}  - 随机生成长整型100-200范围内的一个值
 * ${random.uuid}           - 生成一个uuid，有短杠连接
 * ${random.int(10)}        - 随机生成一个10以内的数
 * ${random.int(100,200)}   - 随机生成一个100-200 范围以内的数
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:27 <br/>
 */
public class RandomValuePropertySource extends PropertySource<Random> {
    /**
     * 随机变量的名称 {@link PropertySource}.
     */
    public static final String RANDOM_PROPERTY_SOURCE_NAME = "random";
    private static final String PREFIX = "random.";
    private static final Logger logger = LoggerFactory.getLogger(RandomValuePropertySource.class);

    public RandomValuePropertySource() {
        this(RANDOM_PROPERTY_SOURCE_NAME);
    }

    public RandomValuePropertySource(String name) {
        super(name, new Random());
    }

    @Override
    public Object getProperty(String name) {
        if (!name.startsWith(PREFIX)) {
            return null;
        }
        logger.trace(String.format("Generating random property for '%s'", name).toString());
        return getRandomValue(name.substring(PREFIX.length()));
    }

    private Object getRandomValue(String type) {
        if (type.equals("int")) {
            return getSource().nextInt();
        }
        if (type.equals("long")) {
            return getSource().nextLong();
        }
        String range = getRange(type, "int");
        if (range != null) {
            return getNextIntInRange(Range.of(range, Integer::parseInt));
        }
        range = getRange(type, "long");
        if (range != null) {
            return getNextLongInRange(Range.of(range, Long::parseLong));
        }
        if (type.equals("uuid")) {
            return UUID.randomUUID().toString();
        }
        return getRandomBytes();
    }

    private String getRange(String type, String prefix) {
        if (type.startsWith(prefix)) {
            int startIndex = prefix.length() + 1;
            if (type.length() > startIndex) {
                return type.substring(startIndex, type.length() - 1);
            }
        }
        return null;
    }

    private int getNextIntInRange(Range<Integer> range) {
        OptionalInt first = getSource().ints(1, range.getMin(), range.getMax()).findFirst();
        assertPresent(first.isPresent(), range);
        return first.getAsInt();
    }

    private long getNextLongInRange(Range<Long> range) {
        OptionalLong first = getSource().longs(1, range.getMin(), range.getMax()).findFirst();
        assertPresent(first.isPresent(), range);
        return first.getAsLong();
    }

    private void assertPresent(boolean present, Range<?> range) {
        Assert.state(present, () -> "Could not get random number for range '" + range + "'");
    }

    private Object getRandomBytes() {
        byte[] bytes = new byte[32];
        getSource().nextBytes(bytes);
        return DigestUtils.md5DigestAsHex(bytes);
    }

    public static void addToEnvironment(ConfigurableEnvironment environment) {
        addToEnvironment(environment, logger);
    }

    public static void addToEnvironment(ConfigurableEnvironment environment, Logger logger) {
        MutablePropertySources sources = environment.getPropertySources();
        PropertySource<?> existing = sources.get(RANDOM_PROPERTY_SOURCE_NAME);
        if (existing != null) {
            logger.trace("RandomValuePropertySource already present");
            return;
        }
        RandomValuePropertySource randomSource = new RandomValuePropertySource(RANDOM_PROPERTY_SOURCE_NAME);
        if (sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) != null) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, randomSource);
        } else {
            sources.addLast(randomSource);
        }
        logger.trace("RandomValuePropertySource add to Environment");
    }

    static final class Range<T extends Number> {
        private final String value;
        private final T min;
        private final T max;

        private Range(String value, T min, T max) {
            this.value = value;
            this.min = min;
            this.max = max;
        }

        T getMin() {
            return this.min;
        }

        T getMax() {
            return this.max;
        }

        @Override
        public String toString() {
            return this.value;
        }

        static <T extends Number & Comparable<T>> Range<T> of(String value, Function<String, T> parse) {
            T zero = parse.apply("0");
            String[] tokens = StringUtils.commaDelimitedListToStringArray(value);
            T min = parse.apply(tokens[0]);
            if (tokens.length == 1) {
                Assert.isTrue(min.compareTo(zero) > 0, "Bound must be positive.");
                return new Range<>(value, zero, min);
            }
            T max = parse.apply(tokens[1]);
            Assert.isTrue(min.compareTo(max) < 0, "Lower bound must be less than upper bound.");
            return new Range<>(value, min, max);
        }
    }
}
