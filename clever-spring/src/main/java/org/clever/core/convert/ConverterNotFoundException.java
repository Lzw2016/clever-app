package org.clever.core.convert;

/**
 * 在给定的转换服务中找不到合适的转换器时引发的异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:13 <br/>
 */
public class ConverterNotFoundException extends ConversionException {
    private final TypeDescriptor sourceType;
    private final TypeDescriptor targetType;

    /**
     * 创建新的转换执行器未找到异常
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     */
    public ConverterNotFoundException(TypeDescriptor sourceType, TypeDescriptor targetType) {
        super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
        this.sourceType = sourceType;
        this.targetType = targetType;
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
}
