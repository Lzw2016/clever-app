package org.clever.js.v8.utils;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.IV8ValueObject;
import com.caoccao.javet.values.reference.V8ValueArray;
import lombok.SneakyThrows;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:40 <br/>
 */
public class ScriptEngineUtils {
    /**
     * 新建一个js 普通对象
     */
    @SneakyThrows
    public static IV8ValueObject newObject(V8Runtime v8) {
        Assert.notNull(v8, "参数v8不能为空");
        return v8.createV8ValueObject();
    }

    /**
     * 新建一个js 数组对象
     */
    @SneakyThrows
    public static IV8ValueObject newArray(V8Runtime v8, Object... args) {
        Assert.notNull(v8, "参数v8不能为空");
        V8ValueArray array = v8.createV8ValueArray();
        if (args != null) {
            array.push(args);
        }
        return array;
    }

    /**
     * 解析Json成为 Value 对象
     */
    @SneakyThrows
    public static IV8ValueObject parseJson(V8Runtime v8, String json) {
        Assert.notNull(v8, "参数v8不能为空");
        return v8.getExecutor(String.format("( %s );", json)).execute();
    }
}
