package org.clever.web.support.mvc.argument;

import org.clever.core.MethodParameter;
import org.clever.core.ResolvableType;
import org.clever.util.CollectionUtils;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;
import org.clever.util.StringUtils;
import org.clever.web.http.multipart.MultipartFile;
import org.clever.web.http.multipart.MultipartRequest;
import org.clever.web.http.multipart.support.MultipartResolutionDelegate;
import org.clever.web.support.mvc.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * 解析使用 @{@link RequestParam} 注释的 {@link Map} 方法参数，其中注释未指定请求参数名称。
 *
 * <p>创建的 {@link Map} 包含所有请求参数名称值对，
 * 或者如果使用 {@link MultipartFile} 作为值类型专门声明，
 * 则包含给定参数名称的所有多部分文件。
 * 如果方法参数类型为 {@link MultiValueMap}，
 * 则创建的映射包含所有请求参数及其所有值，
 * 用于请求参数具有多个值（或多个同名多部分文件）的情况。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:55 <br/>
 *
 * @see RequestParamMethodArgumentResolver
 * @see HttpServletRequest#getParameterMap()
 * @see MultipartRequest#getMultiFileMap()
 * @see MultipartRequest#getFileMap()
 */
public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        return (requestParam != null && Map.class.isAssignableFrom(parameter.getParameterType()) && !StringUtils.hasText(requestParam.name()));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request) throws Exception {
        ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
        if (MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
            // MultiValueMap
            Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
            if (valueType == MultipartFile.class) {
                MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(request);
                return (multipartRequest != null ? multipartRequest.getMultiFileMap() : new LinkedMultiValueMap<>(0));
            } else if (valueType == Part.class) {
                if (MultipartResolutionDelegate.isMultipartRequest(request)) {
                    Collection<Part> parts = request.getParts();
                    LinkedMultiValueMap<String, Part> result = new LinkedMultiValueMap<>(parts.size());
                    for (Part part : parts) {
                        result.add(part.getName(), part);
                    }
                    return result;
                }
                return new LinkedMultiValueMap<>(0);
            } else {
                Map<String, String[]> parameterMap = request.getParameterMap();
                MultiValueMap<String, String> result = new LinkedMultiValueMap<>(parameterMap.size());
                parameterMap.forEach((key, values) -> {
                    for (String value : values) {
                        result.add(key, value);
                    }
                });
                return result;
            }
        } else {
            // Regular Map
            Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
            if (valueType == MultipartFile.class) {
                MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(request);
                return (multipartRequest != null ? multipartRequest.getFileMap() : new LinkedHashMap<>(0));
            } else if (valueType == Part.class) {
                if (MultipartResolutionDelegate.isMultipartRequest(request)) {
                    Collection<Part> parts = request.getParts();
                    LinkedHashMap<String, Part> result = CollectionUtils.newLinkedHashMap(parts.size());
                    for (Part part : parts) {
                        if (!result.containsKey(part.getName())) {
                            result.put(part.getName(), part);
                        }
                    }
                    return result;
                }
                return new LinkedHashMap<>(0);
            } else {
                Map<String, String[]> parameterMap = request.getParameterMap();
                Map<String, String> result = CollectionUtils.newLinkedHashMap(parameterMap.size());
                parameterMap.forEach((key, values) -> {
                    if (values.length > 0) {
                        result.put(key, values[0]);
                    }
                });
                return result;
            }
        }
    }
}
