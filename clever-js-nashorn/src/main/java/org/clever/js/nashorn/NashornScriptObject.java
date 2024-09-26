package org.clever.js.nashorn;

import org.clever.core.Assert;
import org.clever.js.api.AbstractScriptObject;
import org.clever.js.api.ScriptEngineContext;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Arrays;
import java.util.Collection;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 21:26 <br/>
 */
public class NashornScriptObject extends AbstractScriptObject<NashornScriptEngine, ScriptObjectMirror> {

    public NashornScriptObject(ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context, ScriptObjectMirror original) {
        super(context, original);
    }

    @Override
    public Collection<String> getMemberNames() {
        return Arrays.asList(original.getOwnKeys(false));
    }

    @Override
    public Collection<Object> getMembers() {
        return original.values();
    }

    @Override
    public boolean hasMember(String name) {
        return original.hasMember(name);
    }

    @Override
    public Object getMember(String name) {
        return original.getMember(name);
    }

    @Override
    public void setMember(String name, Object value) {
        original.setMember(name, value);
    }

    @Override
    public void delMember(String name) {
        original.removeMember(name);
    }

    @Override
    public Object callMember(String functionName, Object... args) {
        return original.callMember(functionName, args);
    }

    @Override
    public boolean canExecute() {
        return original.isFunction();
    }

    @Override
    public Object execute(Object... args) {
        Assert.isTrue(original.isFunction(), "当前脚本对象不能执行");
        // ReflectionsUtils.getFieldValue(original, "sobj");
        return original.call(null, args);
    }

    @Override
    public int size() {
        return original.size();
    }
}
