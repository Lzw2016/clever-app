package org.clever.js.nashorn.module;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.module.AbstractModule;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;
import org.clever.js.nashorn.NashornScriptObject;
import org.clever.js.nashorn.utils.ScriptEngineUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/15 22:39 <br/>
 */
@Slf4j
public class NashornModule extends AbstractModule<NashornScriptEngine, ScriptObjectMirror> {
    public NashornModule(
            ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context,
            String id,
            String filename,
            ScriptObjectMirror exports,
            Module<ScriptObjectMirror> parent,
            Require<ScriptObjectMirror> require) {
        super(context, id, filename, exports, parent, require);
    }

    private NashornModule(ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context) {
        super(context);
    }

    /**
     * 创建主模块(根模块)
     */
    public static NashornModule createMainModule(ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context) {
        return new NashornModule(context);
    }

    @Override
    protected void initModule(ScriptObjectMirror exports) {
        this.module.put(GlobalConstant.MODULE_ID, this.id);
        this.module.put(GlobalConstant.MODULE_FILENAME, this.filename);
        this.module.put(GlobalConstant.MODULE_LOADED, this.loaded);
        if (this.parent != null) {
            this.module.put(GlobalConstant.MODULE_PARENT, this.parent.getModule());
        }
        this.module.put(GlobalConstant.MODULE_PATHS, this.paths);
        this.module.put(GlobalConstant.MODULE_CHILDREN, this.childrenIds);
        this.module.put(GlobalConstant.MODULE_EXPORTS, exports);
        this.module.put(GlobalConstant.MODULE_REQUIRE, this.require);
    }

    @Override
    protected ScriptObjectMirror newScriptObject() {
        return ScriptEngineUtils.newObject();
    }

    @Override
    public ScriptObjectMirror getExports() {
        return (ScriptObjectMirror) this.module.get(GlobalConstant.MODULE_EXPORTS);
    }

    @Override
    public ScriptObject<ScriptObjectMirror> getExportsWrapper() {
        return new NashornScriptObject(engineContext, getExports());
    }

    @Override
    protected void doTriggerOnLoaded() {
        this.module.put(GlobalConstant.MODULE_LOADED, true);
    }

    @Override
    protected void doTriggerOnRemove() {
    }
}
