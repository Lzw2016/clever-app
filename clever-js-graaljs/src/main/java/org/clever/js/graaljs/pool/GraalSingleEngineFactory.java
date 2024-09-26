package org.clever.js.graaljs.pool;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Assert;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.pool.AbstractEngineFactory;
import org.clever.js.graaljs.GraalScriptEngineInstance;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.function.Consumer;

/**
 * 单个Engine对象的 GraalScriptEngineInstance 创建工厂
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/08/24 19:24 <br/>
 */
@Getter
@Slf4j
public class GraalSingleEngineFactory extends AbstractEngineFactory<Context, Value> {
    protected final GraalScriptEngineInstance.Builder builder;

    public GraalSingleEngineFactory(GraalScriptEngineInstance.Builder builder) {
        Assert.notNull(builder, "参数 builder 不能为 null");
        this.builder = builder;
    }

    @Override
    protected ScriptEngineInstance<Context, Value> doCreate() {
        return builder.build();
    }

    /**
     * 释放 Engine 对象
     */
    @Override
    public void close() {
        Engine graalvmEngine = builder.getGraalvmEngine();
        if (graalvmEngine != null) {
            graalvmEngine.close();
        }
    }

    /**
     * @param rootFolder       根目录对象
     * @param graalvmEngine    Engine对象
     * @param customContext    自定义 Context 逻辑
     * @param customHostAccess 自定义 HostAccess 逻辑
     */
    public static GraalSingleEngineFactory create(Folder rootFolder,
                                                  Engine graalvmEngine,
                                                  Consumer<Context.Builder> customContext,
                                                  Consumer<HostAccess.Builder> customHostAccess) {
        GraalScriptEngineInstance.Builder builder = new GraalScriptEngineInstance.Builder(graalvmEngine, rootFolder)
            .setCustomContext(customContext)
            .setCustomHostAccess(customHostAccess);
        return new GraalSingleEngineFactory(builder);
    }

    /**
     * @param rootFolder    根目录对象
     * @param graalvmEngine Engine对象
     */
    public static GraalSingleEngineFactory create(Folder rootFolder, Engine graalvmEngine) {
        return create(rootFolder, graalvmEngine, null, null);
    }

    /**
     * @param rootFolder 根目录对象
     */
    public static GraalSingleEngineFactory create(Folder rootFolder) {
        Engine graalvmEngine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        return create(rootFolder, graalvmEngine, null, null);
    }
}
