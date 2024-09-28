package org.clever.js.graaljs.utils;

import org.clever.core.Assert;
import org.clever.js.api.GlobalConstant;
import org.clever.js.graaljs.GraalConstant;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

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
    public static final Map<String, String> CONTEXT_DEFAULT_OPTIONS = new HashMap<>() {{
        // put("js.ecmascript-version", GraalConstant.ECMASCRIPT_VERSION);
        // "js.nashorn-compat", "true", // EXPERIMENTAL | js.nashorn-compat -> 实验性特性需要删除
        // "js.experimental-foreign-object-prototype", "true" // 实验性特性需要删除
    }};

    private static final Source OBJECT_CONSTRUCTOR_SOURCE = Source.newBuilder(GraalConstant.JS_LANGUAGE_ID, "Object", "Unnamed").cached(true).buildLiteral();
    private static final Source ARRAY_CONSTRUCTOR_SOURCE = Source.newBuilder(GraalConstant.JS_LANGUAGE_ID, "Array", "Unnamed").cached(true).buildLiteral();
    private static final Source JSON_CONSTRUCTOR_SOURCE = Source.newBuilder(GraalConstant.JS_LANGUAGE_ID, "JSON", "Unnamed").cached(true).buildLiteral();

    /**
     * 创建 Context.Builder
     *
     * @param graalvmEngine Engine对象
     * @param custom        自定义 Context 逻辑(可选参数)
     */
    public static Context.Builder createContextBuilder(Engine graalvmEngine, Consumer<Context.Builder> custom) {
        Assert.notNull(graalvmEngine, "参数 graalvmEngine 不能为 null");
        Context.Builder builder = Context.newBuilder(GraalConstant.JS_LANGUAGE_ID)
            .engine(graalvmEngine)
            .options(CONTEXT_DEFAULT_OPTIONS)
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
            .allowIO(IOAccess.NONE)
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
        addDenyAccess(builder, GlobalConstant.DEFAULT_DENY_ACCESS_CLASS);
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
     * @param graalvmEngine Engine对象
     * @param customContext 自定义 Context 逻辑(可选参数)
     * @param hostAccess    HostAccess对象
     */
    public static Context creatEngine(Engine graalvmEngine, Consumer<Context.Builder> customContext, HostAccess hostAccess) {
        Context.Builder contextBuilder = createContextBuilder(graalvmEngine, customContext);
        // 沙箱环境控制 - 定义JavaScript可以访问的Class(使用黑名单机制)
        contextBuilder.allowHostAccess(hostAccess);
        // 沙箱环境控制 - 限制JavaScript的资源使用
        // ResourceLimits resourceLimits = ResourceLimits.newBuilder().statementLimit()
        return contextBuilder.build();
    }

    /**
     * 创建一个新的 Context
     *
     * @param graalvmEngine    Engine对象
     * @param customContext    自定义 Context 逻辑(可选参数)
     * @param customHostAccess 自定义 HostAccess 逻辑(可选参数)
     */
    public static Context creatEngine(Engine graalvmEngine, Consumer<Context.Builder> customContext, Consumer<HostAccess.Builder> customHostAccess) {
        return creatEngine(graalvmEngine, customContext, createHostAccessBuilder(customHostAccess).build());
    }

    /**
     * 新建一个js 普通对象
     */
    public static Value newObject(Context context, Object... args) {
        Assert.notNull(context, "参数 context 不能为 null");
        Value constructor;
        try {
            context.enter();
            constructor = context.eval(OBJECT_CONSTRUCTOR_SOURCE);
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
        Value constructor = context.eval(OBJECT_CONSTRUCTOR_SOURCE);
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
            constructor = context.eval(ARRAY_CONSTRUCTOR_SOURCE);
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
        Value constructor = context.eval(ARRAY_CONSTRUCTOR_SOURCE);
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
            constructor = context.eval(JSON_CONSTRUCTOR_SOURCE);
        } finally {
            context.leave();
        }
        return constructor.invokeMember("parse", json);
    }
}
