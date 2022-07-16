package org.clever.beans;

/**
 * 引用无效的bean属性时引发异常。携带有问题的bean类和属性名称
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:06 <br/>
 */
public class InvalidPropertyException extends FatalBeanException {
    /**
     * 问题的bean类
     */
    private final Class<?> beanClass;
    /**
     * 问题的bean属性
     */
    private final String propertyName;

    public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg) {
        this(beanClass, propertyName, msg, null);
    }

    public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
        super("Invalid property '" + propertyName + "' of bean class [" + beanClass.getName() + "]: " + msg, cause);
        this.beanClass = beanClass;
        this.propertyName = propertyName;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    public String getPropertyName() {
        return this.propertyName;
    }
}
