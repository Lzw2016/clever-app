package org.clever.context.support;

import org.clever.context.MessageSourceResolvable;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.io.Serializable;

/**
 * 提供一种简单的方法来存储通过 {@link org.clever.context.MessageSource} 解析消息所需的所有必要值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:08 <br/>
 */
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {
    private final String[] codes;
    private final Object[] arguments;
    private final String defaultMessage;

    /**
     * @param code 用于解析此消息的代码
     */
    public DefaultMessageSourceResolvable(String code) {
        this(new String[]{code}, null, null);
    }

    /**
     * @param codes 用于解析此消息的代码
     */
    public DefaultMessageSourceResolvable(String[] codes) {
        this(codes, null, null);
    }

    /**
     * @param codes          用于解析此消息的代码
     * @param defaultMessage 用于解析此消息的默认消息
     */
    public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
        this(codes, null, defaultMessage);
    }

    /**
     * @param codes     用于解析此消息的代码
     * @param arguments 用于解析此消息的参数数组
     */
    public DefaultMessageSourceResolvable(String[] codes, Object[] arguments) {
        this(codes, arguments, null);
    }

    /**
     * @param codes          用于解析此消息的代码
     * @param arguments      用于解析此消息的参数数组
     * @param defaultMessage 用于解析此消息的默认消息
     */
    public DefaultMessageSourceResolvable(String[] codes, Object[] arguments, String defaultMessage) {
        this.codes = codes;
        this.arguments = arguments;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 复制构造函数：从另一个可解析对象创建一个新实例。
     *
     * @param resolvable 要从中复制的可解析对象
     */
    public DefaultMessageSourceResolvable(MessageSourceResolvable resolvable) {
        this(resolvable.getCodes(), resolvable.getArguments(), resolvable.getDefaultMessage());
    }

    /**
     * 返回此 resolvable 的默认代码，即 codes 数组中的最后一个代码。
     */
    public String getCode() {
        return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
    }

    @Override
    public String[] getCodes() {
        return this.codes;
    }

    @Override
    public Object[] getArguments() {
        return this.arguments;
    }

    @Override
    public String getDefaultMessage() {
        return this.defaultMessage;
    }

    /**
     * 指示是否需要呈现指定的默认消息以替换占位符和/或 {@link java.text.MessageFormat} 转义。
     *
     * @return {@code true} 如果默认消息可能包含参数占位符； {@code false} 如果它绝对不包含占位符或自定义转义，因此可以简单地按原样公开
     * @see #getDefaultMessage()
     * @see #getArguments()
     */
    public boolean shouldRenderDefaultMessage() {
        return true;
    }

    /**
     * 为此 MessageSourceResolvable 构建一个默认的字符串表示形式：包括代码、参数和默认消息。
     */
    protected final String resolvableToString() {
        return "codes [" + StringUtils.arrayToDelimitedString(this.codes, ",") + "]; " +
                "arguments [" + StringUtils.arrayToDelimitedString(this.arguments, ",") + "]; " +
                "default message [" + this.defaultMessage + ']';
    }

    /**
     * 默认实现公开此 MessageSourceResolvable 的属性。
     * <p>在更具体的子类中被覆盖，可能通过 {@code resolvableToString()} 包含可解析的内容。
     *
     * @see #resolvableToString()
     */
    @Override
    public String toString() {
        return getClass().getName() + ": " + resolvableToString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MessageSourceResolvable)) {
            return false;
        }
        MessageSourceResolvable otherResolvable = (MessageSourceResolvable) other;
        return ObjectUtils.nullSafeEquals(getCodes(), otherResolvable.getCodes())
                && ObjectUtils.nullSafeEquals(getArguments(), otherResolvable.getArguments())
                && ObjectUtils.nullSafeEquals(getDefaultMessage(), otherResolvable.getDefaultMessage());
    }

    @Override
    public int hashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(getCodes());
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArguments());
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getDefaultMessage());
        return hashCode;
    }
}
