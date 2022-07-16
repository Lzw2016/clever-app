package org.clever.beans;

import java.beans.PropertyChangeEvent;

/**
 * 与属性访问相关的异常超类，例如类型不匹配或调用目标异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:00 <br/>
 */
public abstract class PropertyAccessException extends BeansException {
    /**
     * 导致问题的PropertyChangeEvent，可以为null
     */
    private final PropertyChangeEvent propertyChangeEvent;

    /**
     * 创建新的PropertyAccessException
     *
     * @param propertyChangeEvent 导致问题的PropertyChangeEvent
     * @param msg                 详细信息
     * @param cause               根本原因
     */
    public PropertyAccessException(PropertyChangeEvent propertyChangeEvent, String msg, Throwable cause) {
        super(msg, cause);
        this.propertyChangeEvent = propertyChangeEvent;
    }

    public PropertyAccessException(String msg, Throwable cause) {
        super(msg, cause);
        this.propertyChangeEvent = null;
    }

    public PropertyChangeEvent getPropertyChangeEvent() {
        return this.propertyChangeEvent;
    }

    /**
     * 返回受影响属性的名称(如果可用)
     */
    public String getPropertyName() {
        return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getPropertyName() : null);
    }

    /**
     * 返回要设置的受影响值(如果有)
     */
    public Object getValue() {
        return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getNewValue() : null);
    }

    /**
     * 为此类型的异常返回相应的错误代码
     */
    public abstract String getErrorCode();
}
