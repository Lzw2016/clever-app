//package org.clever.js.javet.utils;
//
//import com.eclipsesource.v8.V8;
//import com.eclipsesource.v8.V8Array;
//import com.eclipsesource.v8.V8Object;
//import com.eclipsesource.v8.utils.V8ObjectUtils;
//import org.clever.js.api.utils.Assert;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 12:40 <br/>
// */
//public class ScriptEngineUtils {
//
//    /**
//     * 新建一个js 普通对象
//     */
//    public static V8Object newObject(V8 v8, Object... args) {
//        Assert.notNull(v8, "参数v8不能为空");
//        V8Array parameters = new V8Array(v8);
//        if (args != null) {
//            for (Object arg : args) {
//                V8ObjectUtils.pushValue(v8, parameters, arg);
//            }
//        }
//        V8Object v8Object = v8.executeObjectFunction("Object", parameters);
//        // parameters.release();
//        return v8Object;
//    }
//
//    /**
//     * 新建一个js 数组对象
//     */
//    public static V8Object newArray(V8 v8, Object... args) {
//        Assert.notNull(v8, "参数v8不能为空");
//        V8Array parameters = new V8Array(v8);
//        if (args != null) {
//            for (Object arg : args) {
//                V8ObjectUtils.pushValue(v8, parameters, arg);
//            }
//        }
//        V8Object v8Object = v8.executeObjectFunction("Array", parameters);
//        // parameters.release();
//        return v8Object;
//    }
//
//    /**
//     * 解析Json成为 Value 对象
//     */
//    public static V8Object parseJson(V8 v8, String json) {
//        Assert.notNull(v8, "参数v8不能为空");
//        V8Object v8JSON = v8.executeObjectScript("JSON");
//        V8Array parameters = new V8Array(v8);
//        parameters.push(json);
//        V8Object res = v8JSON.executeObjectFunction("parse", parameters);
//        // parameters.release();
//        // v8JSON.release();
//        return res;
//    }
//}
