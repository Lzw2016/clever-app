package org.clever.data.domain;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.Optional;

/**
 * 用于处理范围和边界的简单值对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:20 <br/>
 */
public final class Range<T extends Comparable<T>> {
    private final static Range<?> UNBOUNDED = Range.of(Bound.unbounded(), Bound.UNBOUNDED);
    /**
     * 范围的下限
     */
    private final Bound<T> lowerBound;
    /**
     * 范围的上限
     */
    private final Bound<T> upperBound;

    private Range(Bound<T> lowerBound, Bound<T> upperBound) {
        Assert.notNull(lowerBound, "Lower bound must not be null");
        Assert.notNull(upperBound, "Upper bound must not be null");
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * 返回一个无界的 {@link Range}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Range<T> unbounded() {
        return (Range<T>) UNBOUNDED;
    }

    /**
     * 为两个值创建一个包含边界的新 {@link Range}
     *
     * @param from 不得为 {@literal null}
     * @param to   不得为 {@literal null}
     */
    public static <T extends Comparable<T>> Range<T> closed(T from, T to) {
        return new Range<>(Bound.inclusive(from), Bound.inclusive(to));
    }

    /**
     * 创建一个新的 {@link Range}，两个值都具有排他性边界
     *
     * @param from 不得为 {@literal null}
     * @param to   不得为 {@literal null}
     */
    public static <T extends Comparable<T>> Range<T> open(T from, T to) {
        return new Range<>(Bound.exclusive(from), Bound.exclusive(to));
    }

    /**
     * 创建一个新的左开 {@link Range}，即左独占，右包含
     *
     * @param from 不得为 {@literal null}
     * @param to   不得为 {@literal null}
     */
    public static <T extends Comparable<T>> Range<T> leftOpen(T from, T to) {
        return new Range<>(Bound.exclusive(from), Bound.inclusive(to));
    }

    /**
     * 创建一个新的右开 {@link Range}，即左包容，右独占
     *
     * @param from 不得为 {@literal null}
     * @param to   不得为 {@literal null}
     */
    public static <T extends Comparable<T>> Range<T> rightOpen(T from, T to) {
        return new Range<>(Bound.inclusive(from), Bound.exclusive(to));
    }

    /**
     * 使用给定的右边界创建一个左无边界的 {@link Range}（左边界设置为 {@link Bound#unbounded()}）
     *
     * @param to the right {@link Bound}, 不得为 {@literal null}
     */
    public static <T extends Comparable<T>> Range<T> leftUnbounded(Bound<T> to) {
        return new Range<>(Bound.unbounded(), to);
    }

    /**
     * 使用给定的左边界创建右无边界的 {@link Range}（右边界设置为 {@link Bound#unbounded()}）
     *
     * @param from 左边的 {@link Bound}，不得为 {@literal null}
     */
    public static <T extends Comparable<T>> Range<T> rightUnbounded(Bound<T> from) {
        return new Range<>(from, Bound.unbounded());
    }

    /**
     * 给定较低的 {@link Bound} 创建一个 {@link RangeBuilder}
     *
     * @param lower 不得为 {@literal null}
     */
    public static <T extends Comparable<T>> RangeBuilder<T> from(Bound<T> lower) {
        Assert.notNull(lower, "Lower bound must not be null");
        return new RangeBuilder<>(lower);
    }

    /**
     * 使用给定的下限和上限创建一个新的 {@link Range}。更喜欢 {@link #from(Bound)} 以获得更多构建器风格的 API
     *
     * @param lowerBound 不得为 {@literal null}
     * @param upperBound 不得为 {@literal null}
     * @see #from(Bound)
     */
    public static <T extends Comparable<T>> Range<T> of(Bound<T> lowerBound, Bound<T> upperBound) {
        return new Range<>(lowerBound, upperBound);
    }

    /**
     * 创建一个以给定值作为唯一成员的新范围
     *
     * @param value 不得为 {@literal null}
     * @see Range#closed(Comparable, Comparable)
     */
    public static <T extends Comparable<T>> Range<T> just(T value) {
        return Range.closed(value, value);
    }

    /**
     * 返回 {@link Range} 是否包含给定值
     *
     * @param value 不得为 {@literal null}
     */
    public boolean contains(T value) {
        Assert.notNull(value, "Reference value must not be null");
        boolean greaterThanLowerBound = lowerBound.getValue().map(it -> lowerBound.isInclusive() ? it.compareTo(value) <= 0 : it.compareTo(value) < 0).orElse(true);
        boolean lessThanUpperBound = upperBound.getValue().map(it -> upperBound.isInclusive() ? it.compareTo(value) >= 0 : it.compareTo(value) > 0).orElse(true);
        return greaterThanLowerBound && lessThanUpperBound;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", lowerBound.toPrefixString(), upperBound.toSuffixString());
    }

    public Range.Bound<T> getLowerBound() {
        return this.lowerBound;
    }

    public Range.Bound<T> getUpperBound() {
        return this.upperBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Range)) {
            return false;
        }
        Range<?> range = (Range<?>) o;
        if (!ObjectUtils.nullSafeEquals(lowerBound, range.lowerBound)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(upperBound, range.upperBound);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(lowerBound);
        result = 31 * result + ObjectUtils.nullSafeHashCode(upperBound);
        return result;
    }

    /**
     * 表示边界的值对象。边界可以是 {@link #unbounded() unbounded}、{@link #inclusive(Comparable) including its value} 或 {@link #exclusive(Comparable) its value}
     */
    public static final class Bound<T extends Comparable<T>> {
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static final Bound<?> UNBOUNDED = new Bound(Optional.empty(), true);
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private final Optional<T> value;
        private final boolean inclusive;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Bound(Optional<T> value, boolean inclusive) {
            this.value = value;
            this.inclusive = inclusive;
        }

        /**
         * 创建一个无界的 {@link Bound}
         */
        @SuppressWarnings("unchecked")
        public static <T extends Comparable<T>> Bound<T> unbounded() {
            return (Bound<T>) UNBOUNDED;
        }

        /**
         * 返回此边界是否有界
         */
        public boolean isBounded() {
            return value.isPresent();
        }

        /**
         * 创建包含 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static <T extends Comparable<T>> Bound<T> inclusive(T value) {
            Assert.notNull(value, "Value must not be null");
            return new Bound<>(Optional.of(value), true);
        }

        /**
         * 创建包含 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Integer> inclusive(int value) {
            return inclusive((Integer) value);
        }

        /**
         * 创建包含 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Long> inclusive(long value) {
            return inclusive((Long) value);
        }

        /**
         * 创建包含 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Float> inclusive(float value) {
            return inclusive((Float) value);
        }

        /**
         * 创建包含 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Double> inclusive(double value) {
            return inclusive((Double) value);
        }

        /**
         * 创建不包括 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static <T extends Comparable<T>> Bound<T> exclusive(T value) {
            Assert.notNull(value, "Value must not be null");
            return new Bound<>(Optional.of(value), false);
        }

        /**
         * 创建不包括 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Integer> exclusive(int value) {
            return exclusive((Integer) value);
        }

        /**
         * 创建不包括 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Long> exclusive(long value) {
            return exclusive((Long) value);
        }

        /**
         * 创建不包括 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Float> exclusive(float value) {
            return exclusive((Float) value);
        }

        /**
         * 创建不包括 {@code value} 的边界
         *
         * @param value 不得为 {@literal null}
         */
        public static Bound<Double> exclusive(double value) {
            return exclusive((Double) value);
        }

        String toPrefixString() {
            return getValue().map(Object::toString).map(it -> isInclusive() ? "[".concat(it) : "(".concat(it)).orElse("unbounded");
        }

        String toSuffixString() {
            return getValue().map(Object::toString).map(it -> isInclusive() ? it.concat("]") : it.concat(")")).orElse("unbounded");
        }

        @Override
        public String toString() {
            return value.map(Object::toString).orElse("unbounded");
        }

        public Optional<T> getValue() {
            return this.value;
        }

        public boolean isInclusive() {
            return this.inclusive;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Bound)) {
                return false;
            }
            Bound<?> bound = (Bound<?>) o;
            if (inclusive != bound.inclusive)
                return false;
            return ObjectUtils.nullSafeEquals(value, bound.value);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(value);
            result = 31 * result + (inclusive ? 1 : 0);
            return result;
        }
    }

    /**
     * {@link Range} 的构建器允许指定上限
     */
    public static class RangeBuilder<T extends Comparable<T>> {
        private final Bound<T> lower;

        RangeBuilder(Bound<T> lower) {
            this.lower = lower;
        }

        /**
         * 给定上限 {@link Bound} 创建一个 {@link Range}
         *
         * @param upper 不得为 {@literal null}
         */
        public Range<T> to(Bound<T> upper) {
            Assert.notNull(upper, "Upper bound must not be null");
            return new Range<>(lower, upper);
        }
    }
}
