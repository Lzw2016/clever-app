//package org.clever.web.support.mvc.bind.argument;
//
//import org.clever.core.MethodParameter;
//import org.clever.core.ResolvableType;
//import org.clever.util.CollectionUtils;
//import org.clever.util.LinkedMultiValueMap;
//import org.clever.util.MultiValueMap;
//import org.clever.util.StringUtils;
//import org.clever.web.support.mvc.bind.annotation.RequestParam;
//import org.clever.web.support.mvc.multipart.MultipartFile;
//import org.clever.web.support.mvc.multipart.MultipartRequest;
//import org.clever.web.support.mvc.multipart.support.MultipartResolutionDelegate;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.Part;
//import java.util.Collection;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//
///**
// * Resolves {@link Map} method arguments annotated with an @{@link RequestParam}
// * where the annotation does not specify a request parameter name.
// *
// * <p>The created {@link Map} contains all request parameter name/value pairs,
// * or all multipart files for a given parameter name if specifically declared
// * with {@link MultipartFile} as the value type. If the method parameter type is
// * {@link MultiValueMap} instead, the created map contains all request parameters
// * and all their values for cases where request parameters have multiple values
// * (or multiple multipart files of the same name).
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 22:55 <br/>
// *
// * @see RequestParamMethodArgumentResolver
// * @see HttpServletRequest#getParameterMap()
// * @see MultipartRequest#getMultiFileMap()
// * @see MultipartRequest#getFileMap()
// */
//public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
//    @Override
//    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
//        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
//        return (requestParam != null && Map.class.isAssignableFrom(parameter.getParameterType()) && !StringUtils.hasText(requestParam.name()));
//    }
//
//    @Override
//    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request) throws Exception {
//        ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
//        if (MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
//            // MultiValueMap
//            Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
//            if (valueType == MultipartFile.class) {
//                MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
//                return (multipartRequest != null ? multipartRequest.getMultiFileMap() : new LinkedMultiValueMap<>(0));
//            } else if (valueType == Part.class) {
//                HttpServletRequest servletRequest = request;
//                if (MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
//                    Collection<Part> parts = servletRequest.getParts();
//                    LinkedMultiValueMap<String, Part> result = new LinkedMultiValueMap<>(parts.size());
//                    for (Part part : parts) {
//                        result.add(part.getName(), part);
//                    }
//                    return result;
//                }
//                return new LinkedMultiValueMap<>(0);
//            } else {
//                Map<String, String[]> parameterMap = request.getParameterMap();
//                MultiValueMap<String, String> result = new LinkedMultiValueMap<>(parameterMap.size());
//                parameterMap.forEach((key, values) -> {
//                    for (String value : values) {
//                        result.add(key, value);
//                    }
//                });
//                return result;
//            }
//        } else {
//            // Regular Map
//            Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
//            if (valueType == MultipartFile.class) {
//                MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
//                return (multipartRequest != null ? multipartRequest.getFileMap() : new LinkedHashMap<>(0));
//            } else if (valueType == Part.class) {
//                HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
//                if (servletRequest != null && MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
//                    Collection<Part> parts = servletRequest.getParts();
//                    LinkedHashMap<String, Part> result = CollectionUtils.newLinkedHashMap(parts.size());
//                    for (Part part : parts) {
//                        if (!result.containsKey(part.getName())) {
//                            result.put(part.getName(), part);
//                        }
//                    }
//                    return result;
//                }
//                return new LinkedHashMap<>(0);
//            } else {
//                Map<String, String[]> parameterMap = webRequest.getParameterMap();
//                Map<String, String> result = CollectionUtils.newLinkedHashMap(parameterMap.size());
//                parameterMap.forEach((key, values) -> {
//                    if (values.length > 0) {
//                        result.put(key, values[0]);
//                    }
//                });
//                return result;
//            }
//        }
//    }
//}
