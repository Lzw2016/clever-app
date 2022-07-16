package org.clever.beans;

import java.beans.PropertyChangeEvent;

/**
 * 当bean属性getter或setter方法引发异常时引发，类似于InvocationTargetException
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:36 <br/>
 */
public class MethodInvocationException extends PropertyAccessException {
    /**
     * 将向其注册方法调用错误的错误代码
     */
    public static final String ERROR_CODE = "methodInvocation";

    /**
     * @param propertyChangeEvent 导致异常的PropertyChangeEvent
     * @param cause               被调用的方法引发的Throwable
     */
    public MethodInvocationException(PropertyChangeEvent propertyChangeEvent, Throwable cause) {
        super(propertyChangeEvent, "Property '" + propertyChangeEvent.getPropertyName() + "' threw exception", cause);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
