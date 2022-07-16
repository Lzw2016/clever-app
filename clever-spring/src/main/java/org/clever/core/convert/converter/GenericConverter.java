package org.clever.core.convert.converter;

import org.clever.core.convert.TypeDescriptor;
import org.clever.util.Assert;

import java.util.Set;

/**
 * 用于在两种或多种类型之间转换的通用转换器(N:N的转换器)<br/>
 * <p>
 * 这是最灵活的转换器SPI接口，但也是最复杂的<br/>
 * 它的灵活性在于，GenericConverter可能支持在多个源/目标类型对之间进行转换(参见{@link #getConvertibleTypes()})<br/>
 * 此外，GenericConverter实现在类型转换过程中可以访问源/目标字段上下文<br/>
 * 这允许解析源字段和目标字段元数据，例如注解和泛型信息，它们可用于影响转换逻辑<br/>
 * 当更简单的转换器或转换器工厂接口足够时，通常不应使用此接口<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/17 23:24 <br/>
 */
public interface GenericConverter {
    /**
     * 返回此转换器可以转换的源类型和目标类型，每个条目都是可转换的源到目标类型对<br/>
     * 对于{@link ConditionalConverter 条件转换器}，此方法可能返回null，以指示应考虑所有源到目标对<br/>
     */
    Set<ConvertiblePair> getConvertibleTypes();

    /**
     * 将源对象转换为TypeDescriptor描述的目标类型
     *
     * @param source     要转换的源对象(可能为null)
     * @param sourceType 源类型TypeDescriptor描述
     * @param targetType 目标类型TypeDescriptor描述
     * @return 转换后的对象
     */
    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

    /**
     * 源到目标类型对
     */
    final class ConvertiblePair {
        /**
         * 源类型
         */
        private final Class<?> sourceType;
        /**
         * 目标类型
         */
        private final Class<?> targetType;

        /**
         * 创建一个源到目标类型对
         *
         * @param sourceType 源类型
         * @param targetType 目标类型
         */
        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
            Assert.notNull(sourceType, "Source type must not be null");
            Assert.notNull(targetType, "Target type must not be null");
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public Class<?> getSourceType() {
            return this.sourceType;
        }

        public Class<?> getTargetType() {
            return this.targetType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || other.getClass() != ConvertiblePair.class) {
                return false;
            }
            ConvertiblePair otherPair = (ConvertiblePair) other;
            return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
        }

        @Override
        public int hashCode() {
            return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
        }

        @Override
        public String toString() {
            return (this.sourceType.getName() + " -> " + this.targetType.getName());
        }
    }
}
