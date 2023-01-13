package org.clever.web.support.mvc.argument;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.clever.core.GenericTypeResolver;
import org.clever.core.MethodParameter;
import org.clever.util.Assert;
import org.clever.web.exception.HttpMessageNotReadableException;
import org.clever.web.http.HttpMethod;
import org.clever.web.http.MediaType;
import org.clever.web.support.mvc.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;


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
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request) throws Exception {
        parameter = parameter.nestedIfOptional();
        InputStream body = getBody(request);
        Type targetType = parameter.getNestedGenericParameterType();
        Class<?> contextClass = parameter.getContainingClass();
        JavaType javaType = objectMapper.constructType(GenericTypeResolver.resolveType(targetType, contextClass));
        Object arg = objectMapper.readValue(body, javaType);
        if (arg == null && checkRequired(parameter)) {
            throw new HttpMessageNotReadableException("Required request body is missing: " + parameter.getExecutable().toGenericString());
        }
        return adaptArgumentIfNecessary(arg, parameter);
    }

    protected Object adaptArgumentIfNecessary(Object arg, MethodParameter parameter) {
        if (parameter.getParameterType() == Optional.class) {
            if (arg == null
                    || (arg instanceof Collection && ((Collection<?>) arg).isEmpty())
                    || (arg instanceof Object[] && ((Object[]) arg).length == 0)) {
                return Optional.empty();
            } else {
                return Optional.of(arg);
            }
        }
        return arg;
    }

    public InputStream getBody(HttpServletRequest request) throws IOException {
        if (isFormPost(request)) {
            return getBodyFromServletRequestParameters(request);
        } else {
            return request.getInputStream();
        }
    }

    private boolean isFormPost(HttpServletRequest request) {
        String contentType = request.getContentType();
        return (contentType != null
                && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                && HttpMethod.POST.matches(request.getMethod()));
    }

    /**
     * 使用 {@link javax.servlet.ServletRequest#getParameterMap()} 重建表单“POST”的主体，
     * 提供可预测的结果，而不是从主体读取，如果任何其他代码使用 ServletRequest 访问参数，
     * 这可能会失败，从而导致输入流被“消耗”。
     */
    private InputStream getBodyFromServletRequestParameters(HttpServletRequest request) throws IOException {
        final Charset FORM_CHARSET = StandardCharsets.UTF_8;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        Writer writer = new OutputStreamWriter(bos, FORM_CHARSET);
        Map<String, String[]> form = request.getParameterMap();
        for (Iterator<Map.Entry<String, String[]>> entryIterator = form.entrySet().iterator(); entryIterator.hasNext(); ) {
            Map.Entry<String, String[]> entry = entryIterator.next();
            String name = entry.getKey();
            List<String> values = Arrays.asList(entry.getValue());
            for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
                String value = valueIterator.next();
                writer.write(URLEncoder.encode(name, FORM_CHARSET.name()));
                if (value != null) {
                    writer.write('=');
                    writer.write(URLEncoder.encode(value, FORM_CHARSET.name()));
                    if (valueIterator.hasNext()) {
                        writer.write('&');
                    }
                }
            }
            if (entryIterator.hasNext()) {
                writer.append('&');
            }
        }
        writer.flush();
        return new ByteArrayInputStream(bos.toByteArray());
    }

    protected boolean checkRequired(MethodParameter parameter) {
        RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
        return (requestBody != null && requestBody.required() && !parameter.isOptional());
    }
}
