package org.clever.validation;

import org.clever.beans.BeanUtils;
import org.clever.beans.ConfigurablePropertyAccessor;
import org.clever.beans.PropertyAccessorUtils;
import org.clever.beans.PropertyEditorRegistry;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.support.ConvertingPropertyEditorAdapter;
import org.clever.util.Assert;

import java.beans.PropertyEditor;

/**
 * 与 {@link org.clever.beans.PropertyAccessor} 机制一起工作的 {@link BindingResult} 实现的抽象基类。
 * 通过委托相应的 PropertyAccessor 方法预先实现字段访问。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:43 <br/>
 */
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {
    private transient ConversionService conversionService;

    /**
     * @param objectName 目标对象的名称
     * @see DefaultMessageCodesResolver
     */
    protected AbstractPropertyBindingResult(String objectName) {
        super(objectName);
    }

    public void initConversion(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
        if (getTarget() != null) {
            getPropertyAccessor().setConversionService(conversionService);
        }
    }

    /**
     * 返回基础 PropertyAccessor
     *
     * @see #getPropertyAccessor()
     */
    @Override
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        return (getTarget() != null ? getPropertyAccessor() : null);
    }

    /**
     * 返回规范的属性名称。
     *
     * @see org.clever.beans.PropertyAccessorUtils#canonicalPropertyName
     */
    @Override
    protected String canonicalFieldName(String field) {
        return PropertyAccessorUtils.canonicalPropertyName(field);
    }

    /**
     * 根据属性类型确定字段类型。
     *
     * @see #getPropertyAccessor()
     */
    @Override
    public Class<?> getFieldType(String field) {
        return (getTarget() != null ? getPropertyAccessor().getPropertyType(fixedField(field)) : super.getFieldType(field));
    }

    /**
     * 从 PropertyAccessor 获取字段值。
     *
     * @see #getPropertyAccessor()
     */
    @Override

    protected Object getActualFieldValue(String field) {
        return getPropertyAccessor().getPropertyValue(field);
    }

    /**
     * 根据已注册的 PropertyEditors 格式化字段值。
     *
     * @see #getCustomEditor
     */
    @Override
    protected Object formatFieldValue(String field, Object value) {
        String fixedField = fixedField(field);
        // 尝试自定义编辑器...
        PropertyEditor customEditor = getCustomEditor(fixedField);
        if (customEditor != null) {
            customEditor.setValue(value);
            String textValue = customEditor.getAsText();
            // 如果 PropertyEditor 返回 null，则此值没有合适的文本表示：仅在非 null 时使用它。
            if (textValue != null) {
                return textValue;
            }
        }
        if (this.conversionService != null) {
            // 尝试自定义编辑器...
            TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
            TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
            if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
                return this.conversionService.convert(value, fieldDesc, strDesc);
            }
        }
        return value;
    }

    /**
     * 检索给定字段的自定义 PropertyEditor（如果有）。
     *
     * @param fixedField 完全限定的字段名称
     * @return 自定义 PropertyEditor，或 {@code null}
     */
    protected PropertyEditor getCustomEditor(String fixedField) {
        Class<?> targetType = getPropertyAccessor().getPropertyType(fixedField);
        PropertyEditor editor = getPropertyAccessor().findCustomEditor(targetType, fixedField);
        if (editor == null) {
            editor = BeanUtils.findEditorByConvention(targetType);
        }
        return editor;
    }

    /**
     * 如果适用，此实现会为 Formatter 公开一个 PropertyEditor 适配器。
     */
    @Override
    public PropertyEditor findEditor(String field, Class<?> valueType) {
        Class<?> valueTypeForLookup = valueType;
        if (valueTypeForLookup == null) {
            valueTypeForLookup = getFieldType(field);
        }
        PropertyEditor editor = super.findEditor(field, valueTypeForLookup);
        if (editor == null && this.conversionService != null) {
            TypeDescriptor td = null;
            if (field != null && getTarget() != null) {
                TypeDescriptor ptd = getPropertyAccessor().getPropertyTypeDescriptor(fixedField(field));
                if (ptd != null && (valueType == null || valueType.isAssignableFrom(ptd.getType()))) {
                    td = ptd;
                }
            }
            if (td == null) {
                td = TypeDescriptor.valueOf(valueTypeForLookup);
            }
            if (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class), td)) {
                editor = new ConvertingPropertyEditorAdapter(this.conversionService, td);
            }
        }
        return editor;
    }

    /**
     * 根据访问的具体策略，提供要使用的 PropertyAccessor。
     * <p>请注意，BindingResult 使用的 PropertyAccessor 应始终将其“extractOldValueForEditor”标志默认设置为“true”，因为这通常对用作数据绑定目标的模型对象没有副作用。
     *
     * @see ConfigurablePropertyAccessor#setExtractOldValueForEditor
     */
    public abstract ConfigurablePropertyAccessor getPropertyAccessor();
}
