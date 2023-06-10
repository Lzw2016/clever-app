package org.clever.validation;

import org.clever.beans.PropertyAccessException;
import org.clever.context.support.DefaultMessageSourceResolvable;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

/**
 * 默认 {@link BindingErrorProcessor} 实现。
 * <p>使用“required”错误代码和字段名称来解析缺少字段错误的消息代码。
 * <p>为每个给定的 {@code PropertyAccessException} 创建一个 {@code FieldError}，使用 {@code PropertyAccessException} 的错误代码（“typeMismatch”、“methodInvocation”）解析消息代码。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 22:15 <br/>
 *
 * @see #MISSING_FIELD_ERROR_CODE
 * @see DataBinder#setBindingErrorProcessor
 * @see BeanPropertyBindingResult#addError
 * @see BeanPropertyBindingResult#resolveMessageCodes
 * @see org.clever.beans.PropertyAccessException#getErrorCode
 * @see org.clever.beans.TypeMismatchException#ERROR_CODE
 * @see org.clever.beans.MethodInvocationException#ERROR_CODE
 */
public class DefaultBindingErrorProcessor implements BindingErrorProcessor {
    /**
     * 将注册缺少字段错误（即在属性值列表中找不到必填字段）的错误代码：“required”。
     */
    public static final String MISSING_FIELD_ERROR_CODE = "required";

    @Override
    public void processMissingFieldError(String missingField, BindingResult bindingResult) {
        // 使用代码“required”创建字段错误。
        String fixedField = bindingResult.getNestedPath() + missingField;
        String[] codes = bindingResult.resolveMessageCodes(MISSING_FIELD_ERROR_CODE, missingField);
        Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), fixedField);
        FieldError error = new FieldError(
                bindingResult.getObjectName(), fixedField, "", true, codes, arguments, "Field '" + fixedField + "' is required"
        );
        bindingResult.addError(error);
    }

    @Override
    public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
        // 使用异常代码创建字段错误，例如“typeMismatch”。
        String field = ex.getPropertyName();
        Assert.state(field != null, "No field in exception");
        String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
        Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), field);
        Object rejectedValue = ex.getValue();
        if (ObjectUtils.isArray(rejectedValue)) {
            rejectedValue = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(rejectedValue));
        }
        FieldError error = new FieldError(bindingResult.getObjectName(), field, rejectedValue, true, codes, arguments, ex.getLocalizedMessage());
        error.wrap(ex);
        bindingResult.addError(error);
    }

    /**
     * 为给定字段上的绑定错误返回 FieldError 参数。为每个缺失的必填字段和每个类型不匹配调用。
     * <p>默认实现返回一个指示字段名称的参数（类型为 DefaultMessageSourceResolvable，代码为“objectName.field”和“field”）。
     *
     * @param objectName 目标对象的名称
     * @param field      导致绑定错误的字段
     * @return 表示 FieldError 参数的 Object 数组
     * @see org.clever.validation.FieldError#getArguments
     * @see org.clever.context.support.DefaultMessageSourceResolvable
     */
    protected Object[] getArgumentsForBindError(String objectName, String field) {
        String[] codes = new String[]{objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
        return new Object[]{new DefaultMessageSourceResolvable(codes, field)};
    }
}
