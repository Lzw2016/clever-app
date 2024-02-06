package org.clever.js.graaljs.utils;

import org.clever.js.api.GlobalConstant;
import org.clever.js.graaljs.GraalConstant;
import org.clever.util.Assert;
import org.graalvm.polyglot.*;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:59 <br/>
 */
public class ScriptEngineUtils {
    /**
     * Context 默认选项
     */
    public static final Map<String, String> Context_Default_Options = new HashMap<String, String>() {{
        put("js.ecmascript-version", GraalConstant.ECMAScript_Version);
        // "js.nashorn-compat", "true", // EXPERIMENTAL | js.nashorn-compat -> 实验性特性需要删除
        // "js.experimental-foreign-object-prototype", "true" // 实验性特性需要删除
    }};

    private static final Source Object_Constructor_Source = Source.newBuilder(GraalConstant.Js_Language_Id, "Object", "Unnamed").cached(true).buildLiteral();
    private static final Source Array_Constructor_Source = Source.newBuilder(GraalConstant.Js_Language_Id, "Array", "Unnamed").cached(true).buildLiteral();
    private static final Source Json_Constructor_Source = Source.newBuilder(GraalConstant.Js_Language_Id, "JSON", "Unnamed").cached(true).buildLiteral();

    /**
     * 创建 Context.Builder
     *
     * @param engine Engine对象
     * @param custom 自定义 Context 逻辑(可选参数)
     */
    public static Context.Builder createContextBuilder(Engine engine, Consumer<Context.Builder> custom) {
        Assert.notNull(engine, "参数 engine 不能为 null");
        Context.Builder builder = Context.newBuilder(GraalConstant.Js_Language_Id)
            .engine(engine)
            .options(Context_Default_Options)
            // 设置时间时区
            .timeZone(ZoneId.of("Asia/Shanghai"))
            // 不允许使用实验特性
            .allowExperimentalOptions(false)
            // 不允许多语言访问
            .allowPolyglotAccess(PolyglotAccess.NONE)
            // 默认允许所有行为
            .allowAllAccess(true)
            // 不允许JavaScript创建进程
            .allowCreateProcess(false)
            // 不允许JavaScript创建线程
            .allowCreateThread(false)
            // 不允许JavaScript访问环境变量
            .allowEnvironmentAccess(EnvironmentAccess.NONE)
            // 不允许JavaScript对主机的IO操作
            .allowIO(false)
            // 不允许JavaScript访问本机接口
            .allowNativeAccess(false)
            // 不允许JavaScript加载Class
            .allowHostClassLoading(false)
            // 定义JavaScript可以加载的Class
            // .allowHostClassLookup()
            // 定义JavaScript可以访问的Class
            // .allowHostAccess(HostAccess.ALL)
            // 限制JavaScript的资源使用(CPU)
            // .resourceLimits()
            ;
        if (custom != null) {
            custom.accept(builder);
        }
        return builder;
    }

    /**
     * 创建 HostAccess.Builder
     *
     * @param custom 自定义 HostAccess 逻辑(可选参数)
     */
    public static HostAccess.Builder createHostAccessBuilder(Consumer<HostAccess.Builder> custom) {
        // 沙箱环境控制 - 定义JavaScript可以访问的Class(使用黑名单机制)
        HostAccess.Builder builder = HostAccess.newBuilder();
        builder.allowArrayAccess(true);
        builder.allowListAccess(true);
        builder.allowMapAccess(true);
        builder.allowIterableAccess(true);
        builder.allowIteratorAccess(true);
        builder.allowPublicAccess(true);
        builder.allowAllImplementations(true);
        builder.allowAllClassImplementations(true);
        builder.allowBufferAccess(true);
        addDenyAccess(builder, GlobalConstant.Default_Deny_Access_Class);
        // for (Field field : aClass.getFields()) {
        //     builder.allowAccess(field);
        // }
        // for (Method method : aClass.getMethods()) {
        //     builder.allowAccess(method);
        // }
        if (custom != null) {
            custom.accept(builder);
        }
        return builder;
    }

    /**
     * 定义JavaScript不允许访问的Class
     */
    public static void addDenyAccess(HostAccess.Builder builder, Set<Class<?>> denyAccessClass) {
        for (Class<?> aClass : denyAccessClass) {
            if (aClass == null) {
                continue;
            }
            builder.denyAccess(aClass);
        }
    }

    /**
     * 创建一个新的 Context
     *
     * @param engine        Engine对象
     * @param customContext 自定义 Context 逻辑(可选参数)
     * @param hostAccess    HostAccess对象
     */
    public static Context creatEngine(Engine engine, Consumer<Context.Builder> customContext, HostAccess hostAccess) {
        Context.Builder contextBuilder = createContextBuilder(engine, customContext);
        // 沙箱环境控制 - 定义JavaScript可以访问的Class(使用黑名单机制)
        contextBuilder.allowHostAccess(hostAccess);
        // 沙箱环境控制 - 限制JavaScript的资源使用
        // ResourceLimits resourceLimits = ResourceLimits.newBuilder().statementLimit()
        return contextBuilder.build();
    }

    /**
     * 创建一个新的 Context
     *
     * @param engine           Engine对象
     * @param customContext    自定义 Context 逻辑(可选参数)
     * @param customHostAccess 自定义 HostAccess 逻辑(可选参数)
     */
    public static Context creatEngine(Engine engine, Consumer<Context.Builder> customContext, Consumer<HostAccess.Builder> customHostAccess) {
        return creatEngine(engine, customContext, createHostAccessBuilder(customHostAccess).build());
    }

    /**
     * 新建一个js 普通对象
     */
    public static Value newObject(Context context, Object... args) {
        Assert.notNull(context, "参数 context 不能为 null");
        Value constructor;
        try {
            context.enter();
            constructor = context.eval(Object_Constructor_Source);
        } finally {
            context.leave();
        }
        return constructor.newInstance(args);
    }

    /**
     * 新建一个js 普通对象
     */
    public static Value newObject(Object... args) {
        Context context = Context.getCurrent();
        Assert.notNull(context, "参数 context 不能为 null");
        Value constructor = context.eval(Object_Constructor_Source);
        return constructor.newInstance(args);
    }

    /**
     * 新建一个js 数组对象
     */
    public static Value newArray(Context context, Object... args) {
        Assert.notNull(context, "参数 context 不能为 null");
        Value constructor;
        try {
            context.enter();
            constructor = context.eval(Array_Constructor_Source);
        } finally {
            context.leave();
        }
        return constructor.newInstance(args);
    }

    /**
     * 新建一个js 数组对象
     */
    public static Value newArray(Object... args) {
        Context context = Context.getCurrent();
        Assert.notNull(context, "参数 context 不能为 null");
        Value constructor = context.eval(Array_Constructor_Source);
        return constructor.newInstance(args);
    }

    /**
     * 解析Json成为 Value 对象
     */
    public static Value parseJson(Context context, String json) {
        Assert.notNull(context, "参数 context 不能为 null");
        Value constructor;
        try {
            context.enter();
            constructor = context.eval(Json_Constructor_Source);
        } finally {
            context.leave();
        }
        return constructor.invokeMember("parse", json);
    }
}
