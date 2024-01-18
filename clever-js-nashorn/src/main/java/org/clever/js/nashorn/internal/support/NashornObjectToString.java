package org.clever.js.nashorn.internal.support;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.clever.js.api.internal.support.ObjectToString;
import org.clever.js.nashorn.utils.JSTools;

import javax.script.Bindings;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 13:04 <br/>
 */
public class NashornObjectToString extends ObjectToString {
    public static final NashornObjectToString Instance = new NashornObjectToString();

    protected NashornObjectToString() {
    }

    @Override
    public String toString(Object obj) {
        if (obj instanceof ScriptObjectMirror) {
            return JSTools.inspect((Bindings) obj);
        } else if (obj instanceof JSObject) {
            return JSTools.inspect((JSObject) obj);
        } else if (obj != null && "jdk.nashorn.internal.runtime.Undefined".equals(obj.getClass().getName())) {
            return obj.toString();
        }
        return super.toString(obj);
    }
}
