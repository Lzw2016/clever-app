package org.clever.context;

import java.util.Locale;

/**
 * 用于解析消息的策略接口，支持此类消息的参数化和国际化。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:07 <br/>
 */
public interface MessageSource {
    /**
     * 尝试解决消息。如果未找到消息，则返回默认消息。
     *
     * @param code           要查找的消息代码，例如 'calculator.noRateSet'。鼓励 MessageSource 用户将消息名称基于合格的类或包名称，避免潜在的冲突并确保最大程度的清晰度。
     * @param args           将为消息中的参数填充的参数数组（参数在消息中类似于“{0}”、“{1,date}”、“{2,time}”）或 {@code null}如果没有
     * @param defaultMessage 查找失败时返回的默认消息
     * @param locale         进行查找的语言环境
     * @return 如果查找成功，则为已解析的消息，否则默认消息作为参数传递（可能是 {@code null}）
     * @see #getMessage(MessageSourceResolvable, Locale)
     * @see java.text.MessageFormat
     */
    String getMessage(String code, Object[] args, String defaultMessage, Locale locale);

    /**
     * 尝试解决消息。如果找不到消息，则视为错误。
     *
     * @param code   要查找的消息代码，例如 'calculator.noRateSet'。鼓励 MessageSource 用户将消息名称基于合格的类或包名称，避免潜在的冲突并确保最大程度的清晰度。
     * @param args   将为消息中的参数填充的参数数组（参数在消息中类似于“{0}”、“{1,date}”、“{2,time}”）或 {@code null}如果没有
     * @param locale 进行查找的语言环境
     * @return 已解决的消息（永远不会 {@code null}）
     * @throws NoSuchMessageException 如果没有找到对应的消息
     * @see #getMessage(MessageSourceResolvable, Locale)
     * @see java.text.MessageFormat
     */
    String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException;

    /**
     * 尝试使用传入的 {@code MessageSourceResolvable} 参数中包含的所有属性来解析消息。
     * <p>注意：我们必须在此方法上抛出 {@code NoSuchMessageException}，因为在调用此方法时我们无法确定可解析对象的 {@code defaultMessage} 属性是否为 {@code null}。
     *
     * @param resolvable 存储解析消息所需的属性的值对象（可能包括默认消息）
     * @param locale     进行查找的语言环境
     * @return 已解析的消息（永远不会 {@code null} 因为即使是 {@code MessageSourceResolvable} 提供的默认消息也需要是非空的）
     * @throws NoSuchMessageException 如果没有找到相应的消息（并且 {@code MessageSourceResolvable} 没有提供默认消息）
     * @see MessageSourceResolvable#getCodes()
     * @see MessageSourceResolvable#getArguments()
     * @see MessageSourceResolvable#getDefaultMessage()
     * @see java.text.MessageFormat
     */
    String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;
}
