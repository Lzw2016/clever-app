package org.clever.web.support.bind.support;

import org.clever.beans.MutablePropertyValues;
import org.clever.util.StringUtils;
import org.clever.validation.BindException;
import org.clever.web.http.HttpHeaders;
import org.clever.web.http.HttpMethod;
import org.clever.web.http.MediaType;
import org.clever.web.http.multipart.MultipartRequest;
import org.clever.web.http.multipart.support.StandardServletPartUtils;
import org.clever.web.support.bind.WebDataBinder;
import org.clever.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 特殊的 {@link org.clever.validation.DataBinder} 用于执行从 Web 请求参数到 JavaBeans 的数据绑定，包括对多部分文件的支持。
 * <p>请参阅 DataBinder/WebDataBinder 超类以获取自定义选项，其中包括指定允许/必需的字段以及注册自定义属性编辑器。
 * <p>简单地为每个绑定进程实例化一个 WebRequestDataBinder，并以当前 WebRequest 作为参数调用 {@code bind}：
 * <pre>{@code
 *   MyBean myBean = new MyBean();
 *   // apply binder to custom target object
 *   WebRequestDataBinder binder = new WebRequestDataBinder(myBean);
 *   // register custom editors, if desired
 *   binder.registerCustomEditor(...);
 *   // trigger actual binding of request parameters
 *   binder.bind(request);
 *   // optionally evaluate binding errors
 *   Errors errors = binder.getErrors();
 *   ...
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/08 22:14 <br/>
 *
 * @see #bind(javax.servlet.http.HttpServletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class WebRequestDataBinder extends WebDataBinder {
    /**
     * 使用默认对象名称创建一个新的 WebRequestDataBinder 实例。
     *
     * @param target 要绑定到的目标对象（或 {@code null}，如果绑定器仅用于转换普通参数值）
     * @see #DEFAULT_OBJECT_NAME
     */
    public WebRequestDataBinder(Object target) {
        super(target);
    }

    /**
     * 创建一个新的 WebRequestDataBinder 实例。
     *
     * @param target     要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @param objectName 目标对象的名称
     */
    public WebRequestDataBinder(Object target, String objectName) {
        super(target, objectName);
    }

    /**
     * 将给定请求的参数绑定到此绑定器的目标，同时在多部分请求的情况下绑定多部分文件。
     * <p>此调用可以创建字段错误，表示基本绑定错误，如必填字段（代码“required”）或值和 bean 属性之间的类型不匹配（代码“typeMismatch”）。
     * <p>多部分文件通过它们的参数名称绑定，就像普通的 HTTP 参数一样：即“uploadedFile”到“uploadedFile”bean 属性，调用“setUploadedFile”setter 方法。
     * <p>多部分文件的目标属性类型可以是 MultipartFile、byte[] 或 String。
     * 当请求未通过 MultipartResolver 解析为 MultipartRequest 时，也支持 Servlet Part 绑定。
     *
     * @param request 带有参数绑定的请求（可以是多部分的）
     * @see org.clever.web.http.multipart.MultipartRequest
     * @see org.clever.web.http.multipart.MultipartFile
     * @see javax.servlet.http.Part
     * @see #bind(org.clever.beans.PropertyValues)
     */
    public void bind(HttpServletRequest request) {
        MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());
        MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
        if (multipartRequest != null) {
            bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
        } else if (StringUtils.startsWithIgnoreCase(request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
            HttpServletRequest servletRequest = WebUtils.getNativeRequest(request, HttpServletRequest.class);
            if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
                StandardServletPartUtils.bindParts(servletRequest, mpvs, isBindEmptyMultipartFiles());
            }
        }
        doBind(mpvs);
    }

    /**
     * 将错误视为致命错误。
     * <p>仅当输入无效时出现错误时才使用此方法。例如，如果所有输入都来自下拉菜单，这可能是合适的。
     *
     * @throws BindException 如果遇到绑定错误
     */
    public void closeNoCatch() throws BindException {
        if (getBindingResult().hasErrors()) {
            throw new BindException(getBindingResult());
        }
    }
}
