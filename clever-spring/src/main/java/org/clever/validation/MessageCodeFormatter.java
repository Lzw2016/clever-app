package org.clever.validation;

/**
 * 用于格式化消息代码的策略接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:54 <br/>
 */
@FunctionalInterface
public interface MessageCodeFormatter {
    /**
     * 构建并返回由给定字段组成的消息代码，通常由 {@link DefaultMessageCodesResolver#CODE_SEPARATOR} 分隔。
     *
     * @param errorCode  例如: "typeMismatch"
     * @param objectName 例如: "user"
     * @param field      例如: "age"
     * @return 连接的消息代码，例如: "typeMismatch.user.age"
     * @see DefaultMessageCodesResolver.Format
     */
    String format(String errorCode, String objectName, String field);
}
