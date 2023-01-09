package org.clever.web.support.mvc.bind.argument;

import org.clever.core.MethodParameter;
import org.clever.web.exception.ServletRequestBindingException;
import org.clever.web.support.mvc.bind.annotation.ValueConstants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于从命名值解析方法参数的抽象基类。
 * 请求参数、请求标头和路径变量是命名值的示例。
 * 每个都可能有一个名称、一个必需的标志和一个默认值。
 *
 * <p>子类定义如何执行以下操作：
 * <ul>
 * <li>获取方法参数的命名值信息
 * <li>将名称解析为参数值
 * <li>在需要参数值时处理丢失的参数值
 * <li>可选地处理已解析的值
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:28 <br/>
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request) throws Exception {
        NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
        String resolvedName = namedValueInfo.name;
        MethodParameter nestedParameter = parameter.nestedIfOptional();
        Object arg = resolveValue(resolvedName, nestedParameter, request);
        if (arg == null) {
            if (namedValueInfo.defaultValue != null) {
                arg = namedValueInfo.defaultValue;
            } else if (namedValueInfo.required && !nestedParameter.isOptional()) {
                handleMissingValue(namedValueInfo.name, nestedParameter, request);
            }
            arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
        } else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
            arg = namedValueInfo.defaultValue;
        }
        // TODO 数据类型转换???

        // 解析值的后置处理
        handleResolvedValue(arg, namedValueInfo.name, parameter, request);
        return arg;
    }

    /**
     * 将给定的参数类型和值名称解析为参数值
     *
     * @param name      正在解析的值的名称
     * @param parameter 解析为参数值的方法参数（在 {@link java.util.Optional} 声明的情况下预先嵌套）
     * @param request   当前请求
     * @return 已解决的参数（可能是 {@code null}）
     */
    protected abstract Object resolveValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception;

    /**
     * Create the {@link NamedValueInfo} object for the given method parameter. Implementations typically
     * retrieve the method annotation by means of {@link MethodParameter#getParameterAnnotation(Class)}.
     *
     * @param parameter the method parameter
     * @return the named value information
     */
    protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

    /**
     * 在解析值后调用
     *
     * @param arg       解析的参数值
     * @param name      参数名称
     * @param parameter argument 参数类型
     * @param request   当前请求
     */
    protected void handleResolvedValue(Object arg, String name, MethodParameter parameter, HttpServletRequest request) {
    }

    /**
     * 当需要命名值时调用，但 {@link #resolveValue(String, MethodParameter, HttpServletRequest)} 返回 {@code null} 并且没有默认值。
     * 在这种情况下，子类通常会抛出异常。
     *
     * @param name      值的名称
     * @param parameter 方法参数
     * @param request   当前请求
     */
    protected void handleMissingValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        handleMissingValue(name, parameter);
    }

    /**
     * 当需要命名值时调用，但 {@link #resolveValue(String, MethodParameter, HttpServletRequest)} 返回 {@code null} 并且没有默认值。
     * 在这种情况下，子类通常会抛出异常。
     *
     * @param name      the name for the value
     * @param parameter the method parameter
     */
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
        throw new ServletRequestBindingException(
                "Missing argument '" + name
                        + "' for method parameter of type "
                        + parameter.getNestedParameterType().getSimpleName()
        );
    }

    /**
     * {@code null} 导致 {@code boolean} 的 {@code false} 值或其他原语的异常
     */
    private Object handleNullValue(String name, Object value, Class<?> paramType) {
        if (value == null) {
            if (Boolean.TYPE.equals(paramType)) {
                return Boolean.FALSE;
            } else if (paramType.isPrimitive()) {
                throw new IllegalStateException(
                        "Optional " + paramType.getSimpleName() + " parameter '"
                                + name + "' is present but cannot be translated into a null value due to being declared as a "
                                + "primitive type. Consider declaring it as object wrapper for the corresponding primitive type."
                );
            }
        }
        return value;
    }

    /**
     * Obtain the named value for the given method parameter.
     */
    private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
        NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
        if (namedValueInfo == null) {
            namedValueInfo = createNamedValueInfo(parameter);
            namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
            this.namedValueInfoCache.put(parameter, namedValueInfo);
        }
        return namedValueInfo;
    }

    /**
     * Create a new NamedValueInfo based on the given NamedValueInfo with sanitized values.
     */
    private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
        String name = info.name;
        if (info.name.isEmpty()) {
            name = parameter.getParameterName();
            if (name == null) {
                throw new IllegalArgumentException(
                        "Name for argument of type ["
                                + parameter.getNestedParameterType().getName()
                                + "] not specified, and parameter name information not found in class file either."
                );
            }
        }
        String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
        return new NamedValueInfo(name, info.required, defaultValue);
    }

    /**
     * 表示有关命名值的信息，包括名称、是否需要和默认值。
     */
    protected static class NamedValueInfo {
        private final String name;
        private final boolean required;
        private final String defaultValue;

        public NamedValueInfo(String name, boolean required, String defaultValue) {
            this.name = name;
            this.required = required;
            this.defaultValue = defaultValue;
        }
    }
}
