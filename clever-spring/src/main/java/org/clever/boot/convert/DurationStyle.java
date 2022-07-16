package org.clever.boot.convert;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 持续时间格式样式。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:38 <br/>
 */
public enum DurationStyle {
    /**
     * 简单的格式设置，例如 '1s'.
     */
    SIMPLE("^([+-]?\\d+)([a-zA-Z]{0,2})$") {
        @Override
        public Duration parse(String value, ChronoUnit unit) {
            try {
                Matcher matcher = matcher(value);
                Assert.state(matcher.matches(), "Does not match simple duration pattern");
                String suffix = matcher.group(2);
                return (StringUtils.hasLength(suffix) ? Unit.fromSuffix(suffix) : Unit.fromChronoUnit(unit))
                        .parse(matcher.group(1));
            } catch (Exception ex) {
                throw new IllegalArgumentException("'" + value + "' is not a valid simple duration", ex);
            }
        }

        @Override
        public String print(Duration value, ChronoUnit unit) {
            return Unit.fromChronoUnit(unit).print(value);
        }
    },

    /**
     * ISO-8601 格式化.
     */
    ISO8601("^[+-]?P.*$") {
        @Override
        public Duration parse(String value, ChronoUnit unit) {
            try {
                return Duration.parse(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 duration", ex);
            }
        }

        @Override
        public String print(Duration value, ChronoUnit unit) {
            return value.toString();
        }
    };

    private final Pattern pattern;

    DurationStyle(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    protected final boolean matches(String value) {
        return this.pattern.matcher(value).matches();
    }

    protected final Matcher matcher(String value) {
        return this.pattern.matcher(value);
    }

    /**
     * 将给定值解析为持续时间。
     *
     * @param value 要分析的值
     * @return 持续时间
     */
    public Duration parse(String value) {
        return parse(value, null);
    }

    /**
     * 将给定值解析为持续时间。
     *
     * @param value 要分析的值
     * @param unit  如果该值未指定持续时间单位，则使用的持续时间单位（null默认为ms）
     * @return 持续时间
     */
    public abstract Duration parse(String value, ChronoUnit unit);

    /**
     * 打印指定的持续时间。
     *
     * @param value 要打印的值
     * @return 打印结果
     */
    public String print(Duration value) {
        return print(value, null);
    }

    /**
     * 使用给定的单位打印指定的持续时间。
     *
     * @param value 要打印的值
     * @param unit  用于打印的值
     * @return 打印结果
     */
    public abstract String print(Duration value, ChronoUnit unit);

    /**
     * 检测样式，然后解析值以返回持续时间。
     *
     * @param value 要分析的值
     * @return 解析的持续时间
     * @throws IllegalArgumentException 如果值不是已知样式或无法分析
     */
    public static Duration detectAndParse(String value) {
        return detectAndParse(value, null);
    }

    /**
     * 检测样式，然后解析值以返回持续时间。
     *
     * @param value 要分析的值
     * @param unit  如果该值未指定持续时间单位，则使用的持续时间单位（null默认为ms）
     * @return 解析的持续时间
     * @throws IllegalArgumentException 如果值不是已知样式或无法分析
     */
    public static Duration detectAndParse(String value, ChronoUnit unit) {
        return detect(value).parse(value, unit);
    }

    /**
     * 从给定的源值检测样式。
     *
     * @param value 源值
     * @return 持续时间样式
     * @throws IllegalArgumentException 如果值不是已知样式
     */
    public static DurationStyle detect(String value) {
        Assert.notNull(value, "Value must not be null");
        for (DurationStyle candidate : values()) {
            if (candidate.matches(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("'" + value + "' is not a valid duration");
    }

    /**
     * 我们支持的单位。
     */
    enum Unit {
        /**
         * 纳秒。
         */
        NANOS(ChronoUnit.NANOS, "ns", Duration::toNanos),
        /**
         * 微秒。
         */
        MICROS(ChronoUnit.MICROS, "us", (duration) -> duration.toNanos() / 1000L),
        /**
         * 毫秒。
         */
        MILLIS(ChronoUnit.MILLIS, "ms", Duration::toMillis),
        /**
         * 秒。
         */
        SECONDS(ChronoUnit.SECONDS, "s", Duration::getSeconds),
        /**
         * 分钟
         */
        MINUTES(ChronoUnit.MINUTES, "m", Duration::toMinutes),
        /**
         * 小时。
         */
        HOURS(ChronoUnit.HOURS, "h", Duration::toHours),
        /**
         * 天。
         */
        DAYS(ChronoUnit.DAYS, "d", Duration::toDays);

        private final ChronoUnit chronoUnit;
        private final String suffix;
        private final Function<Duration, Long> longValue;

        Unit(ChronoUnit chronoUnit, String suffix, Function<Duration, Long> toUnit) {
            this.chronoUnit = chronoUnit;
            this.suffix = suffix;
            this.longValue = toUnit;
        }

        public Duration parse(String value) {
            return Duration.of(Long.parseLong(value), this.chronoUnit);
        }

        public String print(Duration value) {
            return longValue(value) + this.suffix;
        }

        public long longValue(Duration value) {
            return this.longValue.apply(value);
        }

        public static Unit fromChronoUnit(ChronoUnit chronoUnit) {
            if (chronoUnit == null) {
                return Unit.MILLIS;
            }
            for (Unit candidate : values()) {
                if (candidate.chronoUnit == chronoUnit) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unknown unit " + chronoUnit);
        }

        public static Unit fromSuffix(String suffix) {
            for (Unit candidate : values()) {
                if (candidate.suffix.equalsIgnoreCase(suffix)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
        }
    }
}
