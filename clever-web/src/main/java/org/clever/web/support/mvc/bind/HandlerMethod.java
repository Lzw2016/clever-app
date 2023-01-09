package org.clever.web.support.mvc.bind;

import lombok.Data;
import org.clever.core.MethodParameter;

import java.lang.reflect.Method;

/**
 * mvc请求处理器信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 15:54 <br/>
 */
@Data
public class HandlerMethod {
    /**
     * 匹配的url path
     */
    private final String matcherPath;
    /**
     * 处理请求程序的class
     */
    private final Class<?> handlerClass;
    /**
     * 处理请求程序的method
     */
    private final Method method;
    /**
     * 处理请求程序method的参数类型
     */
    private final MethodParameter[] parameters;

//    private HandlerMethod resolvedFromHandlerMethod;
//    private volatile List<Annotation[][]> interfaceParameterAnnotations;

    public HandlerMethod(String matcherPath, Class<?> handlerClass, Method method, MethodParameter[] parameters) {
        this.matcherPath = matcherPath;
        this.handlerClass = handlerClass;
        this.method = method;
        this.parameters = parameters;
    }
}
