package org.clever.beans;

/**
 * 尝试获取不可读属性的值时引发异常，因为没有getter方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:31 <br/>
 */
public class NotReadablePropertyException extends InvalidPropertyException {
    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     */
    public NotReadablePropertyException(Class<?> beanClass, String propertyName) {
        super(
                beanClass,
                propertyName,
                "Bean property '" + propertyName + "' is not readable or has an invalid getter method: " +
                        "Does the return type of the getter match the parameter type of the setter?"
        );
    }

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     * @param msg          详细信息
     */
    public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg) {
        super(beanClass, propertyName, msg);
    }

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     * @param msg          详细信息
     * @param cause        根本原因
     */
    public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
        super(beanClass, propertyName, msg, cause);
    }
}
