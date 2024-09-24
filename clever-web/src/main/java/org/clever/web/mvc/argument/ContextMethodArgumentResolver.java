//package org.clever.web.support.mvc.argument;
//
//import io.javalin.http.Context;
//import io.javalin.http.HandlerType;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.clever.core.reflection.ReflectionsUtils;
//import org.springframework.core.MethodParameter;
//
//import java.util.Collections;
//import java.util.Map;
//
///**
// * 解析类型为 {@link Context} 的参数
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/14 13:22 <br/>
// */
//public class ContextMethodArgumentResolver implements HandlerMethodArgumentResolver {
//    /**
//     * 保存 JavalinConfig.inner.appAttributes
//     */
//    private final Map<String, Object> appAttributes;
//
//    /**
//     * @param appAttributes JavalinConfig.inner.appAttributes
//     */
//    public ContextMethodArgumentResolver(Map<String, Object> appAttributes) {
//        super();
//        this.appAttributes = appAttributes;
//    }
//
//    @Override
//    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
//        return Context.class.isAssignableFrom(parameter.getParameterType());
//    }
//
//    @Override
//    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
//        Context context = new Context(request, response, appAttributes);
//        // 参考 io.javalin.http.util.ContextUtil#update
//        String pathSpec = request.getPathInfo();
//        ReflectionsUtils.setFieldValue(context, "matchedPath", pathSpec);
//        ReflectionsUtils.setFieldValue(context, "pathParamMap", Collections.emptyMap());
//        ReflectionsUtils.setFieldValue(context, "handlerType", HandlerType.Companion.fromServletRequest(request));
//        ReflectionsUtils.setFieldValue(context, "endpointHandlerPath", pathSpec);
//        return context;
//    }
//}
