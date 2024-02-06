package org.clever.js.api.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.clever.js.api.ScriptEngineInstance;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象的 ScriptEngineInstance 创建工厂
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/02/05 16:00 <br/>
 */
@Slf4j
public abstract class AbstractEngineFactory<E, T> extends BasePooledObjectFactory<ScriptEngineInstance<E, T>> implements Closeable {
    private final static String COUNTER_NAME = "__ENGINE_INSTANCE_COUNTER";
    private final static AtomicLong COUNTER = new AtomicLong(0);

    /**
     * 创建一个新的对象
     */
    @Override
    public ScriptEngineInstance<E, T> create() {
        ScriptEngineInstance<E, T> instance = doCreate();
        long counter = COUNTER.incrementAndGet();
        instance.getEngineContext().getRegisterGlobalVars().put(COUNTER_NAME, counter);
        log.info("创建 ScriptEngineInstance | counter={}", counter);
        return instance;
    }

    /**
     * 封装为池化对象
     */
    @Override
    public PooledObject<ScriptEngineInstance<E, T>> wrap(ScriptEngineInstance<E, T> obj) {
        return new DefaultPooledObject<>(obj);
    }

    /**
     * 验证对象是否可用
     */
    @Override
    public boolean validateObject(PooledObject<ScriptEngineInstance<E, T>> p) {
        // log.info("# validateObject");
        return true;
    }

    /**
     * 激活对象，从池中取对象时会调用此方法
     */
    @Override
    public void activateObject(PooledObject<ScriptEngineInstance<E, T>> p) {
        // log.info("# activateObject");
    }

    /**
     * 钝化对象，向池中返还对象时会调用此方法
     */
    @Override
    public void passivateObject(PooledObject<ScriptEngineInstance<E, T>> p) {
        // log.info("# passivateObject");
    }

    /**
     * 销毁对象
     */
    @Override
    public void destroyObject(PooledObject<ScriptEngineInstance<E, T>> p) throws Exception {
        if (p.getObject() != null) {
            p.getObject().close();
            Object counter = p.getObject().getEngineContext().getRegisterGlobalVars().get(COUNTER_NAME);
            log.info("释放 ScriptEngineInstance | counter={}", counter);
        }
    }

    /**
     * 创建一个新的 ScriptEngineInstance
     */
    protected abstract ScriptEngineInstance<E, T> doCreate();
}
