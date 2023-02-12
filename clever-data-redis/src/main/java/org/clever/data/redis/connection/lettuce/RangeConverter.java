package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.Range;
import io.lettuce.core.Range.Boundary;
import io.lettuce.core.codec.StringCodec;
import org.clever.data.domain.Range.Bound;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:38 <br/>
 */
class RangeConverter {
    static <T> Range<T> toRange(org.clever.data.domain.Range<?> range) {
        return toRange(range, StringCodec.UTF8::encodeValue);
    }

    /**
     * @param range        要转换的源 {@link org.clever.data.domain.Range}
     * @param lowerDefault 如果 {@link org.clever.data.domain.Range#getLowerBound()} 不是 {@link Bound#isBounded() bounded} 则使用的下限默认值
     * @param upperDefault 如果 {@link org.clever.data.domain.Range#getUpperBound()} 不是 {@link Bound#isBounded() bounded} 则要使用的上限默认值
     * @return {@link Range} 的新实例
     */
    static <T> Range<T> toRangeWithDefault(org.clever.data.domain.Range<?> range, T lowerDefault, T upperDefault) {
        return toRangeWithDefault(range, lowerDefault, upperDefault, StringCodec.UTF8::encodeValue);
    }

    static <T> Range<T> toRange(org.clever.data.domain.Range<?> range, Function<String, ? extends Object> stringEncoder) {
        return toRangeWithDefault(range, null, null, stringEncoder);
    }

    /**
     * @param range         要转换的源 {@link org.clever.data.domain.Range}
     * @param lowerDefault  如果 {@link org.clever.data.domain.Range#getLowerBound()} 不是 {@link Bound#isBounded() bounded} 则使用的较低默认值
     * @param upperDefault  如果 {@link org.clever.data.domain.Range#getUpperBound()} 不是 {@link Bound#isBounded() bounded} 则要使用的上限默认值
     * @param stringEncoder 要使用的编码器
     * @return {@link Range} 的新实例
     */
    static <T> Range<T> toRangeWithDefault(org.clever.data.domain.Range<?> range, T lowerDefault, T upperDefault, Function<String, ?> stringEncoder) {
        return Range.from(
                lowerBoundArgOf(range, lowerDefault, stringEncoder),
                upperBoundArgOf(range, upperDefault, stringEncoder)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> Boundary<T> lowerBoundArgOf(org.clever.data.domain.Range<?> range, T lowerDefault, Function<String, ?> stringEncoder) {
        return (Boundary<T>) rangeToBoundArgumentConverter(false, stringEncoder).apply(range, lowerDefault);
    }

    @SuppressWarnings("unchecked")
    private static <T> Boundary<T> upperBoundArgOf(org.clever.data.domain.Range<?> range, T upperDefault, Function<String, ?> stringEncoder) {
        return (Boundary<T>) rangeToBoundArgumentConverter(true, stringEncoder).apply(range, upperDefault);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BiFunction<org.clever.data.domain.Range, Object, Boundary<?>> rangeToBoundArgumentConverter(boolean upper, Function<String, ?> stringEncoder) {
        return (source, defaultValue) -> {
            boolean inclusive = upper ? source.getUpperBound().isInclusive() : source.getLowerBound().isInclusive();
            Object value = upper ? source.getUpperBound().getValue().orElse(defaultValue) : source.getLowerBound().getValue().orElse(defaultValue);
            if (value instanceof Number) {
                return inclusive ? Boundary.including((Number) value) : Boundary.excluding((Number) value);
            }
            if (value instanceof String) {
                if (!StringUtils.hasText((String) value) || ObjectUtils.nullSafeEquals(value, "+") || ObjectUtils.nullSafeEquals(value, "-")) {
                    return Boundary.unbounded();
                }
                Object encoded = stringEncoder.apply((String) value);
                return inclusive ? Boundary.including(encoded) : Boundary.excluding(encoded);
            }
            if (value == null) {
                return Boundary.unbounded();
            }
            return inclusive ? Boundary.including((ByteBuffer) value) : Boundary.excluding((ByteBuffer) value);
        };
    }
}
