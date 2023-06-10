package org.clever.web.support.mvc.argument;

import org.clever.beans.BeanInstantiationException;
import org.clever.beans.BeanUtils;
import org.clever.beans.TypeMismatchException;
import org.clever.core.Conventions;
import org.clever.core.MethodParameter;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.clever.validation.*;
import org.clever.validation.annotation.ValidationAnnotationUtils;
import org.clever.web.http.HttpHeaders;
import org.clever.web.http.HttpMethod;
import org.clever.web.http.MediaType;
import org.clever.web.http.multipart.MultipartFile;
import org.clever.web.http.multipart.MultipartRequest;
import org.clever.web.http.multipart.support.StandardServletPartUtils;
import org.clever.web.support.bind.ExtendedServletRequestDataBinder;
import org.clever.web.support.bind.WebDataBinder;
import org.clever.web.support.bind.support.WebRequestDataBinder;
import org.clever.web.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * <p>模型属性是从模型中获取的或使用默认构造函数创建的（然后添加到模型中）。
 * 一旦创建属性通过数据绑定填充到 Servlet 请求参数。如果参数用 {@code @javax.validation.Valid} 注释，则可以应用验证。
 * 或 {@link  org.clever.validation.annotation.Validated}。
 *
 * <p>当使用 {@code annotationNotRequired=true} 创建此处理程序时，任何非简单类型参数和返回值都被视为模型属性，无论是否存在 {@code @ModelAttribute}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/05 11:53 <br/>
 */
public class ModelAttributeMethodProcessor implements HandlerMethodArgumentResolver {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final boolean annotationNotRequired;

    public ModelAttributeMethodProcessor(boolean annotationNotRequired) {
        this.annotationNotRequired = annotationNotRequired;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        return this.annotationNotRequired && !BeanUtils.isSimpleProperty(parameter.getParameterType());
    }

    @Override
    public final Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String name = Conventions.getVariableNameForParameter(parameter);
        Object attribute;
        BindingResult bindingResult = null;
        // 创建属性实例
        try {
            attribute = createAttribute(name, parameter, request);
        } catch (BindException ex) {
            if (isBindExceptionRequired(parameter)) {
                // 没有 BindingResult 参数 -> 因 BindException 而失败
                throw ex;
            }
            // 否则，公开 null/空值和关联的 BindingResult
            if (parameter.getParameterType() == Optional.class) {
                attribute = Optional.empty();
            } else {
                attribute = ex.getTarget();
            }
            bindingResult = ex.getBindingResult();
        }
        if (bindingResult == null) {
            // Bean 属性绑定和验证；在构造绑定失败的情况下跳过。
            WebDataBinder binder = createBinder(attribute, name);
            if (binder.getTarget() != null) {
                bindRequestParameters(binder, request);
                validateIfApplicable(binder, parameter);
                if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                    throw new BindException(binder.getBindingResult());
                }
            }
            // 值类型适配，也覆盖java.util.Optional
            if (!parameter.getParameterType().isInstance(attribute)) {
                attribute = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
            }
            // bindingResult = binder.getBindingResult();
        }
        return attribute;
    }

    protected Object createAttribute(String attributeName, MethodParameter parameter, HttpServletRequest request) throws Exception {
        MethodParameter nestedParameter = parameter.nestedIfOptional();
        Class<?> clazz = nestedParameter.getNestedParameterType();
        Constructor<?> ctor = BeanUtils.getResolvableConstructor(clazz);
        Object attribute = constructAttribute(ctor, attributeName, parameter, request);
        if (parameter != nestedParameter) {
            attribute = Optional.of(attribute);
        }
        return attribute;
    }

    protected Object constructAttribute(Constructor<?> ctor, String attributeName, MethodParameter parameter, HttpServletRequest request) throws Exception {
        if (ctor.getParameterCount() == 0) {
            // 单个默认构造函数 -> 显然是标准的 JavaBeans 安排。
            return BeanUtils.instantiateClass(ctor);
        }
        // 单个数据类构造函数 -> 从请求参数中解析构造函数参数。
        String[] paramNames = BeanUtils.getParameterNames(ctor);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        WebDataBinder binder = createBinder(null, attributeName);
        String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
        String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
        boolean bindingFailure = false;
        Set<String> failedParams = new HashSet<>(4);
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            Class<?> paramType = paramTypes[i];
            Object value = request.getParameterValues(paramName);
            // 由于 HttpServletRequest#getParameter 将单值参数公开为具有单个元素的数组，因此我们在这种情况下解包单个值，
            // 类似于 WebExchangeDataBinder.addBindValue(Map<String, Object>, String, List<?>)。
            if (ObjectUtils.isArray(value) && Array.getLength(value) == 1) {
                value = Array.get(value, 0);
            }
            if (value == null) {
                if (fieldDefaultPrefix != null) {
                    value = request.getParameter(fieldDefaultPrefix + paramName);
                }
                if (value == null) {
                    if (fieldMarkerPrefix != null && request.getParameter(fieldMarkerPrefix + paramName) != null) {
                        value = binder.getEmptyValue(paramType);
                    } else {
                        value = resolveConstructorArgument(paramName, paramType, request);
                    }
                }
            }
            try {
                MethodParameter methodParam = new FieldAwareConstructorParameter(ctor, i, paramName);
                if (value == null && methodParam.isOptional()) {
                    args[i] = (methodParam.getParameterType() == Optional.class ? Optional.empty() : null);
                } else {
                    args[i] = binder.convertIfNecessary(value, paramType, methodParam);
                }
            } catch (TypeMismatchException ex) {
                ex.initPropertyName(paramName);
                args[i] = null;
                failedParams.add(paramName);
                binder.getBindingResult().recordFieldValue(paramName, paramType, value);
                binder.getBindingErrorProcessor().processPropertyAccessException(ex, binder.getBindingResult());
                bindingFailure = true;
            }
        }
        if (bindingFailure) {
            BindingResult result = binder.getBindingResult();
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                if (!failedParams.contains(paramName)) {
                    Object value = args[i];
                    result.recordFieldValue(paramName, paramTypes[i], value);
                    validateValueIfApplicable(binder, parameter, ctor.getDeclaringClass(), paramName, value);
                }
            }
            if (!parameter.isOptional()) {
                try {
                    Object target = BeanUtils.instantiateClass(ctor, args);
                    throw new BindException(result) {
                        @Override
                        public Object getTarget() {
                            return target;
                        }
                    };
                } catch (BeanInstantiationException ex) {
                    // 吞下并在没有目标实例的情况下继续
                }
            }
            throw new BindException(result);
        }
        return BeanUtils.instantiateClass(ctor, args);
    }

    /**
     * 将请求绑定到目标对象的扩展点
     *
     * @param binder  用于绑定的数据绑定器实例
     * @param request 当前请求
     */
    protected void bindRequestParameters(WebDataBinder binder, HttpServletRequest request) {
        ((WebRequestDataBinder) binder).bind(request);
    }

    public Object resolveConstructorArgument(String paramName, Class<?> paramType, HttpServletRequest request) throws Exception {
        MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
        if (multipartRequest != null) {
            List<MultipartFile> files = multipartRequest.getFiles(paramName);
            if (!files.isEmpty()) {
                return (files.size() == 1 ? files.get(0) : files);
            }
        } else if (StringUtils.startsWithIgnoreCase(request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
            HttpServletRequest servletRequest = WebUtils.getNativeRequest(request, HttpServletRequest.class);
            if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
                List<Part> parts = StandardServletPartUtils.getParts(servletRequest, paramName);
                if (!parts.isEmpty()) {
                    return (parts.size() == 1 ? parts.get(0) : parts);
                }
            }
        }
        return null;
    }

    /**
     * 如果适用，验证模型属性。
     * <p>默认实现检查 {@code @javax.validation.Valid}、{@link org.clever.validation.annotation.Validated} 和名称以“Valid”开头的自定义注释。
     *
     * @param binder    要使用的 DataBinder
     * @param parameter 方法参数声明
     * @see WebDataBinder#validate(Object...)
     * @see SmartValidator#validate(Object, Errors, Object...)
     */
    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        for (Annotation ann : parameter.getParameterAnnotations()) {
            Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
            if (validationHints != null) {
                binder.validate(validationHints);
                break;
            }
        }
    }

    /**
     * 如果适用，验证指定的候选值。
     * <p>默认实现检查 {@code @javax.validation.Valid}、{@link org.clever.validation.annotation.Validated} 和名称以“Valid”开头的自定义注释。
     *
     * @param binder     要使用的 DataBinder
     * @param parameter  方法参数声明
     * @param targetType 目标类型
     * @param fieldName  字段名称
     * @param value      候选值
     * @see #validateIfApplicable(WebDataBinder, MethodParameter)
     * @see SmartValidator#validateValue(Class, String, Object, Errors, Object...)
     */
    protected void validateValueIfApplicable(WebDataBinder binder, MethodParameter parameter, Class<?> targetType, String fieldName, Object value) {
        for (Annotation ann : parameter.getParameterAnnotations()) {
            Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
            if (validationHints != null) {
                for (Validator validator : binder.getValidators()) {
                    if (validator instanceof SmartValidator) {
                        try {
                            ((SmartValidator) validator).validateValue(targetType, fieldName, value, binder.getBindingResult(), validationHints);
                        } catch (IllegalArgumentException ex) {
                            // 目标类上没有相应的字段...
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * 是否在验证错误时引发致命绑定异常。
     * <p>默认实现委托给 {@link #isBindExceptionRequired(MethodParameter)}。
     *
     * @param binder    用于执行数据绑定的数据绑定器
     * @param parameter 方法参数声明
     * @return {@code true} 如果下一个方法参数不是 {@link Errors} 类型
     * @see #isBindExceptionRequired(MethodParameter)
     */
    protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
        return isBindExceptionRequired(parameter);
    }

    /**
     * 是否在验证错误时引发致命绑定异常。
     *
     * @param parameter 方法参数声明
     * @return {@code true} 如果下一个方法参数不是 {@link Errors} 类型
     */
    protected boolean isBindExceptionRequired(MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }

    /**
     * 替换 WebDataBinderFactory
     */
    protected WebDataBinder createBinder(Object target, String objectName) {
        return new ExtendedServletRequestDataBinder(target, objectName);
    }

    /**
     * {@link MethodParameter} 子类，它也检测字段注释
     */
    private static class FieldAwareConstructorParameter extends MethodParameter {
        private final String parameterName;
        private volatile Annotation[] combinedAnnotations;

        public FieldAwareConstructorParameter(Constructor<?> constructor, int parameterIndex, String parameterName) {
            super(constructor, parameterIndex);
            this.parameterName = parameterName;
        }

        @Override
        public Annotation[] getParameterAnnotations() {
            Annotation[] anns = this.combinedAnnotations;
            if (anns == null) {
                anns = super.getParameterAnnotations();
                try {
                    Field field = getDeclaringClass().getDeclaredField(this.parameterName);
                    Annotation[] fieldAnns = field.getAnnotations();
                    if (fieldAnns.length > 0) {
                        List<Annotation> merged = new ArrayList<>(anns.length + fieldAnns.length);
                        merged.addAll(Arrays.asList(anns));
                        for (Annotation fieldAnn : fieldAnns) {
                            boolean existingType = false;
                            for (Annotation ann : anns) {
                                if (ann.annotationType() == fieldAnn.annotationType()) {
                                    existingType = true;
                                    break;
                                }
                            }
                            if (!existingType) {
                                merged.add(fieldAnn);
                            }
                        }
                        anns = merged.toArray(new Annotation[0]);
                    }
                } catch (NoSuchFieldException | SecurityException ex) {
                    // ignore
                }
                this.combinedAnnotations = anns;
            }
            return anns;
        }

        @Override
        public String getParameterName() {
            return this.parameterName;
        }
    }
}
