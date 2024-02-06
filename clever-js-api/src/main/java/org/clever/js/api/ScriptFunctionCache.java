package org.clever.js.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.exception.ExceptionUtils;
import org.clever.js.api.utils.ScriptCodeUtils;
import org.clever.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/05 22:50 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
@Slf4j
@Getter
public abstract class ScriptFunctionCache<E, T> implements Closeable {
    protected static final AtomicLong FUC_COUNTER = new AtomicLong(0);
    protected static final int Default_Max_Capacity = 4096;

    /**
     * script引擎对象
     */
    protected final ScriptEngineInstance<E, T> engineInstance;
    /**
     * 缓存 {@code Cache<script code, ScriptObject>}
     */
    protected final Cache<String, ScriptObject<T>> cache;

    /**
     * 创建 ScriptObjectCache
     *
     * @param engineInstance script引擎对象
     * @param expireTime     缓存的过期时间，单位：秒(小于等于0表示不清除)
     * @param maxCapacity    最大缓存容量
     */
    public ScriptFunctionCache(ScriptEngineInstance<E, T> engineInstance, long expireTime, int maxCapacity) {
        Assert.notNull(engineInstance, "参数 engineInstance 不能为 null");
        this.engineInstance = engineInstance;
        if (maxCapacity < 0) {
            maxCapacity = Default_Max_Capacity;
        }
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .initialCapacity(32)
            .maximumSize(maxCapacity);
        if (expireTime > 0) {
            cacheBuilder.removalListener(notification -> {
                Object code = notification.getKey();
                log.debug("ModuleCache 移除缓存,原因: {} | 内容 -> {}", notification.getCause(), ScriptCodeUtils.compressCode(String.valueOf(code), false));
            }).expireAfterWrite(expireTime, TimeUnit.SECONDS);
        }
        this.cache = cacheBuilder.build();
    }

    /**
     * 创建 ScriptObjectCache
     *
     * @param engineInstance script引擎对象
     */
    public ScriptFunctionCache(ScriptEngineInstance<E, T> engineInstance) {
        this(engineInstance, -1, Default_Max_Capacity);
    }

    /**
     * 根据“完整的函数定义代码”创建脚本函数对象
     *
     * @param funCode 完整的函数定义代码
     */
    protected abstract ScriptObject<T> createFunction(String funCode);

    /**
     * 把脚本代码包装成函数对象(会使用缓存)
     *
     * @param code 脚本代码片段(并非完整的函数定义，仅仅是一段脚本代码)
     * @return 脚本函数对象
     */
    public ScriptObject<T> wrapFunction(String code) {
        Assert.isNotBlank(code, "参数 code 不能为空");
        ScriptObject<T> function = cache.getIfPresent(code);
        if (function != null) {
            return function;
        }
        synchronized (cache) {
            // 二次确认
            function = cache.getIfPresent(code);
            if (function != null) {
                return function;
            }
            try {
                function = cache.get(code, () -> {
                    String compressCode = ScriptCodeUtils.compressCode(code, true);
                    String funCode = ScriptCodeUtils.wrapFunction(compressCode, Conv.asString(FUC_COUNTER.incrementAndGet()));
                    return createFunction(funCode);
                });
            } catch (ExecutionException e) {
                throw ExceptionUtils.unchecked(e);
            }
        }
        return function;
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        cache.invalidateAll();
    }

    /**
     * 移除指定的缓存
     *
     * @param code 脚本代码
     */
    public void removeCache(String code) {
        cache.invalidate(code);
    }

    /**
     * 释放资源
     */
    @Override
    public void close() throws IOException {
        clearCache();
        engineInstance.close();
    }
}
