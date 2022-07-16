package org.clever.beans;

import java.beans.PropertyChangeEvent;

/**
 * 当找不到bean属性的合适编辑器或转换器时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:16 <br/>
 */
public class ConversionNotSupportedException extends TypeMismatchException {
    /**
     * @param propertyChangeEvent 导致问题的PropertyChangeEvent
     * @param requiredType        所需的目标类型(如果未知，则为null)
     * @param cause               根本原因(可能为null)
     */
    public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType, Throwable cause) {
        super(propertyChangeEvent, requiredType, cause);
    }

    /**
     * @param value        无法转换的有问题的值(可能为null)
     * @param requiredType 所需的目标类型(如果未知，则为null)
     * @param cause        根本原因(可能为null)
     */
    public ConversionNotSupportedException(Object value, Class<?> requiredType, Throwable cause) {
        super(value, requiredType, cause);
    }
}
