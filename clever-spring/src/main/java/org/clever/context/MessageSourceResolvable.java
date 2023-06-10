package org.clever.context;

/**
 * 适用于 {@link MessageSource} 中消息解析的对象的接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:05 <br/>
 */
@FunctionalInterface
public interface MessageSourceResolvable {
    /**
     * 按照应尝试的顺序返回用于解析此消息的代码。因此，最后一个代码将是默认代码。
     *
     * @return 与此消息关联的代码的字符串数组
     */
    String[] getCodes();

    /**
     * 返回用于解析此消息的参数数组。
     * <p>默认实现只返回 {@code null}。
     *
     * @return 用作参数的对象数组，用于替换消息文本中的占位符
     * @see java.text.MessageFormat
     */
    default Object[] getArguments() {
        return null;
    }

    /**
     * 返回用于解析此消息的默认消息。
     *
     * @return 默认消息，如果没有默认消息，则为 {@code null}
     */
    default String getDefaultMessage() {
        return null;
    }
}
