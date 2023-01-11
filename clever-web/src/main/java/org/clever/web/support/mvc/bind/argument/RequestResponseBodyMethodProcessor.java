//package org.clever.web.support.mvc.bind.argument;
//
//import org.clever.core.MethodParameter;
//import org.clever.core.annotation.AnnotatedElementUtils;
//
//import org.clever.util.Assert;
//import org.clever.validation.BindingResult;
//import org.clever.web.spring.context.request.NativeWebRequest;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.IOException;
//import java.lang.reflect.Type;
//import java.util.List;
//
//
///**
// * Resolves method arguments annotated with {@code @RequestBody} and handles return
// * values from methods annotated with {@code @ResponseBody} by reading and writing
// * to the body of the request or response with an {@link HttpMessageConverter}.
// *
// * <p>An {@code @RequestBody} method argument is also validated if it is annotated
// * with any
// * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints
// * annotations that trigger validation}. In case of validation failure,
// * {@link MethodArgumentNotValidException} is raised and results in an HTTP 400
// * response status code if {@link DefaultHandlerExceptionResolver} is configured.
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/06 11:20 <br/>
// */
//public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor {
//    /**
//     * Complete constructor for resolving {@code @RequestBody} method arguments.
//     * For handling {@code @ResponseBody} consider also providing a
//     * {@code ContentNegotiationManager}.
//     *
//     * @since 4.2
//     */
//    public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters, List<Object> requestResponseBodyAdvice) {
//        super(converters, null, requestResponseBodyAdvice);
//    }
//
//    /**
//     * Complete constructor for resolving {@code @RequestBody} and handling
//     * {@code @ResponseBody}.
//     */
//    public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters, ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {
//        super(converters, manager, requestResponseBodyAdvice);
//    }
//
//
//    @Override
//    public boolean supportsParameter(MethodParameter parameter) {
//        return parameter.hasParameterAnnotation(RequestBody.class);
//    }
//
//    @Override
//    public boolean supportsReturnType(MethodParameter returnType) {
//        return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||
//                returnType.hasMethodAnnotation(ResponseBody.class));
//    }
//
//    /**
//     * Throws MethodArgumentNotValidException if validation fails.
//     *
//     * @throws HttpMessageNotReadableException if {@link RequestBody#required()}
//     *                                         is {@code true} and there is no body content or if there is no suitable
//     *                                         converter to read the content with.
//     */
//    @Override
//    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
//                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
//
//        parameter = parameter.nestedIfOptional();
//        Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
//        String name = Conventions.getVariableNameForParameter(parameter);
//
//        if (binderFactory != null) {
//            WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
//            if (arg != null) {
//                validateIfApplicable(binder, parameter);
//                if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
//                    throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
//                }
//            }
//            if (mavContainer != null) {
//                mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
//            }
//        }
//
//        return adaptArgumentIfNecessary(arg, parameter);
//    }
//
//    @Override
//    protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
//                                                   Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
//
//        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
//        Assert.state(servletRequest != null, "No HttpServletRequest");
//        ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);
//
//        Object arg = readWithMessageConverters(inputMessage, parameter, paramType);
//        if (arg == null && checkRequired(parameter)) {
//            throw new HttpMessageNotReadableException("Required request body is missing: " +
//                    parameter.getExecutable().toGenericString(), inputMessage);
//        }
//        return arg;
//    }
//
//    protected boolean checkRequired(MethodParameter parameter) {
//        RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
//        return (requestBody != null && requestBody.required() && !parameter.isOptional());
//    }
//
//    @Override
//    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
//        mavContainer.setRequestHandled(true);
//        ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
//        ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
//
//        // Try even with null return value. ResponseBodyAdvice could get involved.
//        writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
//    }
//}