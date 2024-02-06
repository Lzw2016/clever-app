package org.clever.js.graaljs;

import org.clever.js.api.AbstractScriptObject;
import org.clever.js.api.ScriptEngineContext;
import org.clever.util.Assert;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:58 <br/>
 */
public class GraalScriptObject extends AbstractScriptObject<Context, Value> {

    public GraalScriptObject(ScriptEngineContext<Context, Value> engineContext, Value original) {
        super(engineContext, original);
    }

    @Override
    public Collection<String> getMemberNames() {
        return original.getMemberKeys();
    }

    @Override
    public Collection<Object> getMembers() {
        List<Object> list = new ArrayList<>(size());
        if (original.hasArrayElements()) {
            for (int i = 0; i < original.getArraySize(); i++) {
                list.add(original.getArrayElement(i));
            }
        } else {
            for (String memberKey : original.getMemberKeys()) {
                list.add(original.getMember(memberKey));
            }
        }
        return list;
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
        original.putMember(name, value);
    }

    @Override
    public void delMember(String name) {
        original.removeMember(name);
    }

    @Override
    public Value callMember(String functionName, Object... args) {
        Assert.isTrue(original.canInvokeMember(functionName), "对象属性不能执行,functionName=" + functionName);
        Context engine = engineContext.getEngine();
        Value res;
        try {
            engine.enter();
            res = original.invokeMember(functionName, args);
        } finally {
            if (engine != null) {
                engine.leave();
            }
        }
        return res;
    }

    @Override
    public boolean canExecute() {
        return original.canExecute();
    }

    @Override
    public Value execute(Object... args) {
        Assert.isTrue(original.canExecute(), "当前脚本对象不能执行");
        Context engine = engineContext.getEngine();
        Value res;
        try {
            engine.enter();
            res = original.execute(args);
        } finally {
            if (engine != null) {
                engine.leave();
            }
        }
        return res;
    }

    @Override
    public void executeVoid(Object... args) {
        Assert.isTrue(original.canExecute(), "当前脚本对象不能执行");
        Context engine = engineContext.getEngine();
        try {
            engine.enter();
            original.executeVoid(args);
        } finally {
            if (engine != null) {
                engine.leave();
            }
        }
    }

    @Override
    public int size() {
        if (original.hasArrayElements()) {
            long size = original.getArraySize();
            if (size > Integer.MAX_VALUE) {
                throw new ClassCastException("数组 length=" + size + " 太长(超出范围)");
            }
            return (int) size;
        }
        return original.getMemberKeys().size();
    }
}
