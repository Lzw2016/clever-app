package org.clever.js.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.exception.ExceptionUtils;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.require.Require;
import org.clever.js.api.utils.ScriptCodeUtils;
import org.clever.util.Assert;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:27 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
@Slf4j
public abstract class AbstractScriptEngineInstance<E, T> implements ScriptEngineInstance<E, T> {
    protected static final AtomicLong FUC_COUNTER = new AtomicLong(0);
    protected static final int DEFAULT_MAX_CAPACITY = 4096;

    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;
    /**
     * Function对象缓存 {@code Cache<script code, ScriptObject>}
     */
    protected final Cache<String, ScriptObject<T>> functionCache;

    /**
     * @param engineContext 引擎上下文
     * @param expireTime    缓存的过期时间，单位：秒(小于等于0表示不清除)
     * @param maxCapacity   最大缓存容量
     */
    public AbstractScriptEngineInstance(ScriptEngineContext<E, T> engineContext, long expireTime, int maxCapacity) {
        Assert.notNull(engineContext, "参数 engineContext 不能为 null");
        if (maxCapacity < 0) {
            maxCapacity = DEFAULT_MAX_CAPACITY;
        }
        this.engineContext = engineContext;
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .initialCapacity(32)
            .maximumSize(maxCapacity);
        if (expireTime > 0) {
            cacheBuilder.removalListener(notification -> {
                Object code = notification.getKey();
                log.debug("ModuleCache 移除缓存,原因: {} | 内容 -> {}", notification.getCause(), ScriptCodeUtils.compressCode(String.valueOf(code), false));
            }).expireAfterWrite(expireTime, TimeUnit.SECONDS);
        }
        this.functionCache = cacheBuilder.build();
    }

    /**
     * @param engineContext 引擎上下文
     */
    public AbstractScriptEngineInstance(ScriptEngineContext<E, T> engineContext) {
        this(engineContext, -1, DEFAULT_MAX_CAPACITY);
    }

    @Override
    public ScriptEngineContext<E, T> getEngineContext() {
        return engineContext;
    }

    @Override
    public E getEngine() {
        return engineContext.getEngine();
    }

    @Override
    public Folder getRootPath() {
        return engineContext.getRootPath();
    }

    @Override
    public T getGlobal() {
        return engineContext.getGlobal();
    }

    @Override
    public Require<T> getRequire() {
        return engineContext.getRequire();
    }

    @Override
    public ScriptObject<T> require(String id) throws Exception {
        T original = engineContext.getRequire().require(id);
        return wrapScriptObject(original);
    }

    @Override
    public void clearModuleCache() {
        engineContext.getModuleCache().clear();
    }

    @Override
    public void removeModuleCache(String id) {
        engineContext.getModuleCache().remove(id);
    }

    @Override
    public ScriptObject<T> wrapFunction(String code) {
        Assert.isNotBlank(code, "参数 code 不能为空");
        ScriptObject<T> function = functionCache.getIfPresent(code);
        if (function != null) {
            return function;
        }
        synchronized (functionCache) {
            // 二次确认
            function = functionCache.getIfPresent(code);
            if (function != null) {
                return function;
            }
            try {
                function = functionCache.get(code, () -> {
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

    @Override
    public void clearFunctionCache() {
        functionCache.invalidateAll();
    }

    @Override
    public void removeFunctionCache(String code) {
        functionCache.invalidate(code);
    }

    @Override
    public void close() {
        clearModuleCache();
        clearFunctionCache();
    }

    /**
     * 根据 “script引擎对象” 包装成 ScriptObject
     */
    protected abstract ScriptObject<T> wrapScriptObject(T original);

    /**
     * 根据“完整的函数定义代码”创建脚本函数对象
     *
     * @param funCode 完整的函数定义代码
     */
    protected abstract ScriptObject<T> createFunction(String funCode);

    /**
     * ScriptEngineInstance 构建器
     *
     * @param <E> script引擎类型
     * @param <T> script引擎对象类型
     */
    public static abstract class AbstractBuilder<E, T> extends org.clever.js.api.AbstractBuilder<E, T, ScriptEngineInstance<E, T>> {
        /**
         * @param rootPath 根路径文件夹
         */
        public AbstractBuilder(Folder rootPath) {
            super(rootPath);
        }
    }
}
