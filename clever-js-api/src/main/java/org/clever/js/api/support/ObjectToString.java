package org.clever.js.api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.clever.core.StrFormatter;
import org.clever.core.mapper.JacksonMapper;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.Module;
import org.clever.js.api.module.ModuleCache;
import org.clever.js.api.require.Require;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 11:42 <br/>
 */
public class ObjectToString {
    static {
        // 注册 Module
        final ObjectMapper mapper = JacksonMapper.getInstance().getMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ModuleCache.class, ToStringSerializer.instance);
        module.addSerializer(Module.class, ToStringSerializer.instance);
        module.addSerializer(CompileModule.class, ToStringSerializer.instance);
        module.addSerializer(Require.class, ToStringSerializer.instance);
        module.addSerializer(Folder.class, ToStringSerializer.instance);
        // module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        // module.addSerializer(Long.class, ToStringSerializer.instance);
        // module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModules(module);
    }

    public static final ObjectToString INSTANCE = new ObjectToString();

    /**
     * 格式化字符串,类似log4j日志实现<br>
     * 此方法只是简单将占位符"{}"按照顺序替换为参数<br>
     * 如果想输出"{}"使用"\\"转义"{"即可，如果想输出"{}"之前的"\"使用双转义符"\\\\"即可<br>
     * <pre>
     *  format("this is {} for {}", "a", "b")       -> this is a for b
     *  format("this is \\{} for {}", "a", "b")     -> this is \{} for a
     *  format("this is \\\\{} for {}", "a", "b")   -> this is \a for b
     * </pre>
     *
     * @param strPattern 字符串模板
     * @param argArray   参数列表
     */
    public String format(final String strPattern, final Object... argArray) {
        return StrFormatter.format(strPattern, argArray);
    }

    /**
     * 数组或集合转String
     *
     * @param obj 集合或数组对象
     * @return 数组字符串，与集合转字符串格式相同
     */
    public String toString(Object obj) {
        return StrFormatter.toString(obj);
    }
}
