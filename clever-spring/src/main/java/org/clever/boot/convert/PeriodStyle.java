package org.clever.boot.convert;

import org.clever.util.Assert;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Period}单位的标准集。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:47 <br/>
 *
 * @see Period
 */
public enum PeriodStyle {
    /**
     * 简单的格式设置,例如 '1d'.
     */
    SIMPLE("^" + "(?:([-+]?[0-9]+)Y)?" + "(?:([-+]?[0-9]+)M)?" + "(?:([-+]?[0-9]+)W)?" + "(?:([-+]?[0-9]+)D)?" + "$", Pattern.CASE_INSENSITIVE) {
        @Override
        public Period parse(String value, ChronoUnit unit) {
            try {
                if (NUMERIC.matcher(value).matches()) {
                    return Unit.fromChronoUnit(unit).parse(value);
                }
                Matcher matcher = matcher(value);
                Assert.state(matcher.matches(), "Does not match simple period pattern");
                Assert.isTrue(hasAtLeastOneGroupValue(matcher), () -> "'" + value + "' is not a valid simple period");
                int years = parseInt(matcher, 1);
                int months = parseInt(matcher, 2);
                int weeks = parseInt(matcher, 3);
                int days = parseInt(matcher, 4);
                return Period.of(years, months, Math.addExact(Math.multiplyExact(weeks, 7), days));
            } catch (Exception ex) {
                throw new IllegalArgumentException("'" + value + "' is not a valid simple period", ex);
            }
        }

        boolean hasAtLeastOneGroupValue(Matcher matcher) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                if (matcher.group(i + 1) != null) {
                    return true;
                }
            }
            return false;
        }

        private int parseInt(Matcher matcher, int group) {
            String value = matcher.group(group);
            return (value != null) ? Integer.parseInt(value) : 0;
        }

        @Override
        protected boolean matches(String value) {
            return NUMERIC.matcher(value).matches() || matcher(value).matches();
        }

        @Override
        public String print(Period value, ChronoUnit unit) {
            if (value.isZero()) {
                return Unit.fromChronoUnit(unit).print(value);
            }
            StringBuilder result = new StringBuilder();
            append(result, value, Unit.YEARS);
            append(result, value, Unit.MONTHS);
            append(result, value, Unit.DAYS);
            return result.toString();
        }

        private void append(StringBuilder result, Period value, Unit unit) {
            if (!unit.isZero(value)) {
                result.append(unit.print(value));
            }
        }
    },

    /**
     * ISO-8601 格式化.
     */
    ISO8601("^[+-]?P.*$", 0) {
        @Override
        public Period parse(String value, ChronoUnit unit) {
            try {
                return Period.parse(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 period", ex);
            }
        }

        @Override
        public String print(Period value, ChronoUnit unit) {
            return value.toString();
        }

    };

    private static final Pattern NUMERIC = Pattern.compile("^[-+]?\\d+$");

    private final Pattern pattern;

    PeriodStyle(String pattern, int flags) {
        this.pattern = Pattern.compile(pattern, flags);
    }

    protected boolean matches(String value) {
        return this.pattern.matcher(value).matches();
    }

    protected final Matcher matcher(String value) {
        return this.pattern.matcher(value);
    }

    /**
     * 将给定值解析为 Period.
     *
     * @param value 要分析的值
     * @return period
     */
    public Period parse(String value) {
        return parse(value, null);
    }

    /**
     * 将给定值解析为 Period
     *
     * @param value 要分析的值
     * @param unit  如果值未指定周期单位，则使用周期单位（null将默认为d）
     * @return period
     */
    public abstract Period parse(String value, ChronoUnit unit);

    /**
     * 打印指定的期间。
     *
     * @param value 要打印的值
     * @return 打印结果
     */
    public String print(Period value) {
        return print(value, null);
    }

    /**
     * 使用给定的单位打印指定的周期。
     *
     * @param value 要打印的值
     * @param unit  用于打印的值
     * @return 打印结果
     */
    public abstract String print(Period value, ChronoUnit unit);

    /**
     * 检测样式，然后解析值以返回 period.
     *
     * @param value 要分析的值
     * @return 已解析的 period
     * @throws IllegalArgumentException 如果值不是已知样式或无法分析
     */
    public static Period detectAndParse(String value) {
        return detectAndParse(value, null);
    }

    /**
     * 检测样式，然后解析值以返回 period.
     *
     * @param value 要分析的值
     * @param unit  如果该值未指定周期单位，则使用的周期单位（null将默认为ms）
     * @return 已解析的 period
     * @throws IllegalArgumentException 如果值不是已知样式或无法分析
     */
    public static Period detectAndParse(String value, ChronoUnit unit) {
        return detect(value).parse(value, unit);
    }

    /**
     * 从给定的源值检测样式。
     *
     * @param value 源值
     * @return period style
     * @throws IllegalArgumentException 如果值不是已知样式
     */
    public static PeriodStyle detect(String value) {
        Assert.notNull(value, "Value must not be null");
        for (PeriodStyle candidate : values()) {
            if (candidate.matches(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("'" + value + "' is not a valid period");
    }

    private enum Unit {
        /**
         * 天，由后缀表示 {@code d}.
         */
        DAYS(ChronoUnit.DAYS, "d", Period::getDays, Period::ofDays),
        /**
         * 周，由后缀表示 {@code w}.
         */
        WEEKS(ChronoUnit.WEEKS, "w", null, Period::ofWeeks),
        /**
         * 月份，由后缀表示 {@code m}.
         */
        MONTHS(ChronoUnit.MONTHS, "m", Period::getMonths, Period::ofMonths),
        /**
         * 年份，由后缀表示 {@code y}.
         */
        YEARS(ChronoUnit.YEARS, "y", Period::getYears, Period::ofYears);

        private final ChronoUnit chronoUnit;
        private final String suffix;
        private final Function<Period, Integer> intValue;
        private final Function<Integer, Period> factory;

        Unit(ChronoUnit chronoUnit, String suffix, Function<Period, Integer> intValue, Function<Integer, Period> factory) {
            this.chronoUnit = chronoUnit;
            this.suffix = suffix;
            this.intValue = intValue;
            this.factory = factory;
        }

        private Period parse(String value) {
            return this.factory.apply(Integer.parseInt(value));
        }

        private String print(Period value) {
            return intValue(value) + this.suffix;
        }

        private boolean isZero(Period value) {
            return intValue(value) == 0;
        }

        private int intValue(Period value) {
            Assert.notNull(this.intValue, () -> "intValue cannot be extracted from " + this.name());
            return this.intValue.apply(value);
        }

        private static Unit fromChronoUnit(ChronoUnit chronoUnit) {
            if (chronoUnit == null) {
                return Unit.DAYS;
            }
            for (Unit candidate : values()) {
                if (candidate.chronoUnit == chronoUnit) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unsupported unit " + chronoUnit);
        }
    }
}
