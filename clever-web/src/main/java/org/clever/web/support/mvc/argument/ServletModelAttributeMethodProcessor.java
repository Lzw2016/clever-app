//package org.clever.web.support.mvc.argument;
//
//import org.clever.core.MethodParameter;
//import org.clever.core.convert.ConversionService;
//import org.clever.core.convert.TypeDescriptor;
//import org.clever.core.convert.converter.Converter;
//import org.clever.util.Assert;
//import org.clever.util.StringUtils;
//import org.clever.validation.DataBinder;
//import org.clever.web.support.bind.ServletRequestDataBinder;
//import org.clever.web.support.bind.WebDataBinder;
//
//import javax.servlet.http.HttpServletRequest;
//
///**
// * 特定于 Servlet 的 {@link ModelAttributeMethodProcessor}，它通过 {@link ServletRequestDataBinder} 类型的 WebDataBinder 应用数据绑定。
// * <p>如果名称与模型属性名称匹配并且存在适当的类型转换策略，还添加一个回退策略以从 URI 模板变量或请求参数实例化模型属性。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/06/08 22:26 <br/>
// */
//public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {
//    /**
//     * @param annotationNotRequired 如果为“true”，则非简单方法参数和返回值被视为具有或不具有 {@code @ModelAttribute} 注释的模型属性
//     */
//    public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
//        super(annotationNotRequired);
//    }
//
//    /**
//     * 如果名称与模型属性名称匹配并且存在适当的类型转换策略，则从 URI 模板变量或请求参数实例化模型属性。如果这些都不是真正的委托回基类。
//     *
//     * @see #createAttributeFromRequestValue
//     */
//    @Override
//    protected final Object createAttribute(String attributeName, MethodParameter parameter, HttpServletRequest request) throws Exception {
//        String value = getRequestValueForAttribute(attributeName, request);
//        if (value != null) {
//            Object attribute = createAttributeFromRequestValue(value, attributeName, parameter);
//            if (attribute != null) {
//                return attribute;
//            }
//        }
//        return super.createAttribute(attributeName, parameter, request);
//    }
//
//    /**
//     * 从请求中获取一个值，该值可用于通过从 String 到目标类型的类型转换来实例化模型属性。
//     * <p>默认实现首先查找属性名称以匹配 URI 变量，然后查找请求参数。
//     *
//     * @param attributeName 模型属性名称
//     * @param request       当前请求
//     * @return 尝试转换的请求值，如果没有则为 {@code null}
//     */
//    protected String getRequestValueForAttribute(String attributeName, HttpServletRequest request) {
//        String parameterValue = request.getParameter(attributeName);
//        if (StringUtils.hasText(parameterValue)) {
//            return parameterValue;
//        }
//        return null;
//    }
//
//    /**
//     * 使用类型转换从字符串请求值（例如 URI 模板变量、请求参数）创建模型属性。
//     * <p>仅当存在可以执行转换的已注册 {@link Converter} 时，默认实现才会转换。
//     *
//     * @param sourceValue   从中创建模型属性的源值
//     * @param attributeName 属性的名称（从不 {@code null}）
//     * @param parameter     方法参数
//     * @return 创建的模型属性，如果没有找到合适的转换，则为 {@code null}
//     */
//    protected Object createAttributeFromRequestValue(String sourceValue, String attributeName, MethodParameter parameter) {
//        DataBinder binder = createBinder(null, attributeName);
//        ConversionService conversionService = binder.getConversionService();
//        if (conversionService != null) {
//            TypeDescriptor source = TypeDescriptor.valueOf(String.class);
//            TypeDescriptor target = new TypeDescriptor(parameter);
//            if (conversionService.canConvert(source, target)) {
//                return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 此实现在绑定之前将 {@link WebDataBinder} 向下转换为 {@link ServletRequestDataBinder}。
//     */
//    @Override
//    protected void bindRequestParameters(WebDataBinder binder, HttpServletRequest request) {
//        Assert.state(request != null, "No ServletRequest");
//        ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
//        servletBinder.bind(request);
//    }
//
//    @Override
//    public Object resolveConstructorArgument(String paramName, Class<?> paramType, HttpServletRequest request) throws Exception {
//        return super.resolveConstructorArgument(paramName, paramType, request);
//    }
//}
