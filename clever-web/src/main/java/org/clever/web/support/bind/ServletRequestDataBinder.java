package org.clever.web.support.bind;

import org.apache.commons.lang3.StringUtils;
import org.clever.beans.MutablePropertyValues;
import org.clever.validation.BindException;
import org.clever.web.exception.ServletRequestBindingException;
import org.clever.web.http.HttpMethod;
import org.clever.web.http.MediaType;
import org.clever.web.http.multipart.MultipartRequest;
import org.clever.web.http.multipart.support.StandardServletPartUtils;
import org.clever.web.utils.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * 特殊的 {@link org.clever.validation.DataBinder} 执行从 servlet 请求参数到 JavaBeans 的数据绑定，包括对多部分文件的支持。
 * <p>请参阅 DataBinder/WebDataBinder 超类以获取自定义选项，其中包括指定允许/必需的字段以及注册自定义属性编辑器。
 * <p>也可用于自定义 Web 控制器中的手动数据绑定：例如，在普通控制器实现中或在 MultiActionController 处理程序方法中。
 * 简单地为每个绑定进程实例化一个 ServletRequestDataBinder，并以当前 ServletRequest 作为参数调用 {@code bind}：
 * <pre>{@code
 *   MyBean myBean = new MyBean();
 *   // apply binder to custom target object
 *   ServletRequestDataBinder binder = new ServletRequestDataBinder(myBean);
 *   // register custom editors, if desired
 *   binder.registerCustomEditor(...);
 *   // trigger actual binding of request parameters
 *   binder.bind(request);
 *   // optionally evaluate binding errors
 *   Errors errors = binder.getErrors();
 *   ...
 * }</pre>
 * <p>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/08 21:48 <br/>
 *
 * @see #bind(javax.servlet.ServletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class ServletRequestDataBinder extends WebDataBinder {
    /**
     * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @see #DEFAULT_OBJECT_NAME
     */
    public ServletRequestDataBinder(Object target) {
        super(target);
    }

    /**
     * @param target     要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @param objectName 目标对象的名称
     */
    public ServletRequestDataBinder(Object target, String objectName) {
        super(target, objectName);
    }

    /**
     * 将给定请求的参数绑定到此绑定器的目标，同时在多部分请求的情况下绑定多部分文件。
     * <p>此调用可以创建字段错误，表示基本绑定错误，如必填字段（代码“required”）或值和 bean 属性之间的类型不匹配（代码“typeMismatch”）。
     * <p>多部分文件通过它们的参数名称绑定，就像普通的 HTTP 参数一样：即“uploadedFile”到“uploadedFile”bean 属性，调用“setUploadedFile”setter 方法。
     * <p>多部分文件的目标属性类型可以是 MultipartFile、byte[] 或 String。当请求未通过 MultipartResolver 解析为 MultipartRequest 时，也支持 Servlet Part 绑定。
     *
     * @param request the request with parameters to bind (can be multipart)
     * @see org.clever.web.http.multipart.MultipartRequest
     * @see org.clever.web.http.multipart.MultipartFile
     * @see #bind(org.clever.beans.PropertyValues)
     */
    public void bind(ServletRequest request) {
        MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
        MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
        if (multipartRequest != null) {
            bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
        } else if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.MULTIPART_FORM_DATA_VALUE)) {
            HttpServletRequest httpServletRequest = WebUtils.getNativeRequest(request, HttpServletRequest.class);
            if (httpServletRequest != null && HttpMethod.POST.matches(httpServletRequest.getMethod())) {
                StandardServletPartUtils.bindParts(httpServletRequest, mpvs, isBindEmptyMultipartFiles());
            }
        }
        addBindValues(mpvs, request);
        doBind(mpvs);
    }

    /**
     * 子类可用于为请求添加额外绑定值的扩展点。在 {@link #doBind(MutablePropertyValues)} 之前调用。默认实现是空的。
     *
     * @param mpvs    将用于数据绑定的属性值
     * @param request 当前请求
     */
    protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
    }

    /**
     * 将错误视为致命错误。
     * <p>仅当输入无效时出现错误时才使用此方法。例如，如果所有输入都来自下拉菜单，这可能是合适的。
     *
     * @throws ServletRequestBindingException 任何绑定问题上的 ServletException 子类
     */
    public void closeNoCatch() throws ServletRequestBindingException {
        if (getBindingResult().hasErrors()) {
            throw new ServletRequestBindingException("Errors binding onto object '" + getBindingResult().getObjectName() + "'", new BindException(getBindingResult()));
        }
    }
}
