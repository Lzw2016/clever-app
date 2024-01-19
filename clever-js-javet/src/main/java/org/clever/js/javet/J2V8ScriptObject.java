//package org.clever.js.javet;
//
//import com.eclipsesource.v8.V8;
//import com.eclipsesource.v8.V8Array;
//import com.eclipsesource.v8.V8Object;
//import com.eclipsesource.v8.V8Value;
//import com.eclipsesource.v8.utils.V8ObjectUtils;
//import org.clever.js.api.AbstractScriptObject;
//import org.clever.js.api.ScriptEngineContext;
//
//import java.util.*;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 12:40 <br/>
// */
//public class J2V8ScriptObject extends AbstractScriptObject<V8, V8Object> {
//
//    public J2V8ScriptObject(ScriptEngineContext<V8, V8Object> context, V8Object original) {
//        super(context, original);
//    }
//
//    @Override
//    public Collection<String> getMemberNames() {
//        return Arrays.asList(original.getKeys());
//    }
//
//    @Override
//    public Object getMember(String name) {
//        return original.get(name);
//    }
//
//    @Override
//    public boolean hasMember(String name) {
//        return original.contains(name);
//    }
//
//    @Override
//    public Collection<Object> getMembers() {
//        String[] keys = original.getKeys();
//        if (keys == null) {
//            return Collections.emptyList();
//        }
//        List<Object> list = new ArrayList<>(keys.length);
//        for (String key : keys) {
//            list.add(original.get(key));
//        }
//        return list;
//    }
//
//    @Override
//    public Object callMember(String functionName, Object... args) {
//        return original.executeJSFunction(functionName, args);
//    }
//
//    @Override
//    public void delMember(String name) {
//        original.addUndefined(name);
//    }
//
//    @Override
//    public void setMember(String name, Object value) {
//        V8Array parameters = new V8Array(context.getEngine());
//        V8ObjectUtils.pushValue(context.getEngine(), parameters, value);
//        Object val = parameters.get(0);
//        if (val == null) {
//            original.addUndefined(name);
//        } else if (val instanceof Integer) {
//            original.add(name, (Integer) val);
//        } else if (val instanceof Long) {
//            original.add(name, ((Long) val).doubleValue());
//        } else if (val instanceof Double) {
//            original.add(name, (Double) val);
//        } else if (val instanceof Float) {
//            original.add(name, (Float) val);
//        } else if (val instanceof String) {
//            original.add(name, (String) val);
//        } else if (val instanceof Boolean) {
//            original.add(name, (Boolean) val);
//        } else if (val instanceof V8Value) {
//            original.add(name, (V8Value) val);
//        } else {
//            throw new UnsupportedOperationException("不支持的操作");
//        }
//        // parameters.release();
//    }
//
//    @Override
//    public int size() {
//        if (original instanceof V8Array) {
//            return ((V8Array) original).length();
//        }
//        String[] keys = original.getKeys();
//        return keys == null ? 0 : keys.length;
//    }
//}
