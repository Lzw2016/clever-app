package org.clever.js.graaljs.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.clever.js.api.AbstractBuilder;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.Folder;
import org.clever.js.graaljs.GraalScriptEngineInstance;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.clever.util.Assert;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 单个Engine对象的 GraalScriptEngineInstance 创建工厂
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/08/24 19:24 <br/>
 */
@Slf4j
public class GraalSingleEngineFactory extends BasePooledObjectFactory<ScriptEngineInstance<Context, Value>> implements Closeable {
    private final static String COUNTER_NAME = "org.clever.js.graaljs.GraalScriptEngineInstance#COUNTER";
    private final static AtomicLong COUNTER = new AtomicLong(0);

    protected final Folder rootFolder;
    protected final Engine engine;
    protected final HostAccess hostAccess;
    protected final Consumer<Context.Builder> customContext;

    /**
     * @param rootFolder       根目录对象
     * @param engine           Engine对象
     * @param customContext    自定义 Context 逻辑(可选参数)
     * @param customHostAccess 自定义 HostAccess 逻辑(可选参数)
     */
    public GraalSingleEngineFactory(Folder rootFolder, Engine engine, Consumer<Context.Builder> customContext, Consumer<HostAccess.Builder> customHostAccess) {
        Assert.notNull(rootFolder, "参数rootFolder不能为空");
        Assert.notNull(engine, "参数engine不能为空");
        this.rootFolder = rootFolder;
        this.engine = engine;
        this.customContext = customContext;
        this.hostAccess = ScriptEngineUtils.createHostAccessBuilder(customHostAccess).build();
    }

    /**
     * @param rootFolder 根目录对象
     * @param engine     Engine对象
     */
    public GraalSingleEngineFactory(Folder rootFolder, Engine engine) {
        this(rootFolder, engine, null, null);
    }

    /**
     * 返回ScriptEngineInstance Builder对象
     */
    protected AbstractBuilder<Context, Value, ScriptEngineInstance<Context, Value>> getScriptEngineInstanceBuilder() {
        return GraalScriptEngineInstance.Builder.create(engine, rootFolder).setEngine(ScriptEngineUtils.creatEngine(engine, customContext, hostAccess));
    }

    /**
     * 创建一个新的对象
     */
    @Override
    public ScriptEngineInstance<Context, Value> create() {
        ScriptEngineInstance<Context, Value> instance = getScriptEngineInstanceBuilder().build();
        long counter = COUNTER.incrementAndGet();
        instance.getContext().getContextMap().put(COUNTER_NAME, counter);
        log.info("创建 GraalScriptEngineInstance | counter={}", counter);
        // instance.getContext().getEngine().leave();
        return instance;
    }

    /**
     * 封装为池化对象
     */
    @Override
    public PooledObject<ScriptEngineInstance<Context, Value>> wrap(ScriptEngineInstance<Context, Value> obj) {
        return new DefaultPooledObject<>(obj);
    }

    /**
     * 验证对象是否可用
     */
    @Override
    public boolean validateObject(PooledObject<ScriptEngineInstance<Context, Value>> p) {
        // log.info("# validateObject");
        return true;
    }

    /**
     * 激活对象，从池中取对象时会调用此方法
     */
    @Override
    public void activateObject(PooledObject<ScriptEngineInstance<Context, Value>> p) {
        // log.info("# activateObject");
    }

    /**
     * 钝化对象，向池中返还对象时会调用此方法
     */
    @Override
    public void passivateObject(PooledObject<ScriptEngineInstance<Context, Value>> p) {
        // log.info("# passivateObject");
    }

    /**
     * 销毁对象
     */
    @Override
    public void destroyObject(PooledObject<ScriptEngineInstance<Context, Value>> p) throws Exception {
        if (p.getObject() != null) {
            p.getObject().close();
            Object counter = p.getObject().getContext().getContextMap().get(COUNTER_NAME);
            log.info("关闭 GraalScriptEngineInstance | counter={}", counter);
        }
    }

    /**
     * 释放 Engine 对象
     */
    public void close() {
        engine.close();
    }
}
