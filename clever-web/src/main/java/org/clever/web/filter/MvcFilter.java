package org.clever.web.filter;

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
import org.clever.web.config.MvcConfig;
import org.clever.web.http.HttpMethod;
import org.clever.web.support.mvc.*;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/06 13:29 <br/>
 */
@Slf4j
public class MvcFilter implements FilterRegistrar.FilterFuc {
    public static MvcFilter create(String rootPath, MvcConfig mvcConfig) {
        return new MvcFilter(rootPath, mvcConfig);
    }

    public static MvcFilter create(String rootPath, Environment environment) {
        MvcConfig mvcConfig = Binder.get(environment).bind(MvcConfig.PREFIX, MvcConfig.class).orElseGet(MvcConfig::new);
        fixedLocations(rootPath, mvcConfig);
        MvcConfig.HotReload hotReload = Optional.of(mvcConfig.getHotReload()).orElse(new MvcConfig.HotReload());
        mvcConfig.setHotReload(hotReload);
        Map<String, String> locationMap = fixedLocations(rootPath, mvcConfig);
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

    private static Map<String, String> fixedLocations(String rootPath, MvcConfig mvcConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为 null");
        Map<String, String> locationMap = new LinkedHashMap<>();
        MvcConfig.HotReload hotReload = mvcConfig.getHotReload();
        if (hotReload != null && hotReload.getLocations() != null) {
            for (String location : hotReload.getLocations()) {
                locationMap.put(location, ResourcePathUtils.getAbsolutePath(rootPath, location));
            }
        }
        return locationMap;
    }

    private static final Object[] EMPTY_ARGS = new Object[0];

    // 保存 JavalinConfig.inner.appAttributes
//    private Map<String, Object> appAttributes = Collections.emptyMap();
    private final MvcConfig mvcConfig;
    private final Map<String, String> locationMap;
    @Getter
    private HandlerMethodResolver handlerMethodResolver;
    private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
    private final List<HandlerInterceptor> interceptors = new ArrayList<>();

    public MvcFilter(String rootPath, MvcConfig mvcConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为 null");
        this.locationMap = Collections.unmodifiableMap(fixedLocations(rootPath, mvcConfig));
        this.mvcConfig = mvcConfig;
        this.handlerMethodResolver = new DefaultHandlerMethodResolver(mvcConfig.getHotReload());
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
                // TODO 上抛异常
            }
            if (returnValue != null && !ctx.res.isCommitted()) {
                // TODO 响应客户端数据
//                response.setContentType(CONTENT_TYPE);
//                String json = serializeRes(result);
//                response.getWriter().println(json);
            }
        } catch (Exception e) {
            // TODO 异常类型
            throw new RuntimeException(e);
        }
    }

    protected Object[] getMethodArgumentValues(FilterRegistrar.Context ctx, HandlerMethod handlerMethod) throws Exception {
        Object[] args = EMPTY_ARGS;
        MethodParameter[] parameters = handlerMethod.getParameters();
        if (!ObjectUtils.isEmpty(parameters)) {
            args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodParameter parameter = parameters[i];
//                // TODO 从IOC容器中获取参数值(实现一个 HandlerMethodArgumentResolver???)
//                String parameterName = parameter.getParameterName();
//                args[i] = AppContextHolder.getBean(parameterName, parameter.getParameterType(), false);
//                if (args[i] != null && !parameter.getParameterType().isInstance(args[i])) {
//                    args[i] = AppContextHolder.getBean(parameterName, false);
//                    if (args[i] != null && !parameter.getParameterType().isInstance(args[i])) {
//                        args[i] = AppContextHolder.getBean(parameter.getParameterType(), false);
//                        if (args[i] != null && !parameter.getParameterType().isInstance(args[i])) {
//                            args[i] = null;
//                        }
//                    }
//                }
                for (HandlerMethodArgumentResolver argumentResolver : argumentResolvers) {
                    if (!argumentResolver.supportsParameter(parameter, ctx.req)) {
                        continue;
                    }
                    args[i] = argumentResolver.resolveArgument(parameter, ctx.req);
                    break;
                }
            }
        }
        return args;
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
//        Context context = new Context(ctx.req, ctx.res, appAttributes);
//        // 参考 io.javalin.http.util.ContextUtil#update
//        // String requestUri = StringUtils.removeStart(ctx.req.getRequestURI(), ctx.req.getContextPath());
//        ReflectionsUtils.setFieldValue(ctx, "matchedPath", pathSpec);
//        // ReflectionsUtils.setFieldValue(ctx, "pathParamMap", Collections.emptyMap());
//        ReflectionsUtils.setFieldValue(ctx, "handlerType", HandlerType.Companion.fromServletRequest(ctx.req));
//        ReflectionsUtils.setFieldValue(ctx, "endpointHandlerPath", pathSpec);

//        Method method = getBridgedMethod();
//        try {
//            if (KotlinDetector.isSuspendingFunction(method)) {
//                return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
//            }
//            return method.invoke(getBean(), args);
//        } catch (IllegalArgumentException ex) {
//            assertTargetBean(method, getBean(), args);
//            String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
//            throw new IllegalStateException(formatInvokeError(text, args), ex);
//        } catch (InvocationTargetException ex) {
//            // Unwrap for HandlerExceptionResolvers ...
//            Throwable targetException = ex.getTargetException();
//            if (targetException instanceof RuntimeException) {
//                throw (RuntimeException) targetException;
//            } else if (targetException instanceof Error) {
//                throw (Error) targetException;
//            } else if (targetException instanceof Exception) {
//                throw (Exception) targetException;
//            } else {
//                throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
//            }
//        }
        return null;
    }

    public void setHandlerMethodResolver(HandlerMethodResolver handlerMethodResolver) {
        Assert.notNull(handlerMethodResolver, "参数 handlerMethodResolver 不能为 null");
        this.handlerMethodResolver = handlerMethodResolver;
    }

    public void addArgumentResolver(HandlerMethodArgumentResolver argumentResolver) {
        Assert.notNull(argumentResolver, "参数 argumentResolver 不能为 null");
        argumentResolvers.add(argumentResolver);
        argumentResolvers.sort(Comparator.comparingDouble(HandlerMethodArgumentResolver::getOrder));
    }

    public void addInterceptor(HandlerInterceptor interceptor) {
        Assert.notNull(interceptor, "参数 interceptor 不能为 null");
        interceptors.add(interceptor);
        interceptors.sort(Comparator.comparingDouble(HandlerInterceptor::getOrder));
    }
}
