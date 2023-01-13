package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.util.Assert;

import java.beans.PropertyEditorSupport;

/**
 * 为任何给定的 {@link org.clever.core.convert.ConversionService} 和特定目标类型公开 {@link java.beans.PropertyEditor} 的适配器。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/01 21:42 <br/>
 */
public class ConvertingPropertyEditorAdapter extends PropertyEditorSupport {
    private final ConversionService conversionService;
    private final TypeDescriptor targetDescriptor;
    private final boolean canConvertToString;

    /**
     * 为给定的 {@link org.clever.core.convert.ConversionService} 和给定的目标类型创建一个新的 ConvertingPropertyEditorAdapter
     *
     * @param conversionService 要委托给的 ConversionService
     * @param targetDescriptor  要转换为的目标类型
     */
    public ConvertingPropertyEditorAdapter(ConversionService conversionService, TypeDescriptor targetDescriptor) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        Assert.notNull(targetDescriptor, "TypeDescriptor must not be null");
        this.conversionService = conversionService;
        this.targetDescriptor = targetDescriptor;
        this.canConvertToString = conversionService.canConvert(this.targetDescriptor, TypeDescriptor.valueOf(String.class));
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(this.conversionService.convert(text, TypeDescriptor.valueOf(String.class), this.targetDescriptor));
    }

    @Override
    public String getAsText() {
        if (this.canConvertToString) {
            return (String) this.conversionService.convert(getValue(), this.targetDescriptor, TypeDescriptor.valueOf(String.class));
        } else {
            return null;
        }
    }
}
