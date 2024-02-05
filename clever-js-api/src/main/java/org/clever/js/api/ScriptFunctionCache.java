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
    private static final AtomicLong FUC_COUNTER = new AtomicLong(0);
    private static final int Default_Max_Capacity = 4096;

    /**
     * script引擎对象
     */
    private final E context;
    /**
     * 缓存 {@code Cache<script code, ScriptObject>}
     */
    private final Cache<String, ScriptObject<T>> cache;

    /**
     * 创建 ScriptObjectCache
     *
     * @param context     script引擎对象
     * @param expireTime  缓存的过期时间，单位：秒(小于等于0表示不清除)
     * @param maxCapacity 最大缓存容量
     */
    public ScriptFunctionCache(E context, long expireTime, int maxCapacity) {
        Assert.notNull(context, "参数 context 不能为空");
        this.context = context;
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
     * 创建 ScriptObjectCache(不定时清除缓存，使用默认的缓存容量)
     *
     * @param context script引擎对象
     */
    public ScriptFunctionCache(E context) {
        this(context, -1, Default_Max_Capacity);
    }

    /**
     * 根据“完整的函数定义代码”创建脚本函数对象
     *
     * @param funCode 完整的函数定义代码
     */
    protected abstract ScriptObject<T> createFunction(String funCode);

    /**
     * 把脚本代码包装成函数对象
     *
     * @param code 脚本代码片段(并非完整的函数定义，仅仅是一段脚本代码)
     * @return 脚本函数对象
     */
    public ScriptObject<T> wrapFunction(String code) {
        Assert.isNotBlank(code, "脚本代码不能为空");
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

//    /**
//     * 获取ScriptObject
//     *
//     * @param code   脚本代码
//     * @param loader 根据脚本代码创建ScriptObject
//     * @return 返回ScriptObject，不会返回null
//     */
//    @SuppressWarnings("unchecked")
//    public ScriptObject<T> get(String code, Callable<ScriptObject<T>> loader) {
//        try {
//            return (ScriptObject<T>) cache.get(code, loader);
//        } catch (ExecutionException e) {
//            throw ExceptionUtils.unchecked(e);
//        }
//    }
//
//    /**
//     * 获取ScriptObject
//     *
//     * @param code 脚本代码
//     * @return 返回ScriptObject，不存在就返回null
//     */
//    @SuppressWarnings("unchecked")
//    public  ScriptObject<T> get(String code) {
//        return (ScriptObject<T>) cache.getIfPresent(code);
//    }
//
//    /**
//     * 缓存ScriptObject
//     *
//     * @param code         脚本代码
//     * @param scriptObject ScriptObject对象
//     */
//    public  void put(String code, ScriptObject<T> scriptObject) {
//        cache.put(code, scriptObject);
//    }

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
    public void close() {
        clearCache();
    }
}
