package org.clever.beans;

/**
 * 试图设置不可写属性的值时引发异常(通常是因为没有setter方法)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:25 <br/>
 */
public class NotWritablePropertyException extends InvalidPropertyException {
    private final String[] possibleMatches;

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     */
    public NotWritablePropertyException(Class<?> beanClass, String propertyName) {
        super(
                beanClass,
                propertyName,
                "Bean property '" + propertyName + "' is not writable or has an invalid setter method: " +
                        "Does the return type of the getter match the parameter type of the setter?"
        );
        this.possibleMatches = null;
    }

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     * @param msg          详细信息
     */
    public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg) {
        super(beanClass, propertyName, msg);
        this.possibleMatches = null;
    }

    /**
     * @param beanClass    bean类
     * @param propertyName 属性名
     * @param msg          详细信息
     * @param cause        根本原因
     */
    public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
        super(beanClass, propertyName, msg, cause);
        this.possibleMatches = null;
    }

    /**
     * @param beanClass       bean类
     * @param propertyName    属性名
     * @param msg             详细信息
     * @param possibleMatches 建议使用与无效属性名称紧密匹配的实际bean属性名称
     */
    public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, String[] possibleMatches) {
        super(beanClass, propertyName, msg);
        this.possibleMatches = possibleMatches;
    }

    /**
     * 返回与无效属性名称，紧密匹配的实际bean属性名称的建议
     */
    public String[] getPossibleMatches() {
        return this.possibleMatches;
    }
}
