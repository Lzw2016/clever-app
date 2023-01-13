package org.clever.web.support.mvc.argument;

import org.clever.beans.BeanUtils;
import org.clever.core.MethodParameter;
import org.clever.core.convert.converter.Converter;
import org.clever.util.StringUtils;
import org.clever.web.exception.MissingServletRequestParameterException;
import org.clever.web.exception.MissingServletRequestPartException;
import org.clever.web.exception.MultipartException;
import org.clever.web.http.multipart.MultipartFile;
import org.clever.web.http.multipart.MultipartRequest;
import org.clever.web.http.multipart.support.MultipartResolutionDelegate;
import org.clever.web.support.mvc.annotation.RequestParam;
import org.clever.web.support.mvc.annotation.RequestPart;
import org.clever.web.support.mvc.annotation.ValueConstants;
import org.clever.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;

/**
 * 解析使用 @{@link RequestParam} 注释的方法参数，
 * {@link MultipartFile} 类型的参数表示文件上传，
 * 以及 {@code javax.servlet.http.Part} 类型的参数与 Servlet 3.0相结合多部分请求。
 * 此解析器也可以在默认解析模式下创建，在该模式下，
 * 未使用 {@link RequestParam @RequestParam} 注释的简单类型（int、long 等）也被视为请求参数，
 * 其参数名称派生自参数名称。
 *
 * <p>如果方法参数类型为{@link Map}，则使用注解中指定的名称解析请求参数String值。
 * 假设已经注册了合适的 {@link Converter} 或 {@link PropertyEditor}，然后通过类型转换将该值转换为 {@link Map}。
 * 或者，如果未指定请求参数名称，则使用 {@link RequestParamMapMethodArgumentResolver} 来以Map的形式提供对所有请求参数的访问。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:50 <br/>
 *
 * @see RequestParamMapMethodArgumentResolver
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {
    private final boolean useDefaultResolution;

    /**
     * 创建一个新的 {@link RequestParamMethodArgumentResolver} 实例。
     *
     * @param useDefaultResolution 在默认解析模式下，作为简单类型的方法参数被视为请求参数，即使它没有被注释，请求参数名称是从方法参数名称派生的。
     */
    public RequestParamMethodArgumentResolver(boolean useDefaultResolution) {
        super();
        this.useDefaultResolution = useDefaultResolution;
    }

    /**
     * 支持以下内容：
     * <ul>
     * <li>@RequestParam 注释的方法参数。
     * 这不包括注释未指定名称的 {@link Map} 参数。
     * 有关此类参数，请参阅 {@link RequestParamMapMethodArgumentResolver}。
     * <li>{@link MultipartFile} 类型的参数，除非用 @{@link RequestPart} 注释。
     * <li>{@code Part} 类型的参数，除非用 @{@link RequestPart} 注释。
     * <li>在默认解析模式下，简单类型参数即使不使用@{@link RequestParam}。
     * </ul>
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
                RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
                return (requestParam != null && StringUtils.hasText(requestParam.name()));
            } else {
                return true;
            }
        } else {
            if (parameter.hasParameterAnnotation(RequestPart.class)) {
                return false;
            }
            parameter = parameter.nestedIfOptional();
            if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
                return true;
            } else if (this.useDefaultResolution) {
                return BeanUtils.isSimpleProperty(parameter.getNestedParameterType());
            } else {
                return false;
            }
        }
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
        return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
    }

    @Override
    protected Object resolveValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, request);
        if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
            return mpArg;
        }
        Object arg = null;
        MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
        if (multipartRequest != null) {
            List<MultipartFile> files = multipartRequest.getFiles(name);
            if (!files.isEmpty()) {
                arg = (files.size() == 1 ? files.get(0) : files);
            }
        }
        if (arg == null) {
            String[] paramValues = request.getParameterValues(name);
            if (paramValues != null) {
                arg = (paramValues.length == 1 ? paramValues[0] : paramValues);
            }
        }
        return arg;
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        handleMissingValueInternal(name, parameter, request, false);
    }

    @Override
    protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        handleMissingValueInternal(name, parameter, request, true);
    }

    protected void handleMissingValueInternal(String name, MethodParameter parameter, HttpServletRequest request, boolean missingAfterConversion) throws Exception {
        if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
            if (request == null || !MultipartResolutionDelegate.isMultipartRequest(request)) {
                throw new MultipartException("Current request is not a multipart request");
            } else {
                throw new MissingServletRequestPartException(name);
            }
        } else {
            throw new MissingServletRequestParameterException(name, parameter.getNestedParameterType().getSimpleName(), missingAfterConversion);
        }
    }

    private static class RequestParamNamedValueInfo extends NamedValueInfo {
        public RequestParamNamedValueInfo() {
            super("", false, ValueConstants.DEFAULT_NONE);
        }

        public RequestParamNamedValueInfo(RequestParam annotation) {
            super(annotation.name(), annotation.required(), annotation.defaultValue());
        }
    }
}
