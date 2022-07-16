package org.clever.boot.context.properties.source;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * 使用{@link SoftReference}尽可能长时间缓存值的简单缓存。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:00 <br/>
 *
 * @param <T> 值类型
 * @author Phillip Webb
 */
class SoftReferenceConfigurationPropertyCache<T> implements ConfigurationPropertyCaching {
    private static final Duration UNLIMITED = Duration.ZERO;

    private final boolean neverExpire;
    private volatile Duration timeToLive;
    private volatile SoftReference<T> value = new SoftReference<>(null);
    private volatile Instant lastAccessed = now();

    SoftReferenceConfigurationPropertyCache(boolean neverExpire) {
        this.neverExpire = neverExpire;
    }

    @Override
    public void enable() {
        this.timeToLive = UNLIMITED;
    }

    @Override
    public void disable() {
        this.timeToLive = null;
    }

    @Override
    public void setTimeToLive(Duration timeToLive) {
        this.timeToLive = (timeToLive == null || timeToLive.isZero()) ? null : timeToLive;
    }

    @Override
    public void clear() {
        this.lastAccessed = null;
    }

    /**
     * 从缓存中获取值，必要时创建它。
     *
     * @param factory       如果没有对项目的引用，则用于创建该项目的工厂。
     * @param refreshAction 调用操作以刷新值（如果值已过期）
     * @return 缓存中的值
     */
    T get(Supplier<T> factory, UnaryOperator<T> refreshAction) {
        T value = getValue();
        if (value == null) {
            value = refreshAction.apply(factory.get());
            setValue(value);
        } else if (hasExpired()) {
            value = refreshAction.apply(value);
            setValue(value);
        }
        if (!this.neverExpire) {
            this.lastAccessed = now();
        }
        return value;
    }

    private boolean hasExpired() {
        if (this.neverExpire) {
            return false;
        }
        Duration timeToLive = this.timeToLive;
        Instant lastAccessed = this.lastAccessed;
        if (timeToLive == null || lastAccessed == null) {
            return true;
        }
        return !UNLIMITED.equals(timeToLive) && now().isAfter(lastAccessed.plus(timeToLive));
    }

    protected Instant now() {
        return Instant.now();
    }

    protected T getValue() {
        return this.value.get();
    }

    protected void setValue(T value) {
        this.value = new SoftReference<>(value);
    }
}
