//package org.clever.web.servlet.mvc.method.annotation;
//
//import org.clever.core.MethodParameter;
//import org.clever.core.ResolvableType;
//import org.clever.util.Assert;
//import org.clever.util.StreamUtils;
//import org.clever.validation.Errors;
//import org.clever.web.http.HttpMethod;
//import org.clever.web.spring.context.request.NativeWebRequest;
//import org.clever.web.support.mvc.bind.argument.HandlerMethodArgumentResolver;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.PushbackInputStream;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Type;
//import java.util.*;
//
///**
// * A base class for resolving method argument values by reading from the body of
// * a request with {@link HttpMessageConverter HttpMessageConverters}.
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/06 11:22 <br/>
// */
//public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {
//    private static final Set<HttpMethod> SUPPORTED_METHODS = EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);
//    private static final Object NO_VALUE = new Object();
//    protected final Logger logger = LoggerFactory.getLogger(getClass());
//
//    protected final List<HttpMessageConverter<?>> messageConverters;
//
//    private final RequestResponseBodyAdviceChain advice;
//
//    /**
//     * Basic constructor with converters only.
//     */
//    public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
//        this(converters, null);
//    }
//
//    /**
//     * Constructor with converters and {@code Request~} and {@code ResponseBodyAdvice}.
//     *
//     * @since 4.2
//     */
//    public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters, List<Object> requestResponseBodyAdvice) {
//        Assert.notEmpty(converters, "'messageConverters' must not be empty");
//        this.messageConverters = converters;
//        this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
//    }
//
//
//    /**
//     * Return the configured {@link RequestBodyAdvice} and
//     * {@link RequestBodyAdvice} where each instance may be wrapped as a
//     * {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean}.
//     */
//    RequestResponseBodyAdviceChain getAdvice() {
//        return this.advice;
//    }
//
//    /**
//     * Create the method argument value of the expected parameter type by
//     * reading from the given request.
//     *
//     * @param <T>        the expected type of the argument value to be created
//     * @param webRequest the current request
//     * @param parameter  the method parameter descriptor (may be {@code null})
//     * @param paramType  the type of the argument value to be created
//     * @return the created method argument value
//     * @throws IOException                        if the reading from the request fails
//     * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
//     */
//
//    protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
//                                                   Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
//
//        HttpInputMessage inputMessage = createInputMessage(webRequest);
//        return readWithMessageConverters(inputMessage, parameter, paramType);
//    }
//
//    /**
//     * Create the method argument value of the expected parameter type by reading
//     * from the given HttpInputMessage.
//     *
//     * @param <T>          the expected type of the argument value to be created
//     * @param inputMessage the HTTP input message representing the current request
//     * @param parameter    the method parameter descriptor
//     * @param targetType   the target type, not necessarily the same as the method
//     *                     parameter type, e.g. for {@code HttpEntity<String>}.
//     * @return the created method argument value
//     * @throws IOException                        if the reading from the request fails
//     * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
//     */
//    @SuppressWarnings("unchecked")
//
//    protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
//        MediaType contentType;
//        boolean noContentType = false;
//        try {
//            contentType = inputMessage.getHeaders().getContentType();
//        } catch (InvalidMediaTypeException ex) {
//            throw new HttpMediaTypeNotSupportedException(ex.getMessage());
//        }
//        if (contentType == null) {
//            noContentType = true;
//            contentType = MediaType.APPLICATION_OCTET_STREAM;
//        }
//
//        Class<?> contextClass = parameter.getContainingClass();
//        Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
//        if (targetClass == null) {
//            ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
//            targetClass = (Class<T>) resolvableType.resolve();
//        }
//
//        HttpMethod httpMethod = (inputMessage instanceof HttpRequest ? ((HttpRequest) inputMessage).getMethod() : null);
//        Object body = NO_VALUE;
//
//        EmptyBodyCheckingHttpInputMessage message = null;
//        try {
//            message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
//
//            for (HttpMessageConverter<?> converter : this.messageConverters) {
//                Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
//                GenericHttpMessageConverter<?> genericConverter =
//                        (converter instanceof GenericHttpMessageConverter ? (GenericHttpMessageConverter<?>) converter : null);
//                if (genericConverter != null ? genericConverter.canRead(targetType, contextClass, contentType) :
//                        (targetClass != null && converter.canRead(targetClass, contentType))) {
//                    if (message.hasBody()) {
//                        HttpInputMessage msgToUse =
//                                getAdvice().beforeBodyRead(message, parameter, targetType, converterType);
//                        body = (genericConverter != null ? genericConverter.read(targetType, contextClass, msgToUse) :
//                                ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse));
//                        body = getAdvice().afterBodyRead(body, msgToUse, parameter, targetType, converterType);
//                    } else {
//                        body = getAdvice().handleEmptyBody(null, message, parameter, targetType, converterType);
//                    }
//                    break;
//                }
//            }
//        } catch (IOException ex) {
//            throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
//        } finally {
//            if (message != null && message.hasBody()) {
//                closeStreamIfNecessary(message.getBody());
//            }
//        }
//
//        if (body == NO_VALUE) {
//            if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) ||
//                    (noContentType && !message.hasBody())) {
//                return null;
//            }
//            throw new HttpMediaTypeNotSupportedException(contentType,
//                    getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));
//        }
//
//        MediaType selectedContentType = contentType;
//        Object theBody = body;
//        LogFormatUtils.traceDebug(logger, traceOn -> {
//            String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
//            return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
//        });
//
//        return body;
//    }
//
//    /**
//     * Create a new {@link HttpInputMessage} from the given {@link NativeWebRequest}.
//     *
//     * @param webRequest the web request to create an input message from
//     * @return the input message
//     */
//    protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
//        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
//        Assert.state(servletRequest != null, "No HttpServletRequest");
//        return new ServletServerHttpRequest(servletRequest);
//    }
//
//    /**
//     * Validate the binding target if applicable.
//     * <p>The default implementation checks for {@code @javax.validation.Valid},
//     * Spring's {@link org.springframework.validation.annotation.Validated},
//     * and custom annotations whose name starts with "Valid".
//     *
//     * @param binder    the DataBinder to be used
//     * @param parameter the method parameter descriptor
//     * @see #isBindExceptionRequired
//     * @since 4.1.5
//     */
//    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
//        Annotation[] annotations = parameter.getParameterAnnotations();
//        for (Annotation ann : annotations) {
//            Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
//            if (validationHints != null) {
//                binder.validate(validationHints);
//                break;
//            }
//        }
//    }
//
//    /**
//     * Whether to raise a fatal bind exception on validation errors.
//     *
//     * @param binder    the data binder used to perform data binding
//     * @param parameter the method parameter descriptor
//     * @return {@code true} if the next method argument is not of type {@link Errors}
//     * @since 4.1.5
//     */
//    protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
//        int i = parameter.getParameterIndex();
//        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
//        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
//        return !hasBindingResult;
//    }
//
//    /**
//     * Return the media types supported by all provided message converters sorted
//     * by specificity via {@link MediaType#sortBySpecificity(List)}.
//     *
//     * @since 5.3.4
//     */
//    protected List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
//        Set<MediaType> mediaTypeSet = new LinkedHashSet<>();
//        for (HttpMessageConverter<?> converter : this.messageConverters) {
//            mediaTypeSet.addAll(converter.getSupportedMediaTypes(clazz));
//        }
//        List<MediaType> result = new ArrayList<>(mediaTypeSet);
//        MediaType.sortBySpecificity(result);
//        return result;
//    }
//
//    /**
//     * Adapt the given argument against the method parameter, if necessary.
//     *
//     * @param arg       the resolved argument
//     * @param parameter the method parameter descriptor
//     * @return the adapted argument, or the original resolved argument as-is
//     * @since 4.3.5
//     */
//
//    protected Object adaptArgumentIfNecessary(Object arg, MethodParameter parameter) {
//        if (parameter.getParameterType() == Optional.class) {
//            if (arg == null || (arg instanceof Collection && ((Collection<?>) arg).isEmpty()) ||
//                    (arg instanceof Object[] && ((Object[]) arg).length == 0)) {
//                return Optional.empty();
//            } else {
//                return Optional.of(arg);
//            }
//        }
//        return arg;
//    }
//
//    /**
//     * Allow for closing the body stream if necessary,
//     * e.g. for part streams in a multipart request.
//     */
//    void closeStreamIfNecessary(InputStream body) {
//        // No-op by default: A standard HttpInputMessage exposes the HTTP request stream
//        // (ServletRequest#getInputStream), with its lifecycle managed by the container.
//    }
//
//
//    private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {
//
//        private final HttpHeaders headers;
//
//
//        private final InputStream body;
//
//        public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
//            this.headers = inputMessage.getHeaders();
//            InputStream inputStream = inputMessage.getBody();
//            if (inputStream.markSupported()) {
//                inputStream.mark(1);
//                this.body = (inputStream.read() != -1 ? inputStream : null);
//                inputStream.reset();
//            } else {
//                PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
//                int b = pushbackInputStream.read();
//                if (b == -1) {
//                    this.body = null;
//                } else {
//                    this.body = pushbackInputStream;
//                    pushbackInputStream.unread(b);
//                }
//            }
//        }
//
//        @Override
//        public HttpHeaders getHeaders() {
//            return this.headers;
//        }
//
//        @Override
//        public InputStream getBody() {
//            return (this.body != null ? this.body : StreamUtils.emptyInput());
//        }
//
//        public boolean hasBody() {
//            return (this.body != null);
//        }
//    }
//
//}
