//package org.clever.web.support.mvc.bind.argument;
//
//import org.clever.core.MethodParameter;
//import org.clever.util.Assert;
//import org.clever.validation.BindingResult;
//import org.clever.web.spring.context.request.NativeWebRequest;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.List;
//
///**
// * Resolves the following method arguments:
// * <ul>
// * <li>Annotated with @{@link RequestPart}
// * <li>Of type {@link MultipartFile} in conjunction with Spring's {@link MultipartResolver} abstraction
// * <li>Of type {@code javax.servlet.http.Part} in conjunction with Servlet 3.0 multipart requests
// * </ul>
// *
// * <p>When a parameter is annotated with {@code @RequestPart}, the content of the part is
// * passed through an {@link HttpMessageConverter} to resolve the method argument with the
// * 'Content-Type' of the request part in mind. This is analogous to what @{@link RequestBody}
// * does to resolve an argument based on the content of a regular request.
// *
// * <p>When a parameter is not annotated with {@code @RequestPart} or the name of
// * the part is not specified, the request part's name is derived from the name of
// * the method argument.
// *
// * <p>Automatic validation may be applied if the argument is annotated with any
// * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints
// * annotations that trigger validation}. In case of validation failure, a
// * {@link MethodArgumentNotValidException} is raised and a 400 response status code returned if the
// * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}
// * is configured.
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 23:05 <br/>
// */
//public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {
//    /**
//     * Basic constructor with converters only.
//     */
//    public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
//        super(messageConverters);
//    }
//
//    /**
//     * Constructor with converters and {@code RequestBodyAdvice} and
//     * {@code ResponseBodyAdvice}.
//     */
//    public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters, List<Object> requestResponseBodyAdvice) {
//        super(messageConverters, requestResponseBodyAdvice);
//    }
//
//
//    /**
//     * Whether the given {@linkplain MethodParameter method parameter} is
//     * supported as multipart. Supports the following method parameters:
//     * <ul>
//     * <li>annotated with {@code @RequestPart}
//     * <li>of type {@link MultipartFile} unless annotated with {@code @RequestParam}
//     * <li>of type {@code javax.servlet.http.Part} unless annotated with
//     * {@code @RequestParam}
//     * </ul>
//     */
//    @Override
//    public boolean supportsParameter(MethodParameter parameter) {
//        if (parameter.hasParameterAnnotation(RequestPart.class)) {
//            return true;
//        } else {
//            if (parameter.hasParameterAnnotation(RequestParam.class)) {
//                return false;
//            }
//            return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
//        }
//    }
//
//    @Override
//    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
//        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
//        Assert.state(servletRequest != null, "No HttpServletRequest");
//
//        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
//        boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());
//
//        String name = getPartName(parameter, requestPart);
//        parameter = parameter.nestedIfOptional();
//        Object arg = null;
//
//        Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
//        if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
//            arg = mpArg;
//        } else {
//            try {
//                HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, name);
//                arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
//                if (binderFactory != null) {
//                    WebDataBinder binder = binderFactory.createBinder(request, arg, name);
//                    if (arg != null) {
//                        validateIfApplicable(binder, parameter);
//                        if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
//                            throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
//                        }
//                    }
//                    if (mavContainer != null) {
//                        mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
//                    }
//                }
//            } catch (MissingServletRequestPartException | MultipartException ex) {
//                if (isRequired) {
//                    throw ex;
//                }
//            }
//        }
//
//        if (arg == null && isRequired) {
//            if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
//                throw new MultipartException("Current request is not a multipart request");
//            } else {
//                throw new MissingServletRequestPartException(name);
//            }
//        }
//        return adaptArgumentIfNecessary(arg, parameter);
//    }
//
//    private String getPartName(MethodParameter methodParam, RequestPart requestPart) {
//        String partName = (requestPart != null ? requestPart.name() : "");
//        if (partName.isEmpty()) {
//            partName = methodParam.getParameterName();
//            if (partName == null) {
//                throw new IllegalArgumentException("Request part name for argument type [" +
//                        methodParam.getNestedParameterType().getName() +
//                        "] not specified, and parameter name information not found in class file either.");
//            }
//        }
//        return partName;
//    }
//
//    @Override
//    void closeStreamIfNecessary(InputStream body) {
//        // RequestPartServletServerHttpRequest exposes individual part streams,
//        // potentially from temporary files -> explicit close call after resolution
//        // in order to prevent file descriptor leaks.
//        try {
//            body.close();
//        } catch (IOException ex) {
//            // ignore
//        }
//    }
//}
