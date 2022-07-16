package org.clever.beans;

import org.clever.util.Assert;
import org.clever.util.ClassUtils;

import java.beans.PropertyChangeEvent;

/**
 * 尝试设置bean属性时，类型不匹配引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:01 <br/>
 */
public class TypeMismatchException extends PropertyAccessException {
    /**
     * 将向其注册类型不匹配错误的错误代码
     */
    public static final String ERROR_CODE = "typeMismatch";
    private String propertyName;
    private final transient Object value;
    private final Class<?> requiredType;

    /**
     * @param propertyChangeEvent 导致问题的PropertyChangeEvent
     * @param requiredType        所需的目标类型
     */
    public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType) {
        this(propertyChangeEvent, requiredType, null);
    }

    /**
     * @param propertyChangeEvent 导致问题的PropertyChangeEvent
     * @param requiredType        所需的目标类型(如果未知，则为null)
     * @param cause               根本原因(可能为空)
     */
    public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType, Throwable cause) {
        super(propertyChangeEvent,
                "Failed to convert property value of type '" +
                        ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
                        (requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
                        (propertyChangeEvent.getPropertyName() != null ? " for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
                cause
        );
        this.propertyName = propertyChangeEvent.getPropertyName();
        this.value = propertyChangeEvent.getNewValue();
        this.requiredType = requiredType;
    }

    /**
     * @param value        无法转换的有问题的值(可能为null)
     * @param requiredType 所需的目标类型(如果未知，则为null)
     * @see #initPropertyName
     */
    public TypeMismatchException(Object value, Class<?> requiredType) {
        this(value, requiredType, null);
    }

    /**
     * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
     *
     * @param value        无法转换的有问题的值(可能为null)
     * @param requiredType 所需的目标类型(如果未知，则为null)
     * @param cause        根本原因(可能为空)
     * @see #initPropertyName
     */
    public TypeMismatchException(Object value, Class<?> requiredType, Throwable cause) {
        super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
                        (requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : ""),
                cause
        );
        this.value = value;
        this.requiredType = requiredType;
    }

    /**
     * 通过{@link #getPropertyName()}初始化此异常的属性名称以进行公开，作为通过{@link PropertyChangeEvent}初始化它的替代方法
     *
     * @param propertyName 要公开的属性名称
     * @see #TypeMismatchException(Object, Class)
     * @see #TypeMismatchException(Object, Class, Throwable)
     */
    public void initPropertyName(String propertyName) {
        Assert.state(this.propertyName == null, "Property name already initialized");
        this.propertyName = propertyName;
    }

    /**
     * 返回受影响属性的名称(如果可用)
     */
    @Override
    public String getPropertyName() {
        return this.propertyName;
    }

    /**
     * 返回有问题的值(可能为null)
     */
    @Override
    public Object getValue() {
        return this.value;
    }

    /**
     * 返回所需的目标类型(如果有)
     */
    public Class<?> getRequiredType() {
        return this.requiredType;
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
