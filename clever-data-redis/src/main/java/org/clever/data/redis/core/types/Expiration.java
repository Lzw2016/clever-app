package org.clever.data.redis.core.types;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Expiration 持有一个值及其关联的 {@link TimeUnit}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 19:42 <br/>
 */
public class Expiration {
    private final long expirationTime;
    private final TimeUnit timeUnit;

    /**
     * 创建新的{@link Expiration}
     *
     * @param expirationTime 可以是 {@literal null}。默认为 {@link TimeUnit#SECONDS}
     */
    protected Expiration(long expirationTime, TimeUnit timeUnit) {
        this.expirationTime = expirationTime;
        this.timeUnit = timeUnit != null ? timeUnit : TimeUnit.SECONDS;
    }

    /**
     * 获取转换成 {@link TimeUnit#MILLISECONDS} 的过期时间
     */
    public long getExpirationTimeInMilliseconds() {
        return getConverted(TimeUnit.MILLISECONDS);
    }

    /**
     * 获取转换成 {@link TimeUnit#SECONDS} 的过期时间
     */
    public long getExpirationTimeInSeconds() {
        return getConverted(TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * 获取到期时间的时间单位
     */
    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    /**
     * 将到期时间转换为所需的 {@code targetTimeUnit}
     *
     * @param targetTimeUnit 不得{@literal null}
     */
    public long getConverted(TimeUnit targetTimeUnit) {
        Assert.notNull(targetTimeUnit, "TargetTimeUnit must not be null!");
        return targetTimeUnit.convert(expirationTime, timeUnit);
    }

    /**
     * 使用 {@link TimeUnit#SECONDS} 创建新的 {@link Expiration}
     */
    public static Expiration seconds(long expirationTime) {
        return new Expiration(expirationTime, TimeUnit.SECONDS);
    }

    /**
     * 使用 {@link TimeUnit#MILLISECONDS} 创建新的 {@link Expiration}。
     */
    public static Expiration milliseconds(long expirationTime) {
        return new Expiration(expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 使用给定的 {@literal unix timestamp} 和 {@link TimeUnit} 创建新的 {@link Expiration}
     *
     * @param unixTimestamp 密钥将过期的 unix 时间戳
     * @param timeUnit      不得为 {@literal null}
     * @return {@link Expiration} 的新实例
     */
    public static Expiration unixTimestamp(long unixTimestamp, TimeUnit timeUnit) {
        return new ExpireAt(unixTimestamp, timeUnit);
    }

    /**
     * 获取一个 {@link Expiration} 表示保留现有的。例如。发送 {@code SET} 命令时 <br/>
     * <strong>注意: </strong>请按照各个命令的文档查看 {@code keepTtl()} 是否适用
     *
     * @return 从不 {@literal null}
     */
    public static Expiration keepTtl() {
        return KeepTtl.INSTANCE;
    }

    /**
     * 使用提供的 {@link TimeUnit} 创建新的 {@link Expiration}。<br/>
     * 大于 {@link TimeUnit#SECONDS} 的单位将转换为 {@link TimeUnit#SECONDS}。<br/>
     * 小于 {@link TimeUnit#MILLISECONDS} 的单位将转换为 {@link TimeUnit#MILLISECONDS} 并且可能会丢失精度，
     * 因为 {@link TimeUnit#MILLISECONDS} 是 Redis 支持的最小粒度。
     *
     * @param timeUnit 可以是 {@literal null}。默认为 {@link TimeUnit#SECONDS}
     */
    public static Expiration from(long expirationTime, TimeUnit timeUnit) {
        if (ObjectUtils.nullSafeEquals(timeUnit, TimeUnit.MICROSECONDS)
                || ObjectUtils.nullSafeEquals(timeUnit, TimeUnit.NANOSECONDS)
                || ObjectUtils.nullSafeEquals(timeUnit, TimeUnit.MILLISECONDS)) {
            return new Expiration(timeUnit.toMillis(expirationTime), TimeUnit.MILLISECONDS);
        }
        if (timeUnit != null) {
            return new Expiration(timeUnit.toSeconds(expirationTime), TimeUnit.SECONDS);
        }
        return new Expiration(expirationTime, TimeUnit.SECONDS);
    }

    /**
     * 使用提供的 {@link java.time.Duration} 创建新的 {@link Expiration}。<br/>
     * 至少 {@link TimeUnit#SECONDS} 分辨率的持续时间使用秒，使用毫秒的持续时间使用 {@link TimeUnit#MILLISECONDS} 分辨率。
     *
     * @param duration 不得为 {@literal null}
     */
    public static Expiration from(Duration duration) {
        Assert.notNull(duration, "Duration must not be null!");
        if (duration.toMillis() % 1000 == 0) {
            return new Expiration(duration.getSeconds(), TimeUnit.SECONDS);
        }
        return new Expiration(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 创建新的持久性 {@link Expiration}
     */
    public static Expiration persistent() {
        return new Expiration(-1, TimeUnit.SECONDS);
    }

    /**
     * @return {@literal true} 如果 {@link Expiration} 设置为持久
     */
    public boolean isPersistent() {
        return expirationTime == -1;
    }

    /**
     * @return {@literal true} 如果不应修改现有密钥的 {@link Expiration}
     */
    public boolean isKeepTtl() {
        return false;
    }

    /**
     * @return {@literal true} 如果 {@link Expiration} 设置为密钥将过期的指定 Unix 时间
     */
    public boolean isUnixTimestamp() {
        return false;
    }

    private static class KeepTtl extends Expiration {
        static KeepTtl INSTANCE = new KeepTtl();

        private KeepTtl() {
            super(-2, null);
        }

        @Override
        public boolean isKeepTtl() {
            return true;
        }
    }

    private static class ExpireAt extends Expiration {
        private ExpireAt(long expirationTime, TimeUnit timeUnit) {
            super(expirationTime, timeUnit);
        }

        public boolean isUnixTimestamp() {
            return true;
        }
    }
}
