package org.clever.js.v8;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.IV8ValueArray;
import com.caoccao.javet.values.reference.IV8ValueObject;
import lombok.SneakyThrows;
import org.clever.js.api.AbstractScriptObject;
import org.clever.js.api.ScriptEngineContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:40 <br/>
 */
public class V8ScriptObject extends AbstractScriptObject<V8Runtime, IV8ValueObject> {

    public V8ScriptObject(ScriptEngineContext<V8Runtime, IV8ValueObject> context, IV8ValueObject original) {
        super(context, original);
    }

    @SneakyThrows
    @Override
    public Collection<String> getMemberNames() {
        return original.getOwnPropertyNameStrings();
    }

    @SneakyThrows
    @Override
    public Object getMember(String name) {
        return original.get(name);
    }

    @SneakyThrows
    @Override
    public boolean hasMember(String name) {
        return original.has(name);
    }

    @SneakyThrows
    @Override
    public Collection<Object> getMembers() {
        List<String> keys = original.getOwnPropertyNameStrings();
        if (keys == null) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>(keys.size());
        for (String key : keys) {
            list.add(original.get(key));
        }
        return list;
    }

    @SneakyThrows
    @Override
    public Object callMember(String functionName, Object... args) {
        return original.invoke(functionName, args);
    }

    @SneakyThrows
    @Override
    public void delMember(String name) {
        original.delete(name);
    }

    @SneakyThrows
    @Override
    public void setMember(String name, Object value) {
        original.set(name, value);
    }

    @SneakyThrows
    @Override
    public int size() {
        if (original instanceof IV8ValueArray) {
            return ((IV8ValueArray) original).getLength();
        }
        try (IV8ValueArray names = original.getOwnPropertyNames()) {
            return names.getLength();
        }
    }
}
