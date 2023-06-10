package org.clever.validation;

/**
 * 用于从验证错误代码构建消息代码的策略接口。 DataBinder 使用它来构建 ObjectErrors 和 FieldErrors 的代码列表。
 * <p>生成的消息代码对应于 MessageSourceResolvable 的代码（由 ObjectError 和 FieldError 实现）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:51 <br/>
 *
 * @see DataBinder#setMessageCodesResolver
 * @see ObjectError
 * @see FieldError
 */
public interface MessageCodesResolver {
    /**
     * 为给定的错误代码和对象名称构建消息代码。用于构建 ObjectError 的代码列表。
     *
     * @param errorCode  用于拒绝对象的错误代码
     * @param objectName 对象的名称
     * @return 要使用的消息代码
     */
    String[] resolveMessageCodes(String errorCode, String objectName);

    /**
     * 为给定的错误代码和字段规范构建消息代码。用于构建 FieldError 的代码列表。
     *
     * @param errorCode  用于拒绝该值的错误代码
     * @param objectName 对象的名称
     * @param field      字段名称
     * @param fieldType  字段类型（如果无法确定，则可能是 {@code null}）
     * @return 要使用的消息代码
     */
    String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType);
}
