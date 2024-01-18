package org.clever.js.nashorn.utils;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptObjectType;
import org.clever.js.nashorn.utils.support.CustomClassFilter;

import javax.script.Bindings;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * nashorn文档 <br/>
 * https://www.n-k.de/riding-the-nashorn/ <br/>
 * https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/ <br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2019/08/21 09:26 <br/>
 */
public class ScriptEngineUtils {
    private static final NashornScriptEngineFactory NASHORN_FACTORY;


    static {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        NashornScriptEngineFactory nashornFactory = null;
        for (ScriptEngineFactory factory : scriptEngineManager.getEngineFactories()) {
            if (factory instanceof NashornScriptEngineFactory) {
                nashornFactory = (NashornScriptEngineFactory) factory;
                break;
            }
        }
        if (nashornFactory == null) {
            throw new RuntimeException("当前Java版本没有NashornJS引擎，建议使用Java1.8.0_211以上版本");
        }
        NASHORN_FACTORY = nashornFactory;
    }

    // 默认的 NashornScriptEngine
    private static final NashornScriptEngine Default_Engine = creatEngine();
    // 用于构造Js空对象 {}
    private static final ScriptObjectMirror Object_Constructor;
    // 用于构造Js数组
    private static final ScriptObjectMirror Array_Constructor;
    // 用于构造Js Date对象
    private static final ScriptObjectMirror Date_Constructor;
    // 用户构造Js Error 对象
    private static final ScriptObjectMirror Error_Constructor;
    // 用于解析JSON
    private static final ScriptObjectMirror Json_Constructor;
    // Boolean 构造
    private static final ScriptObjectMirror Boolean_Constructor;
    // Number 构造
    private static final ScriptObjectMirror Number_Constructor;
    // String 构造
    private static final ScriptObjectMirror String_Constructor;
    // Object.prototype.toString 函数
    private static final ScriptObjectMirror Object_Prototype_ToString;

    static {
        try {
            Object_Constructor = (ScriptObjectMirror) Default_Engine.eval("Object");
            Array_Constructor = (ScriptObjectMirror) Default_Engine.eval("Array");
            Date_Constructor = (ScriptObjectMirror) Default_Engine.eval("Date");
            Error_Constructor = (ScriptObjectMirror) Default_Engine.eval("Error");
            Json_Constructor = (ScriptObjectMirror) Default_Engine.eval("JSON");
            Boolean_Constructor = (ScriptObjectMirror) Default_Engine.eval("Boolean");
            Number_Constructor = (ScriptObjectMirror) Default_Engine.eval("Number");
            String_Constructor = (ScriptObjectMirror) Default_Engine.eval("String");
            Object_Prototype_ToString = (ScriptObjectMirror) Default_Engine.eval("Object.prototype.toString");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public static NashornScriptEngine getDefaultEngine() {
        return Default_Engine;
    }

    /**
     * 创建一个新的 NashornScriptEngine
     *
     * @param denyAccessClass 不允许访问的Class
     */
    public static NashornScriptEngine creatEngine(Set<Class<?>> denyAccessClass) {
        if (denyAccessClass == null) {
            denyAccessClass = GlobalConstant.Default_Deny_Access_Class;
        } else {
            denyAccessClass.addAll(GlobalConstant.Default_Deny_Access_Class);
        }
        // options 参考 https://wiki.openjdk.java.net/display/Nashorn/Nashorn+jsr223+engine+notes
        String[] options = new String[]{"-doe"};
        return (NashornScriptEngine) NASHORN_FACTORY.getScriptEngine(options, getAppClassLoader(), new CustomClassFilter(denyAccessClass));
    }

    /**
     * 创建一个新的 NashornScriptEngine
     */
    public static NashornScriptEngine creatEngine() {
        return creatEngine(null);
    }

    // 参考 NashornScriptEngineFactory 实现
    private static ClassLoader getAppClassLoader() {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        return ccl == null ? NashornScriptEngineFactory.class.getClassLoader() : ccl;
    }

    /**
     * 新建一个js Error对象
     */
    public static Bindings newError(Object... args) {
        return (Bindings) Error_Constructor.newObject(args);
    }

    /**
     * 新建一个js 普通对象
     */
    public static ScriptObjectMirror newObject(Object... args) {
        return (ScriptObjectMirror) Object_Constructor.newObject(args);
    }

    /**
     * 新建一个js 数组对象
     */
    public static ScriptObjectMirror newArray(Object... args) {
        return (ScriptObjectMirror) Array_Constructor.newObject(args);
    }

    /**
     * 新建一个js 数组对象
     */
    public static ScriptObjectMirror newArray(Collection<?> args) {
        return (ScriptObjectMirror) Array_Constructor.newObject(args.toArray());
    }

    /**
     * 新建一个js Date对象
     */
    public static ScriptObjectMirror newDate(double timeStamp) {
        return (ScriptObjectMirror) Date_Constructor.newObject(timeStamp);
    }

    /**
     * 新建一个js Date对象
     */
    public static ScriptObjectMirror newDate(Date date) {
        return (ScriptObjectMirror) Date_Constructor.newObject((double) date.getTime());
    }

    /**
     * 解析Json成为 ScriptObjectMirror 对象
     */
    public static ScriptObjectMirror parseJson(String json) {
        return (ScriptObjectMirror) Json_Constructor.callMember("parse", json);
    }

    /**
     * 新建一个js boolean对象
     */
    public static ScriptObjectMirror newBoolean(Object... args) {
        return (ScriptObjectMirror) Boolean_Constructor.newObject(args);
    }

    /**
     * 新建一个js number对象
     */
    public static ScriptObjectMirror newNumber(Object... args) {
        return (ScriptObjectMirror) Number_Constructor.newObject(args);
    }

    /**
     * 新建一个js String对象
     */
    public static ScriptObjectMirror newString(Object... args) {
        return (ScriptObjectMirror) String_Constructor.newObject(args);
    }

    /**
     * 获取Script对象类型
     *
     * @param obj Script对象
     */
    public static ScriptObjectType typeof(ScriptObjectMirror obj) {
        if (obj == null) {
            return ScriptObjectType.Null;
        }
        ScriptObjectType type = null;
        String toString = String.valueOf(Object_Prototype_ToString.call(obj));
        switch (StringUtils.lowerCase(toString)) {
            case "[object string]":
                type = ScriptObjectType.String;
                break;
            case "[object number]":
                type = ScriptObjectType.Number;
                break;
            case "[object object]":
                type = ScriptObjectType.Object;
                break;
            case "[object array]":
                type = ScriptObjectType.Array;
                break;
            case "[object function]":
                type = ScriptObjectType.Function;
                break;
            case "[object null]":
                type = ScriptObjectType.Null;
                break;
            case "[object boolean]":
                type = ScriptObjectType.Boolean;
                break;
            case "[object date]":
                type = ScriptObjectType.Date;
                break;
            case "[object symbol]":
                type = ScriptObjectType.Symbol;
                break;
            case "[object json]":
                type = ScriptObjectType.Json;
                break;
            case "[object math]":
                type = ScriptObjectType.Math;
                break;
            case "[object regexp]":
                type = ScriptObjectType.Regexp;
                break;
            case "[object undefined]":
                type = ScriptObjectType.Undefined;
                break;
            default:
                // 无法识别
        }
        return type;
    }
}
