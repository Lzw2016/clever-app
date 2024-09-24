package org.clever.web.mvc.argument;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.Assert;
import org.clever.web.mvc.annotation.RequestBody;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;

import java.io.InputStream;
import java.lang.reflect.Type;


/**
 * 解析使用 {@code @RequestBody} 注释的方法参数并处理来自使用 {@code @ResponseBody} 注释的方法的返回值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/06 11:20 <br/>
 */
public class RequestBodyMethodProcessor implements HandlerMethodArgumentResolver {
    private final ObjectMapper objectMapper;

    public RequestBodyMethodProcessor(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "参数 objectMapper 不能为 null");
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    /**
     * 如果验证失败，则引发MethodArgumentNotValidException
     *
     * @throws HttpMessageNotReadableException 如果 {@link RequestBody#required()} 是 {@code true} 并且没有内容或者没有合适的转换器来读取内容
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        parameter = parameter.nestedIfOptional();
        HttpInputMessage httpInputMessage = new ServletServerHttpRequest(request);
        InputStream body = httpInputMessage.getBody();
        Type targetType = parameter.getNestedGenericParameterType();
        Class<?> contextClass = parameter.getContainingClass();
        JavaType javaType = objectMapper.constructType(GenericTypeResolver.resolveType(targetType, contextClass));
        Object arg = objectMapper.readValue(body, javaType);
        if (arg == null && checkRequired(parameter)) {
            throw new HttpMessageNotReadableException("Required request body is missing: " + parameter.getExecutable().toGenericString(), httpInputMessage);
        }
        return HandlerMethodArgumentResolver.adaptArgumentIfNecessary(arg, parameter);
    }

    protected boolean checkRequired(MethodParameter parameter) {
        RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
        return (requestBody != null && requestBody.required() && !parameter.isOptional());
    }
}
