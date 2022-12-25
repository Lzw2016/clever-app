package org.clever.util;

/**
 * 在遇到无效的内容类型规范 String 时从 {@link MimeTypeUtils#parseMimeType(String)} 抛出异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 13:41 <br/>
 */
public class InvalidMimeTypeException extends IllegalArgumentException {
    private final String mimeType;

    /**
     * 为给定的内容类型创建一个新的 InvalidContentTypeException
     *
     * @param mimeType 违规媒体类型
     * @param message  指示无效部分的详细消息
     */
    public InvalidMimeTypeException(String mimeType, String message) {
        super("Invalid mime type \"" + mimeType + "\": " + message);
        this.mimeType = mimeType;
    }

    /**
     * 返回有问题的内容类型
     */
    public String getMimeType() {
        return this.mimeType;
    }
}