package org.clever.beans;

/**
 * 有效嵌套属性路径的导航遇到NullPointerException时引发异常。
 * 例如，"spouse.age"可能会失败，因为目标对象的spouse属性具有空值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:26 <br/>
 */
public class NullValueInNestedPathException extends InvalidPropertyException {
    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     */
    public NullValueInNestedPathException(Class<?> beanClass, String propertyName) {
        super(beanClass, propertyName, "Value of nested property '" + propertyName + "' is null");
    }

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     * @param msg          详细信息
     */
    public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg) {
        super(beanClass, propertyName, msg);
    }

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     * @param msg          详细信息
     * @param cause        根本原因
     */
    public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
        super(beanClass, propertyName, msg, cause);
    }
}
