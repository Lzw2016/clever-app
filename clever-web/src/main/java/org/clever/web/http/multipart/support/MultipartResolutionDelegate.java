//package org.clever.web.http.multipart.support;
//
//import org.clever.core.MethodParameter;
//import org.clever.core.ResolvableType;
//import org.clever.web.http.multipart.MultipartFile;
//import org.clever.web.http.multipart.MultipartRequest;
//import org.clever.web.utils.WebUtils;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.Part;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
///**
// * {@code HandlerMethodArgumentResolver} 实现的公共委托，需要解析 {@link MultipartFile} 和 {@link Part} 参数。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 22:54 <br/>
// */
//public final class MultipartResolutionDelegate {
//    /**
//     * 指示无法解析的值。
//     */
//    public static final Object UNRESOLVABLE = new Object();
//
//    private MultipartResolutionDelegate() {
//    }
//
//    public static MultipartRequest resolveMultipartRequest(HttpServletRequest request) {
//        MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
//        if (multipartRequest != null) {
//            return multipartRequest;
//        }
//        if (request != null && isMultipartContent(request)) {
//            return new StandardMultipartHttpServletRequest(request);
//        }
//        return null;
//    }
//
//    public static boolean isMultipartRequest(HttpServletRequest request) {
//        return (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null || isMultipartContent(request));
//    }
//
//    private static boolean isMultipartContent(HttpServletRequest request) {
//        String contentType = request.getContentType();
//        return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
//    }
//
//    public static MultipartHttpServletRequest asMultipartHttpServletRequest(HttpServletRequest request) {
//        MultipartHttpServletRequest unwrapped = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
//        if (unwrapped != null) {
//            return unwrapped;
//        }
//        return new StandardMultipartHttpServletRequest(request);
//    }
//
//    public static boolean isMultipartArgument(MethodParameter parameter) {
//        Class<?> paramType = parameter.getNestedParameterType();
//        return (MultipartFile.class == paramType
//                || isMultipartFileCollection(parameter)
//                || isMultipartFileArray(parameter)
//                || (Part.class == paramType || isPartCollection(parameter) || isPartArray(parameter)));
//    }
//
//    public static Object resolveMultipartArgument(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
//        MultipartHttpServletRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
//        boolean isMultipart = (multipartRequest != null || isMultipartContent(request));
//        if (MultipartFile.class == parameter.getNestedParameterType()) {
//            if (!isMultipart) {
//                return null;
//            }
//            if (multipartRequest == null) {
//                multipartRequest = new StandardMultipartHttpServletRequest(request);
//            }
//            return multipartRequest.getFile(name);
//        } else if (isMultipartFileCollection(parameter)) {
//            if (!isMultipart) {
//                return null;
//            }
//            if (multipartRequest == null) {
//                multipartRequest = new StandardMultipartHttpServletRequest(request);
//            }
//            List<MultipartFile> files = multipartRequest.getFiles(name);
//            return (!files.isEmpty() ? files : null);
//        } else if (isMultipartFileArray(parameter)) {
//            if (!isMultipart) {
//                return null;
//            }
//            if (multipartRequest == null) {
//                multipartRequest = new StandardMultipartHttpServletRequest(request);
//            }
//            List<MultipartFile> files = multipartRequest.getFiles(name);
//            return (!files.isEmpty() ? files.toArray(new MultipartFile[0]) : null);
//        } else if (Part.class == parameter.getNestedParameterType()) {
//            if (!isMultipart) {
//                return null;
//            }
//            return request.getPart(name);
//        } else if (isPartCollection(parameter)) {
//            if (!isMultipart) {
//                return null;
//            }
//            List<Part> parts = resolvePartList(request, name);
//            return (!parts.isEmpty() ? parts : null);
//        } else if (isPartArray(parameter)) {
//            if (!isMultipart) {
//                return null;
//            }
//            List<Part> parts = resolvePartList(request, name);
//            return (!parts.isEmpty() ? parts.toArray(new Part[0]) : null);
//        } else {
//            return UNRESOLVABLE;
//        }
//    }
//
//    private static boolean isMultipartFileCollection(MethodParameter methodParam) {
//        return (MultipartFile.class == getCollectionParameterType(methodParam));
//    }
//
//    private static boolean isMultipartFileArray(MethodParameter methodParam) {
//        return (MultipartFile.class == methodParam.getNestedParameterType().getComponentType());
//    }
//
//    private static boolean isPartCollection(MethodParameter methodParam) {
//        return (Part.class == getCollectionParameterType(methodParam));
//    }
//
//    private static boolean isPartArray(MethodParameter methodParam) {
//        return (Part.class == methodParam.getNestedParameterType().getComponentType());
//    }
//
//    private static Class<?> getCollectionParameterType(MethodParameter methodParam) {
//        Class<?> paramType = methodParam.getNestedParameterType();
//        if (Collection.class == paramType || List.class.isAssignableFrom(paramType)) {
//            return ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
//        }
//        return null;
//    }
//
//    private static List<Part> resolvePartList(HttpServletRequest request, String name) throws Exception {
//        Collection<Part> parts = request.getParts();
//        List<Part> result = new ArrayList<>(parts.size());
//        for (Part part : parts) {
//            if (part.getName().equals(name)) {
//                result.add(part);
//            }
//        }
//        return result;
//    }
//}
