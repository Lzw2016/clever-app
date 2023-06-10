package org.clever.validation;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 封装字段错误，即拒绝特定字段值的原因。
 * <p>有关如何为 {@code FieldError} 构建消息代码列表的详细信息，请参阅 {@link DefaultMessageCodesResolver}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:19 <br/>
 *
 * @see DefaultMessageCodesResolver
 */
public class FieldError extends ObjectError {
    private final String field;
    private final Object rejectedValue;
    private final boolean bindingFailure;

    /**
     * @param objectName     受影响对象的名称
     * @param field          对象的影响域
     * @param defaultMessage 用于解析此消息的默认消息
     */
    public FieldError(String objectName, String field, String defaultMessage) {
        this(objectName, field, null, false, null, null, defaultMessage);
    }

    /**
     * @param objectName     受影响对象的名称
     * @param field          对象的影响域
     * @param rejectedValue  被拒绝的字段值
     * @param bindingFailure 此错误是否表示绑定失败（如类型不匹配）；否则，这是验证失败
     * @param codes          用于解析此消息的代码
     * @param arguments      用于解析此消息的参数数组
     * @param defaultMessage 用于解析此消息的默认消息
     */
    public FieldError(String objectName, String field, Object rejectedValue, boolean bindingFailure, String[] codes, Object[] arguments, String defaultMessage) {
        super(objectName, codes, arguments, defaultMessage);
        Assert.notNull(field, "Field must not be null");
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.bindingFailure = bindingFailure;
    }

    /**
     * 返回对象的受影响字段。
     */
    public String getField() {
        return this.field;
    }

    /**
     * 返回被拒绝的字段值。
     */
    public Object getRejectedValue() {
        return this.rejectedValue;
    }

    /**
     * 返回此错误是否表示绑定失败（如类型不匹配）；否则就是验证失败。
     */
    public boolean isBindingFailure() {
        return this.bindingFailure;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        FieldError otherError = (FieldError) other;
        return getField().equals(otherError.getField())
                && ObjectUtils.nullSafeEquals(getRejectedValue(), otherError.getRejectedValue())
                && isBindingFailure() == otherError.isBindingFailure();
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 29 * hashCode + getField().hashCode();
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getRejectedValue());
        hashCode = 29 * hashCode + (isBindingFailure() ? 1 : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return "Field error in object '" + getObjectName() +
                "' on field '" + this.field + "': rejected value [" + ObjectUtils.nullSafeToString(this.rejectedValue) + "]; " +
                resolvableToString();
    }
}
