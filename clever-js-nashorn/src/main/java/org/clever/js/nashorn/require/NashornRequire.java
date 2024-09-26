package org.clever.js.nashorn.require;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.AbstractRequire;
import org.clever.js.api.require.Require;
import org.clever.js.nashorn.module.NashornModule;
import org.clever.js.nashorn.utils.ScriptEngineUtils;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 21:40 <br/>
 */
@Slf4j
public class NashornRequire extends AbstractRequire<NashornScriptEngine, ScriptObjectMirror> {

    public NashornRequire(
            ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context,
            Module<ScriptObjectMirror> currentModule,
            Folder currentModuleFolder) {
        super(context, currentModule, currentModuleFolder);
    }

    public NashornRequire(ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context, Folder currentModuleFolder) {
        super(context, currentModuleFolder);
    }

    @Override
    protected ScriptObjectMirror newScriptObject() {
        return ScriptEngineUtils.newObject();
    }

    @Override
    protected AbstractRequire<NashornScriptEngine, ScriptObjectMirror> newRequire(
            ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> engineContext,
            Folder currentModuleFolder) {
        return new NashornRequire(engineContext, currentModuleFolder);
    }

    @Override
    protected Module<ScriptObjectMirror> newModule(
            ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> engineContext,
            String id,
            String filename,
            ScriptObjectMirror exports,
            Module<ScriptObjectMirror> parent,
            Require<ScriptObjectMirror> require) {
        return new NashornModule(engineContext, id, filename, exports, parent, require);
    }

    @Override
    protected void moduleFunctionCall(
            ScriptObjectMirror function,
            ScriptObjectMirror that,
            ScriptObjectMirror exports,
            Require<ScriptObjectMirror> require,
            ScriptObjectMirror module,
            String filename,
            String dirname) {
        function.call(that, exports, require, module, filename, dirname);
    }
}
