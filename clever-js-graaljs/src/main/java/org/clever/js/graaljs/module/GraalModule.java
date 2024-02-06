package org.clever.js.graaljs.module;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.module.AbstractModule;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;
import org.clever.js.graaljs.GraalScriptObject;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:58 <br/>
 */
@Slf4j
public class GraalModule extends AbstractModule<Context, Value> {

    public GraalModule(
        ScriptEngineContext<Context, Value> engineContext,
        String id,
        String filename,
        Value exports,
        Module<Value> parent,
        Require<Value> require) {
        super(engineContext, id, filename, exports, parent, require);
    }

    private GraalModule(ScriptEngineContext<Context, Value> engineContext) {
        super(engineContext);
    }

    /**
     * 创建主模块(根模块)
     */
    public static GraalModule createMainModule(ScriptEngineContext<Context, Value> engineContext) {
        return new GraalModule(engineContext);
    }

    @Override
    protected void initModule(Value exports) {
        this.module.putMember(GlobalConstant.MODULE_ID, this.id);
        this.module.putMember(GlobalConstant.MODULE_FILENAME, this.filename);
        this.module.putMember(GlobalConstant.MODULE_LOADED, this.loaded);
        if (this.parent != null) {
            this.module.putMember(GlobalConstant.MODULE_PARENT, this.parent.getModule());
        }
        this.module.putMember(GlobalConstant.MODULE_PATHS, this.paths);
        this.module.putMember(GlobalConstant.MODULE_CHILDREN, this.childrenIds);
        this.module.putMember(GlobalConstant.MODULE_EXPORTS, exports);
        this.module.putMember(GlobalConstant.MODULE_REQUIRE, this.require);
    }

    @Override
    protected Value newScriptObject() {
        return ScriptEngineUtils.newObject(engineContext.getEngine());
    }

    @Override
    public Value getExports() {
        return this.module.getMember(GlobalConstant.MODULE_EXPORTS);
    }

    @Override
    public ScriptObject<Value> getExportsWrapper() {
        return new GraalScriptObject(engineContext, getExports());
    }

    @Override
    protected void doTriggerOnLoaded() {
        this.module.putMember(GlobalConstant.MODULE_LOADED, true);
    }

    @Override
    protected void doTriggerOnRemove() {
    }
}
