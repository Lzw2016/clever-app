package org.clever.web.support.mvc.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;

import java.security.Principal;

/**
 * 解析类型为 {@link Principal} 的参数。这样做是为了启用 {@link Principal} 参数的自定义参数解析（带有自定义注释）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/14 10:08 <br/>
 */
public class PrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        return Principal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Principal principal = request.getUserPrincipal();
        if (principal != null && !parameter.getParameterType().isInstance(principal)) {
            throw new IllegalStateException(
                "Current user principal is not of type [" + parameter.getParameterType().getName() + "]: " + principal
            );
        }
        return principal;
    }
}
