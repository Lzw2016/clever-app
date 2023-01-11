package org.clever.web.support.mvc.bind.argument;

import org.clever.core.MethodParameter;
import org.clever.web.exception.MissingServletRequestPartException;
import org.clever.web.exception.MultipartException;
import org.clever.web.http.multipart.MultipartFile;
import org.clever.web.http.multipart.support.MultipartResolutionDelegate;
import org.clever.web.support.mvc.bind.annotation.RequestBody;
import org.clever.web.support.mvc.bind.annotation.RequestParam;
import org.clever.web.support.mvc.bind.annotation.RequestPart;

import javax.servlet.http.HttpServletRequest;

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
        if (parameter.hasParameterAnnotation(RequestPart.class)) {
            return true;
        } else {
            if (parameter.hasParameterAnnotation(RequestParam.class)) {
                return false;
            }
            return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
        }
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request) throws Exception {
        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());
        String name = getPartName(parameter, requestPart);
        parameter = parameter.nestedIfOptional();
        Object arg = null;
        Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, request);
        if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
            arg = mpArg;
        } else {
//            try {
//                arg = readWithMessageConverters(request, parameter, parameter.getNestedGenericParameterType());
//                WebDataBinder binder = binderFactory.createBinder(request, arg, name);
//                if (arg != null) {
//                    validateIfApplicable(binder, parameter);
//                    if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
//                        throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
//                    }
//                }
//            } catch (MissingServletRequestPartException | MultipartException ex) {
//                if (isRequired) {
//                    throw ex;
//                }
//            }
        }
        if (arg == null && isRequired) {
            if (!MultipartResolutionDelegate.isMultipartRequest(request)) {
                throw new MultipartException("Current request is not a multipart request");
            } else {
                throw new MissingServletRequestPartException(name);
            }
        }
//        return adaptArgumentIfNecessary(arg, parameter);
        return arg;
    }

    private String getPartName(MethodParameter methodParam, RequestPart requestPart) {
        String partName = (requestPart != null ? requestPart.name() : "");
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
