package org.clever.data.redis.core;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 具有计算Redis超时方法的Helper类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:19 <br/>
 */
public abstract class TimeoutUtils {
    /**
     * 检查给定的持续时间是否可以用 {@code sec} 表示或需要 {@code msec} 的表示。
     *
     * @param duration 要检查的实际 {@link Duration}。从不为 {@literal null}
     * @return 如果 {@link Duration} 包含毫秒信息，则返回 {@literal true}
     */
    public static boolean hasMillis(Duration duration) {
        return duration.toMillis() % 1000 != 0;
    }

    /**
     * 将给定超时转换为秒。
     * <p>
     * 由于0超时会无限期地阻止某些Redis操作，因此如果原始值大于0，但在转换时被截断为0，则此方法将返回1。
     *
     * @param duration 要转换的持续时间
     * @return 转换的超时
     */
    public static long toSeconds(Duration duration) {
        return roundUpIfNecessary(duration.toMillis(), duration.getSeconds());
    }

    /**
     * 将给定超时转换为秒。
     * <p>
     * 由于0超时会无限期地阻止某些Redis操作，因此如果原始值大于0，但在转换时被截断为0，则此方法将返回1
     *
     * @param timeout 要转换的超时
     * @param unit    超时的单位
     * @return 转换的超时
     */
    public static long toSeconds(long timeout, TimeUnit unit) {
        return roundUpIfNecessary(timeout, unit.toSeconds(timeout));
    }

    /**
     * 将给定的超时转换为秒（秒的小数）
     *
     * @param timeout 要转换的超时
     * @param unit    超时的单位
     * @return 转换的超时
     */
    public static double toDoubleSeconds(long timeout, TimeUnit unit) {
        switch (unit) {
            case MILLISECONDS:
            case MICROSECONDS:
            case NANOSECONDS:
                return unit.toMillis(timeout) / 1000d;
            default:
                return unit.toSeconds(timeout);
        }
    }

    /**
     * 将给定超时转换为毫秒。
     * <p>
     * 由于0超时会无限期地阻止某些Redis操作，因此如果原始值大于0，但在转换时被截断为0，则此方法将返回1
     *
     * @param timeout 要转换的超时
     * @param unit    超时的单位
     * @return 转换的超时
     */
    public static long toMillis(long timeout, TimeUnit unit) {
        return roundUpIfNecessary(timeout, unit.toMillis(timeout));
    }

    private static long roundUpIfNecessary(long timeout, long convertedTimeout) {
        // 0超时将无限期地阻止某些Redis操作，如果不是这样的话，则进行舍入
        if (timeout > 0 && convertedTimeout == 0) {
            return 1;
        }
        return convertedTimeout;
    }
}
