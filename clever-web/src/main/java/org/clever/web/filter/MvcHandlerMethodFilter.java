package org.clever.web.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.clever.core.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.MvcConfig;
import org.clever.web.mvc.HandlerMethod;
import org.clever.web.mvc.method.HandlerMethodResolver;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.Objects;

/**
 * 解析MVC规则的HandlerMethod过滤器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/23 20:32 <br/>
 */
@Getter
public class MvcHandlerMethodFilter implements FilterRegistrar.FilterFuc {
    private final static String IS_MVC_HANDLE_ATTRIBUTE = MvcHandlerMethodFilter.class.getName() + "_Is_Mvc_Handle";
    private final static String HANDLER_METHOD_ATTRIBUTE = MvcHandlerMethodFilter.class.getName() + "_Handler_Method";
    private final static String HANDLER_METHOD_EXCEPTION_ATTRIBUTE = MvcHandlerMethodFilter.class.getName() + "_Handler_Method_Exception";

    /**
     * 当前请求是否需要MVC处理
     */
    public static boolean isMvcHandle(HttpServletRequest request) {
        return Objects.equals(request.getAttribute(IS_MVC_HANDLE_ATTRIBUTE), true);
    }

    /**
     * 获取当前请求对应的 HandlerMethod, 并检查是否存现异常
     *
     * @return 不存在返回 null
     * @throws Throwable 如果解析 HandlerMethod 时出现异常则抛出异常
     */
    public static HandlerMethod getHandleMethodAndCheckError(HttpServletRequest request) throws Throwable {
        Object exception = request.getAttribute(HANDLER_METHOD_EXCEPTION_ATTRIBUTE);
        if (exception instanceof Throwable) {
            throw (Throwable) exception;
        }
        return getHandleMethod(request);
    }

    /**
     * 获取当前请求对应的 HandlerMethod
     *
     * @return 解析失败或不存在返回 null
     */
    public static HandlerMethod getHandleMethod(HttpServletRequest request) {
        Object handlerMethod = request.getAttribute(HANDLER_METHOD_ATTRIBUTE);
        return handlerMethod instanceof HandlerMethod ? (HandlerMethod) handlerMethod : null;
    }

    protected final MvcConfig mvcConfig;
    protected final HandlerMethodResolver handlerMethodResolver;

    MvcHandlerMethodFilter(MvcConfig mvcConfig, HandlerMethodResolver handlerMethodResolver) {
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为 null");
        Assert.notNull(handlerMethodResolver, "参数 handlerMethodResolver 不能为空");
        this.mvcConfig = mvcConfig;
        this.handlerMethodResolver = handlerMethodResolver;
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
        final HttpMethod httpMethod = HttpMethod.valueOf(ctx.req.getMethod());
        if (!reqPath.startsWith(mvcConfig.getPath()) || !mvcConfig.getHttpMethod().contains(httpMethod)) {
            ctx.next();
            return;
        }
        ctx.req.setAttribute(IS_MVC_HANDLE_ATTRIBUTE, true);
        // 获取 HandlerMethod
        HandlerMethod handlerMethod = null;
        try {
            handlerMethod = handlerMethodResolver.getHandleMethod(ctx.req, ctx.res, mvcConfig);
        } catch (Throwable e) {
            ctx.req.setAttribute(HANDLER_METHOD_EXCEPTION_ATTRIBUTE, e);
        }
        ctx.req.setAttribute(HANDLER_METHOD_ATTRIBUTE, handlerMethod);
        ctx.next();
    }
}
