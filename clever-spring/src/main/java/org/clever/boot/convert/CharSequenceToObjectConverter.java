package org.clever.boot.convert;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.util.Collections;
import java.util.Set;

/**
 * {@link ConditionalGenericConverter}过委托给现有的{@link String}转换器来转换{@link CharSequence}类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:58 <br/>
 */
class CharSequenceToObjectConverter implements ConditionalGenericConverter {
    private static final TypeDescriptor STRING = TypeDescriptor.valueOf(String.class);
    private static final TypeDescriptor BYTE_ARRAY = TypeDescriptor.valueOf(byte[].class);
    private static final Set<ConvertiblePair> TYPES;

    static {
        TYPES = Collections.singleton(new ConvertiblePair(CharSequence.class, Object.class));
    }

    private final ThreadLocal<Boolean> disable = new ThreadLocal<>();
    private final ConversionService conversionService;

    CharSequenceToObjectConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return TYPES;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (sourceType.getType() == String.class || this.disable.get() == Boolean.TRUE) {
            return false;
        }
        this.disable.set(Boolean.TRUE);
        try {
            boolean canDirectlyConvertCharSequence = this.conversionService.canConvert(sourceType, targetType);
            if (canDirectlyConvertCharSequence && !isStringConversionBetter(sourceType, targetType)) {
                return false;
            }
            return this.conversionService.canConvert(STRING, targetType);
        } finally {
            this.disable.set(null);
        }
    }

    /**
     * 如果基于字符串的转换更好地基于目标类型，则返回。当反对。。。转换产生不正确的结果。
     *
     * @param sourceType 要测试的源类型
     * @param targetType 要测试的目标类型
     * @return 如果字符串转换更好
     */
    private boolean isStringConversionBetter(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (this.conversionService instanceof ApplicationConversionService) {
            ApplicationConversionService applicationConversionService = (ApplicationConversionService) this.conversionService;
            if (applicationConversionService.isConvertViaObjectSourceType(sourceType, targetType)) {
                // If an ObjectTo... converter is being used then there might be a better
                // StringTo... version
                return true;
            }
        }
        // noinspection RedundantIfStatement
        if ((targetType.isArray() || targetType.isCollection()) && !targetType.equals(BYTE_ARRAY)) {
            // StringToArrayConverter / StringToCollectionConverter are better than
            // ObjectToArrayConverter / ObjectToCollectionConverter
            return true;
        }
        return false;
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.conversionService.convert(source.toString(), STRING, targetType);
    }
}
