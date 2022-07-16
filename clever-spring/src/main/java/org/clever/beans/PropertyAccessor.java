package org.clever.beans;

import org.clever.core.convert.TypeDescriptor;

import java.util.Map;

/**
 * 可以访问命名属性(例如对象的bean属性或对象中的字段)的类的公共接口，用作{@link BeanWrapper}的基本接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:05 <br/>
 *
 * @see BeanWrapper
 */
public interface PropertyAccessor {
    /**
     * 嵌套属性的路径分隔符<br/>
     * 遵循常规Java约定: getFoo().getBar() 则是 "foo.bar".
     */
    String NESTED_PROPERTY_SEPARATOR = ".";
    /**
     * 嵌套属性的路径分隔符<br/>
     * 遵循常规Java约定: getFoo().getBar() 则是 "foo.bar".
     */
    char NESTED_PROPERTY_SEPARATOR_CHAR = '.';
    /**
     * 指示索引或映射属性的属性键开始的标记，如 "person.addresses[0]".
     */
    String PROPERTY_KEY_PREFIX = "[";
    /**
     * 指示索引或映射属性的属性键开始的标记，如 "person.addresses[0]".
     */
    char PROPERTY_KEY_PREFIX_CHAR = '[';
    /**
     * 指示索引或映射属性的属性键结束的标记，如 "person.addresses[0]".
     */
    String PROPERTY_KEY_SUFFIX = "]";
    /**
     * 指示索引或映射属性的属性键结束的标记，如 "person.addresses[0]".
     */
    char PROPERTY_KEY_SUFFIX_CHAR = ']';

    /**
     * 确定指定的属性是否可读。如果属性不存在，则返回false
     *
     * @param propertyName 要检查的属性（可以是嵌套路径和或索引/映射属性）
     */
    boolean isReadableProperty(String propertyName);

    /**
     * 确定指定的属性是否可写。如果属性不存在，则返回false
     *
     * @param propertyName 要检查的属性（可以是嵌套路径和或索引/映射属性）
     */
    boolean isWritableProperty(String propertyName);

    /**
     * 确定指定属性的属性类型，检查属性描述符或检查索引或映射元素的值
     *
     * @param propertyName 要检查的属性(可以是嵌套路径和/或索引/映射属性)
     * @throws PropertyAccessException 如果属性有效但访问器方法失败
     */
    Class<?> getPropertyType(String propertyName) throws BeansException;

    /**
     * 返回指定属性的类型描述符：最好从read方法返回，返回write方法
     *
     * @param propertyName 要检查的属性(可以是嵌套路径和/或索引/映射属性)
     * @throws PropertyAccessException 如果属性有效但访问器方法失败
     */
    TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

    /**
     * 获取指定属性的当前值
     *
     * @param propertyName 要获取其值的属性的名称(可以是嵌套路径和/或索引/映射属性)
     * @return the value of the property
     * @throws InvalidPropertyException 如果没有此类属性或属性不可读
     * @throws PropertyAccessException  如果属性有效但访问器方法失败
     */
    Object getPropertyValue(String propertyName) throws BeansException;

    /**
     * 将指定的值设置为当前属性值
     *
     * @param propertyName 要设置其值的属性的名称(可以是嵌套路径和/或索引/映射属性)
     * @param value        新值
     * @throws InvalidPropertyException 如果没有此类属性或属性不可写
     * @throws PropertyAccessException  如果属性有效，但访问器方法失败或发生类型不匹配
     */
    void setPropertyValue(String propertyName, Object value) throws BeansException;

    /**
     * 将指定的值设置为当前属性值
     *
     * @param pv 包含新属性值的对象
     * @throws InvalidPropertyException 如果没有此类属性或属性不可写
     * @throws PropertyAccessException  如果属性有效，但访问器方法失败或发生类型不匹配
     */
    void setPropertyValue(PropertyValue pv) throws BeansException;

    /**
     * 从Map执行批更新<br/>
     * 来自PropertyValue的批量更新功能更强大：提供此方法是为了方便。行为将与{@link #setPropertyValues(PropertyValues)}方法的行为相同
     *
     * @param map 从中获取属性的Map。包含由属性名称设置关键帧的属性值对象
     * @throws InvalidPropertyException     如果没有此类属性或属性不可写
     * @throws PropertyBatchUpdateException 如果在批处理更新期间，特定属性发生一个或多个PropertyAccessException。此异常捆绑所有单个PropertyAccessExceptions。将成功更新所有其他属性
     */
    void setPropertyValues(Map<?, ?> map) throws BeansException;

    /**
     * 执行批更新的首选方式<br/>
     * 请注意，执行批更新与执行单个更新的不同之处在于，如果遇到可恢复错误(例如类型不匹配，但不是无效的字段名等)，
     * 则此类的实现将继续更新属性，引发包含所有单个错误的{@link PropertyBatchUpdateException}。
     * 稍后可以检查此异常以查看所有绑定错误。已成功更新的属性仍保持更改状态<br/>
     * 不允许未知字段或无效字段。
     *
     * @param pvs 要在目标对象上设置的属性值
     * @throws InvalidPropertyException     如果没有此类属性或属性不可写
     * @throws PropertyBatchUpdateException 如果在批处理更新期间，特定属性发生一个或多个PropertyAccessException。此异常捆绑所有单个PropertyAccessExceptions。将成功更新所有其他属性
     * @see #setPropertyValues(PropertyValues, boolean, boolean)
     */
    void setPropertyValues(PropertyValues pvs) throws BeansException;

    /**
     * 执行批量更新，对行为进行更多控制<br/>
     * 请注意，执行批更新与执行单个更新的不同之处在于，如果遇到可恢复错误(例如类型不匹配，但不是无效的字段名等)，
     * 则此类的实现将继续更新属性，引发包含所有单个错误的{@link PropertyBatchUpdateException}。
     * 稍后可以检查此异常以查看所有绑定错误。已成功更新的属性仍保持更改状态
     *
     * @param pvs           要在目标对象上设置的属性值
     * @param ignoreUnknown 我们是否应该忽略未知属性(在bean中找不到)
     * @throws InvalidPropertyException     如果没有此类属性或属性不可写
     * @throws PropertyBatchUpdateException 如果在批处理更新期间，特定属性发生一个或多个PropertyAccessException。此异常捆绑所有单个PropertyAccessExceptions。将成功更新所有其他属性
     * @see #setPropertyValues(PropertyValues, boolean, boolean)
     */
    void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException;

    /**
     * 在完全控制行为的情况下执行批处理更新<br/>
     * 请注意，执行批更新与执行单个更新的不同之处在于，如果遇到可恢复错误(例如类型不匹配，但不是无效的字段名等)，
     * 则此类的实现将继续更新属性，引发包含所有单个错误的{@link PropertyBatchUpdateException}。
     * 稍后可以检查此异常以查看所有绑定错误。已成功更新的属性仍保持更改状态
     *
     * @param pvs           要在目标对象上设置的属性值
     * @param ignoreUnknown 我们是否应该忽略未知属性(在bean中找不到)
     * @param ignoreInvalid 我们是否应该忽略无效属性(找到但不可访问)
     * @throws InvalidPropertyException     如果没有此类属性或属性不可写
     * @throws PropertyBatchUpdateException 如果在批处理更新期间，特定属性发生一个或多个PropertyAccessException。此异常捆绑所有单个PropertyAccessExceptions。将成功更新所有其他属性
     */
    void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid) throws BeansException;
}
