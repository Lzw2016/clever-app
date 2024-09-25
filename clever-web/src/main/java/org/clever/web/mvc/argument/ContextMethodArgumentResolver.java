package org.clever.web.mvc.argument;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.Assert;
import org.springframework.core.MethodParameter;

/**
 * 解析类型为 {@link Context} 的参数
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/14 13:22 <br/>
 */
public class ContextMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private final JavalinConfig javalinConfig;

    public ContextMethodArgumentResolver(JavalinConfig config) {
        super();
        Assert.notNull(config, "参数 config 不能为 null");
        this.javalinConfig = config;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        return Context.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return new org.clever.web.Context(request, response, javalinConfig);
    }
}
