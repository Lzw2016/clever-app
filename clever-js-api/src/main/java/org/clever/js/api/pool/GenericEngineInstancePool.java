package org.clever.js.api.pool;

import lombok.Getter;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.js.api.ScriptEngineInstance;

/**
 * 脚本引擎池默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/08/24 19:20 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public class GenericEngineInstancePool<E, T> implements EngineInstancePool<E, T> {
    @Getter
    private final GenericObjectPool<ScriptEngineInstance<E, T>> pool;

    public GenericEngineInstancePool(
            final PooledObjectFactory<ScriptEngineInstance<E, T>> factory,
            final GenericObjectPoolConfig<ScriptEngineInstance<E, T>> config) {
        this.pool = new GenericObjectPool<>(factory, config);
    }

    @Override
    public ScriptEngineInstance<E, T> borrowObject() throws Exception {
        return pool.borrowObject();
    }

    @Override
    public void returnObject(ScriptEngineInstance<E, T> obj) {
        pool.returnObject(obj);
    }

    @Override
    public void invalidateObject(ScriptEngineInstance<E, T> obj) throws Exception {
        pool.invalidateObject(obj);
    }

    @Override
    public int getNumIdle() {
        return pool.getNumIdle();
    }

    @Override
    public int getNumActive() {
        return pool.getNumActive();
    }

    @Override
    public void clear() {
        pool.clear();
    }

    @Override
    public void close() {
        pool.close();
    }
}
