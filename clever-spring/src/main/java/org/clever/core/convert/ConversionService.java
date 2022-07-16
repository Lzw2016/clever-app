package org.clever.core.convert;

/**
 * 类型转换服务。这是转换系统的入口点<br/>
 * 调用{@link #convert(Object, Class)}来使用这个系统执行线程安全的类型转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:17 <br/>
 */
public interface ConversionService {
    /**
     * 如果sourceType的对象可以转换为targetType，则返回true<br/>
     * 如果此方法返回true，则表示{@link #convert(Object, Class)}能够将sourceType的实例转换为targetType<br/>
     * <p>
     * 对于集合、数组和Map类型之间的转换，即使在基础元素不可转换的情况下，此方法将返回true，
     * 转换调用仍可能出现ConversionException，调用方需要处理这种例外情况下的异常
     *
     * @param sourceType 要从中转换的源类型(如果源对象为null，则可能为null)
     * @param targetType 要转换为的目标类型(必需)
     * @return 如果可以执行转换，则为true，否则为false
     * @throws IllegalArgumentException 如果targetType为null
     */
    boolean canConvert(Class<?> sourceType, Class<?> targetType);

    /**
     * 如果{@code sourceType}对象可以转换为{@code targetType}，则返回true<br/>
     * TypeDescriptor提供了有关发生转换的源和目标的额外上下文，通常是对象字段或属性<br/>
     * <p>
     * 如果此方法返回true，则意味着{@link #convert(Object, TypeDescriptor, TypeDescriptor)}能够将sourceType的实例转换为targetType<br/>
     * <p>
     * 对于集合、数组和Map类型之间的转换，即使在基础元素不可转换的情况下，此方法将返回true，
     * 转换调用仍可能出现ConversionException，调用方需要处理这种例外情况下的异常
     *
     * @param sourceType 源类型的上下文(如果源对象为null，则可能为null)
     * @param targetType 目标类型的上下文(必需)
     * @return 如果可以执行转换，则为true，否则为false
     * @throws IllegalArgumentException 如果targetType为null
     */
    boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

    /**
     * 把source对象转换为{@code targetType}类型
     *
     * @param source     要转换的source对象(可能为null)
     * @param targetType 要转换为的目标类型(必需)
     * @return 转换后的对象，targetType的一个实例
     * @throws ConversionException      如果发生转换异常
     * @throws IllegalArgumentException 如果targetType为null
     */
    <T> T convert(Object source, Class<T> targetType);

    /**
     * 把source对象转换为{@code targetType}类型<br/>
     * TypeDescriptor提供了有关将发生转换的源和目标位置的额外上下文，通常是对象字段或属性
     *
     * @param source     要转换的source对象(可能为null)
     * @param sourceType 源类型的上下文(如果源对象为null，则可能为null)
     * @param targetType 目标类型的上下文(必需)
     * @return 转换后的对象，{@link TypeDescriptor#getObjectType() targetType}的一个实例
     * @throws ConversionException      如果发生转换异常
     * @throws IllegalArgumentException 如果targetType为null，或者sourceType为null但source不是null
     */
    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);
}
