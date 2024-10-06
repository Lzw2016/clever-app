package org.clever.web.mvc.argument;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.clever.core.Assert;
import org.clever.web.mvc.annotation.RequestBody;
import org.clever.web.mvc.annotation.RequestParam;
import org.clever.web.mvc.annotation.RequestPart;
import org.clever.web.support.MultipartResolutionDelegate;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 解析以下方法参数：
 * <ul>
 * <li>用@{@link RequestPart} 注释
 * <li>{@link MultipartFile} 类型
 * <li>{@code javax.servlet.http.Part} 类型与 Servlet 3.0 多部分请求相结合
 * </ul>
 * <p>当使用 {@code @RequestPart} 注释参数时，以在考虑请求部分的“Content-Type”的情况下解析方法参数。
 * 这类似于 @{@link RequestBody} 根据常规请求的内容解析参数所做的事情。
 * <p>当参数未使用 {@code @RequestPart} 注释或未指定部分名称时，请求部分的名称派生自方法参数的名称。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 23:05 <br/>
 */
public class RequestPartMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private static final Class<org.springframework.web.bind.annotation.RequestPart> SPRING_REQUEST_PART = org.springframework.web.bind.annotation.RequestPart.class;
    private static final Class<org.springframework.web.bind.annotation.RequestParam> SPRING_REQUEST_PARAM = org.springframework.web.bind.annotation.RequestParam.class;
    private final ObjectMapper objectMapper;

    public RequestPartMethodArgumentResolver(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "参数 objectMapper 不能为 null");
        this.objectMapper = objectMapper;
    }

    /**
     * 给定的 {@linkplain MethodParameter MethodParameter} 是否支持多部分。支持以下方法参数：
     * <ul>
     * <li>用 {@code @RequestPart} 注释
     * <li>{@link MultipartFile} 类型，除非用 {@code @RequestParam} 注释
     * <li>{@code javax.servlet.http.Part} 类型，除非用 {@code @RequestParam} 注释
     * </ul>
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        if (parameter.hasParameterAnnotation(RequestPart.class) || parameter.hasParameterAnnotation(SPRING_REQUEST_PART)) {
            return true;
        } else {
            if (parameter.hasParameterAnnotation(RequestParam.class) || parameter.hasParameterAnnotation(SPRING_REQUEST_PARAM)) {
                return false;
            }
            return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
        }
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        boolean isRequired;
        String partName = "";
        if (requestPart != null) {
            isRequired = requestPart.required();
            partName = requestPart.value();
        } else {
            org.springframework.web.bind.annotation.RequestParam springRequestParam = parameter.getParameterAnnotation(SPRING_REQUEST_PARAM);
            isRequired = springRequestParam == null || springRequestParam.required();
            partName = Optional.ofNullable(springRequestParam).map(org.springframework.web.bind.annotation.RequestParam::name).orElse("");
        }
        isRequired = isRequired && !parameter.isOptional();
        String name = getPartName(parameter, partName);
        parameter = parameter.nestedIfOptional();
        Object arg = null;
        Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, request);
        if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
            arg = mpArg;
        } else {
            try {
                InputStream body = null;
                MultipartHttpServletRequest multipartRequest = MultipartResolutionDelegate.asMultipartHttpServletRequest(request);
                Part part = multipartRequest.getPart(name);
                if (part != null) {
                    body = part.getInputStream();
                }
                if (body == null) {
                    MultipartFile file = multipartRequest.getFile(name);
                    if (file != null) {
                        body = file.getInputStream();
                    }
                }
                if (body == null) {
                    String paramValue = multipartRequest.getParameter(name);
                    if (paramValue != null) {
                        Charset charset = null;
                        HttpHeaders httpHeaders = multipartRequest.getMultipartHeaders(name);
                        MediaType contentType = Optional.ofNullable(httpHeaders).map(HttpHeaders::getContentType).orElse(null);
                        if (contentType != null) {
                            charset = contentType.getCharset();
                        }
                        if (charset == null) {
                            String encoding = multipartRequest.getCharacterEncoding();
                            if (encoding != null) {
                                charset = Charset.forName(encoding);
                            }
                        }
                        if (charset == null) {
                            charset = StandardCharsets.UTF_8;
                        }
                        body = new ByteArrayInputStream(paramValue.getBytes(charset));
                    }
                }
                if (body == null) {
                    throw new IllegalStateException("No body available for request part '" + name + "'");
                }
                Type targetType = parameter.getNestedGenericParameterType();
                Class<?> contextClass = parameter.getContainingClass();
                JavaType javaType = objectMapper.constructType(GenericTypeResolver.resolveType(targetType, contextClass));
                arg = objectMapper.readValue(body, javaType);
            } catch (MissingServletRequestPartException | MultipartException ex) {
                if (isRequired) {
                    throw ex;
                }
            }
        }
        if (arg == null && isRequired) {
            if (!MultipartResolutionDelegate.isMultipartRequest(request)) {
                throw new MultipartException("Current request is not a multipart request");
            } else {
                throw new MissingServletRequestPartException(name);
            }
        }
        return HandlerMethodArgumentResolver.adaptArgumentIfNecessary(arg, parameter);
    }

    private String getPartName(MethodParameter methodParam, String name) {
        String partName = Optional.ofNullable(name).orElse("");
        if (partName.isEmpty()) {
            partName = methodParam.getParameterName();
            if (partName == null) {
                throw new IllegalArgumentException(
                    "Request part name for argument type ["
                        + methodParam.getNestedParameterType().getName()
                        + "] not specified, and parameter name information not found in class file either."
                );
            }
        }
        return partName;
    }
}
