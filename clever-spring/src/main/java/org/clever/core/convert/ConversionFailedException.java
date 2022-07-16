package org.clever.core.convert;

import org.clever.util.ObjectUtils;

/**
 * 实际类型转换尝试失败时引发的异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:12 <br/>
 */
public class ConversionFailedException extends ConversionException {
    private final TypeDescriptor sourceType;
    private final TypeDescriptor targetType;
    private final Object value;

    /**
     * 创建转换异常
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @param value      源对象值
     * @param cause      转换失败的原因
     */
    public ConversionFailedException(TypeDescriptor sourceType, TypeDescriptor targetType, Object value, Throwable cause) {
        super("Failed to convert from type [" + sourceType + "] to type [" + targetType + "] for value '" + ObjectUtils.nullSafeToString(value) + "'", cause);
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.value = value;
    }

    /**
     * 源类型
     */
    public TypeDescriptor getSourceType() {
        return this.sourceType;
    }

    /**
     * 目标类型
     */
    public TypeDescriptor getTargetType() {
        return this.targetType;
    }

    /**
     * 源对象值
     */
    public Object getValue() {
        return this.value;
    }
}
