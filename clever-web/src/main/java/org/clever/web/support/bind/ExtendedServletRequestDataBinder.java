package org.clever.web.support.bind;

import org.clever.beans.MutablePropertyValues;

import javax.servlet.ServletRequest;

/**
 * {@link ServletRequestDataBinder} 的子类，它将 URI 模板变量添加到用于数据绑定的值中。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/09 16:57 <br/>
 *
 * @see ServletRequestDataBinder
 */
public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {
    /**
     * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @see #DEFAULT_OBJECT_NAME
     */
    public ExtendedServletRequestDataBinder(Object target) {
        super(target);
    }

    /**
     * @param target     要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @param objectName 目标对象的名称
     * @see #DEFAULT_OBJECT_NAME
     */
    public ExtendedServletRequestDataBinder(Object target, String objectName) {
        super(target, objectName);
    }

    /**
     * 将 URI 变量合并到属性值中以用于数据绑定。
     */
    @Override
    protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
        // 未实现，暂不支持
    }
}
