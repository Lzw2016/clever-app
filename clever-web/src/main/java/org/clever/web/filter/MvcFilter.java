package org.clever.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.core.MethodParameter;
import org.clever.core.ResourcePathUtils;
import org.clever.core.env.Environment;
import org.clever.core.tuples.TupleTwo;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.web.FilterRegistrar;
import org.clever.web.JavalinAttrKey;
import org.clever.web.config.MvcConfig;
import org.clever.web.http.HttpMethod;
import org.clever.web.support.mvc.HandlerContext;
import org.clever.web.support.mvc.HandlerInterceptor;
import org.clever.web.support.mvc.HandlerMethod;
import org.clever.web.support.mvc.argument.*;
import org.clever.web.support.mvc.method.DefaultHandlerMethodResolver;
import org.clever.web.support.mvc.method.HandlerMethodResolver;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 定义MVC规则的Filter
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/06 13:29 <br/>
 */
@Slf4j
public class MvcFilter implements Plugin, FilterRegistrar.FilterFuc {
    public static MvcFilter create(String rootPath, MvcConfig mvcConfig) {
        return new MvcFilter(rootPath, mvcConfig);
    }

    public static MvcFilter create(String rootPath, Environment environment) {
        MvcConfig mvcConfig = Binder.get(environment).bind(MvcConfig.PREFIX, MvcConfig.class).orElseGet(MvcConfig::new);
        MvcConfig.HotReload hotReload = Optional.of(mvcConfig.getHotReload()).orElse(new MvcConfig.HotReload());
        mvcConfig.setHotReload(hotReload);
        Map<String, String> locationMap = ResourcePathUtils.getAbsolutePath(rootPath, hotReload.getLocations());
        AppContextHolder.registerBean("mvcConfig", mvcConfig, true);
        List<String> logs = new ArrayList<>();
        logs.add("mvc: ");
        logs.add("  enable           : " + mvcConfig.isEnable());
        logs.add("  path             : " + mvcConfig.getPath());
        logs.add("  httpMethod       : " + StringUtils.join(mvcConfig.getHttpMethod(), " | "));
        logs.add("  allowPackages    : " + StringUtils.join(mvcConfig.getAllowPackages(), " | "));
        logs.add("  packagePrefix    : " + mvcConfig.getPackagePrefix());
        logs.add("  hotReload: ");
        logs.add("    enable         : " + hotReload.isEnable());
        logs.add("    interval       : " + hotReload.getInterval().toMillis() + "ms");
        logs.add("    excludePackages: " + StringUtils.join(hotReload.getExcludePackages(), " | "));
        logs.add("    locations      : " + StringUtils.join(hotReload.getLocations().stream().map(locationMap::get).toArray(), " | "));
        BannerUtils.printConfig(log, "mvc配置", logs.toArray(new String[0]));
        return create(rootPath, mvcConfig);
    }

    /**
     * 当前 Javalin 实例
     */
    protected Javalin javalin;
    /**
     * 空的 HandlerMethod 参数
     */
    private static final Object[] EMPTY_ARGS = new Object[0];
    @Getter
    private final MvcConfig mvcConfig;
    @Getter
    private final Map<String, String> locationMap;
    @Getter
    private HandlerMethodResolver handlerMethodResolver;
    protected final List<HandlerMethodArgumentResolver> argumentResolvers = new CopyOnWriteArrayList<>();
    protected final List<HandlerInterceptor> interceptors = new CopyOnWriteArrayList<>();

    public MvcFilter(String rootPath, MvcConfig mvcConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为 null");
        MvcConfig.HotReload hotReload = Optional.of(mvcConfig.getHotReload()).orElse(new MvcConfig.HotReload());
        mvcConfig.setHotReload(hotReload);
        this.locationMap = Collections.unmodifiableMap(ResourcePathUtils.getAbsolutePath(rootPath, hotReload.getLocations()));
        this.mvcConfig = mvcConfig;
        this.handlerMethodResolver = new DefaultHandlerMethodResolver(hotReload, this.locationMap);
    }

    /**
     * 返回要使用的参数解析器列表
     */
    protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
        ObjectMapper objectMapper = javalin.attribute(JavalinAttrKey.JACKSON_OBJECT_MAPPER);
        // 设置默认的 HandlerMethodArgumentResolver
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(16);
        // Annotation-based argument resolution
        resolvers.add(new RequestParamMethodArgumentResolver(false));
        resolvers.add(new RequestParamMapMethodArgumentResolver());
        resolvers.add(new RequestBodyMethodProcessor(objectMapper));
        resolvers.add(new RequestPartMethodArgumentResolver(objectMapper));
        resolvers.add(new RequestHeaderMethodArgumentResolver());
        resolvers.add(new RequestHeaderMapMethodArgumentResolver());
        resolvers.add(new CookieValueMethodArgumentResolver());
        // Type-based argument resolution
        resolvers.add(new ServletRequestMethodArgumentResolver());
        resolvers.add(new ServletResponseMethodArgumentResolver());
        // Catch-all
        resolvers.add(new PrincipalMethodArgumentResolver());
        resolvers.add(new ContextMethodArgumentResolver(javalin._conf.inner.appAttributes));
        resolvers.add(new RequestParamMethodArgumentResolver(true));
        return resolvers;
    }

    @Override
    public void apply(@NotNull Javalin app) {
        this.javalin = app;
        this.argumentResolvers.addAll(this.getDefaultArgumentResolvers());
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        // 是否启用
        if (!mvcConfig.isEnable()) {
            ctx.next();
            return;
        }
        // 当前请求是否满足mvc拦截配置
        final String reqPath = ctx.req.getPathInfo();
        final HttpMethod httpMethod = HttpMethod.resolve(ctx.req.getMethod());
        if (!reqPath.startsWith(mvcConfig.getPath()) || !mvcConfig.getHttpMethod().contains(httpMethod)) {
            ctx.next();
            return;
        }
        try {
            // 获取 HandlerMethod
            final HandlerMethod handlerMethod = handlerMethodResolver.getHandleMethod(ctx.req, ctx.res, mvcConfig);
            if (handlerMethod == null) {
                ctx.next();
                return;
            }
            // 解析 HandlerMethod args
            final Object[] args = getMethodArgumentValues(ctx, handlerMethod);
            // 创建 HandlerContext
            final HandlerContext handlerContext = new HandlerContext(ctx.req, ctx.res, handlerMethod, args);
            // 执行 HandlerInterceptor
            TupleTwo<Object, Throwable> result = executeInterceptor(handlerContext);
            // 处理返回值
            Object returnValue = result.getValue1();
            Throwable exception = result.getValue2();
            if (exception != null) {
                throw exception;
            }
            if (returnValue != null && !ctx.res.isCommitted()) {
                // 响应客户端数据
                serializeRes(returnValue, ctx.req, ctx.res);
                ctx.res.flushBuffer();
            }
        } catch (Throwable e) {
            // TODO 异常类型
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析 HandlerMethod 的调用参数
     */
    protected Object[] getMethodArgumentValues(FilterRegistrar.Context ctx, HandlerMethod handlerMethod) throws Exception {
        Object[] args = EMPTY_ARGS;
        MethodParameter[] parameters = handlerMethod.getParameters();
        if (!ObjectUtils.isEmpty(parameters)) {
            args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodParameter parameter = parameters[i];
                args[i] = resolveArgumentFromIOC(parameter);
                if (args[i] != null) {
                    continue;
                }
                for (HandlerMethodArgumentResolver argumentResolver : argumentResolvers) {
                    if (!argumentResolver.supportsParameter(parameter, ctx.req)) {
                        continue;
                    }
                    args[i] = argumentResolver.resolveArgument(parameter, ctx.req, ctx.res);
                    break;
                }
            }
        }
        return args;
    }

    /**
     * 从IOC容器中获取参数值
     */
    protected Object resolveArgumentFromIOC(MethodParameter parameter) {
        Object arg;
        final String parameterName = parameter.getParameterName();
        final Class<?> parameterType = parameter.getParameterType();
        arg = AppContextHolder.getBean(parameterName, parameterType, false);
        if (arg == null) {
            arg = AppContextHolder.getBean(parameterName, false);
            if (!parameterType.isInstance(arg)) {
                arg = AppContextHolder.getBean(parameterType, false);
            }
        }
        return arg;
    }

    /**
     * 执行mvc拦截器 和
     *
     * @return {@code TupleTwo<Handler Method返回值, 出现的异常>}
     */
    protected TupleTwo<Object, Throwable> executeInterceptor(HandlerContext handlerContext) {
        boolean goOn = true;
        Object returnValue = null;
        Throwable exception = null;
        List<HandlerInterceptor> excInterceptor = new ArrayList<>(interceptors.size());
        // 执行 HandlerInterceptor-before
        for (HandlerInterceptor interceptor : interceptors) {
            try {
                excInterceptor.add(interceptor);
                goOn = interceptor.beforeHandle(handlerContext);
            } catch (Throwable e) {
                exception = e;
                break;
            }
            // 中断执行
            if (!goOn) {
                break;
            }
        }
        Collections.reverse(excInterceptor);
        // 异常处理
        if (exception != null) {
            triggerFinallyHandle(excInterceptor, handlerContext, null, exception);
            return TupleTwo.creat(null, exception);
        }
        // 执行 Handler Method | 未中断 & 未异常
        if (goOn) {
            try {
                returnValue = invokeHandlerMethod(handlerContext);
            } catch (Throwable e) {
                exception = e;
            }
        }
        // 异常处理
        if (exception != null) {
            triggerFinallyHandle(excInterceptor, handlerContext, null, exception);
            return TupleTwo.creat(null, exception);
        }
        // 执行 HandlerInterceptor-after
        final HandlerContext.After afterContext = new HandlerContext.After(handlerContext, returnValue);
        for (HandlerInterceptor interceptor : excInterceptor) {
            try {
                interceptor.afterHandle(afterContext);
            } catch (Throwable e) {
                exception = e;
                break;
            }
        }
        // 更新 Handler Method 返回值
        returnValue = afterContext.getResult();
        // 执行 HandlerInterceptor-finally
        triggerFinallyHandle(excInterceptor, handlerContext, returnValue, exception);
        return TupleTwo.creat(returnValue, exception);
    }

    /**
     * 执行 HandlerInterceptor-finally
     */
    protected void triggerFinallyHandle(List<HandlerInterceptor> interceptors, HandlerContext handlerContext, Object returnValue, Throwable exception) {
        final HandlerContext.Finally finallyContext = new HandlerContext.Finally(handlerContext, returnValue, exception);
        for (HandlerInterceptor interceptor : interceptors) {
            try {
                interceptor.finallyHandle(finallyContext);
            } catch (Throwable e) {
                log.error("HandlerInterceptor.afterCompletion threw exception", e);
            }
        }
    }

    /**
     * 执行 Handler Method
     */
    protected Object invokeHandlerMethod(HandlerContext handlerContext) throws Exception {
        final Method method = handlerContext.getHandleMethod().getMethod();
        final Object[] args = handlerContext.getArgs();
        try {
            // if (KotlinDetector.isSuspendingFunction(method)) {
            //     return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
            // }
            return method.invoke(null, args);
        } catch (IllegalArgumentException ex) {
            String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
            throw new IllegalStateException(formatInvokeError(text, method, args), ex);
        } catch (InvocationTargetException ex) {
            // Unwrap for HandlerExceptionResolvers ...
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else if (targetException instanceof Exception) {
                throw (Exception) targetException;
            } else {
                throw new IllegalStateException(formatInvokeError("Invocation failure", method, args), targetException);
            }
        }
    }

    /**
     * 格式化 HandlerMethod 调用错误信息
     */
    protected String formatInvokeError(String text, Method method, Object[] args) {
        String formattedArgs = IntStream.range(0, args.length)
                .mapToObj(i -> (
                        args[i] != null ?
                                "[" + i + "] [type=" + args[i].getClass().getName() + "] [value=" + args[i] + "]" :
                                "[" + i + "] [null]"
                )).collect(Collectors.joining(",\n", " ", " "));
        return text + "\n"
                + "Controller [" + method.getDeclaringClass().getName() + "]\n"
                + "Method [" + method.toGenericString() + "] " + "with argument values:\n"
                + formattedArgs;
    }

    /**
     *
     */
    protected void serializeRes(Object returnValue, HttpServletRequest request, HttpServletResponse response) {
//                response.setContentType(CONTENT_TYPE);
//                String json = serializeRes(result);
//                response.getWriter().println(json);
    }

    /**
     * 设置 HandleMethod 解析器
     */
    public void setHandlerMethodResolver(HandlerMethodResolver handlerMethodResolver) {
        Assert.notNull(handlerMethodResolver, "参数 handlerMethodResolver 不能为 null");
        this.handlerMethodResolver = handlerMethodResolver;
    }

    /**
     * 增加 mvc 参数解析器
     */
    public void addArgumentResolver(HandlerMethodArgumentResolver argumentResolver) {
        Assert.notNull(argumentResolver, "参数 argumentResolver 不能为 null");
        argumentResolvers.add(argumentResolver);
    }

    /**
     * 增加 mvc 拦截器
     */
    public void addInterceptor(HandlerInterceptor interceptor) {
        Assert.notNull(interceptor, "参数 interceptor 不能为 null");
        interceptors.add(interceptor);
        interceptors.sort(Comparator.comparingDouble(HandlerInterceptor::getOrder));
    }
}
