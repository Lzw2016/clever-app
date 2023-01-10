package org.clever.web.exception;

import org.clever.beans.ConversionNotSupportedException;
import org.clever.core.MethodParameter;

/**
 * 解析方法参数时引发的 ConversionNotSupportedException。
 * 提供对目标 {@link org.clever.core.MethodParameter MethodParameter} 的访问。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/10 15:45 <br/>
 */
public class MethodArgumentConversionNotSupportedException extends ConversionNotSupportedException {
    private final String name;
    private final MethodParameter parameter;

    public MethodArgumentConversionNotSupportedException(Object value, Class<?> requiredType, String name, MethodParameter param, Throwable cause) {
        super(value, requiredType, cause);
        this.name = name;
        this.parameter = param;
    }

    /**
     * 返回方法参数的名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 返回目标方法参数
     */
    public MethodParameter getParameter() {
        return this.parameter;
    }
}
