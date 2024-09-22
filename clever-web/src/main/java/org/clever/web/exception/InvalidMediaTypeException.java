//package org.clever.web.exception;
//
//import org.clever.util.InvalidMimeTypeException;
//import org.clever.web.http.MediaType;
//
///**
// * 如果遇到无效的媒体类型规范字符串，则从 {@link MediaType#parseMediaType(String)} 抛出异常。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/12/25 14:05 <br/>
// */
//public class InvalidMediaTypeException extends IllegalArgumentException {
//    private final String mediaType;
//
//    /**
//     * 为给定的媒体类型创建一个新的 InvalidMediaTypeException
//     *
//     * @param mediaType 违规媒体类型
//     * @param message   指示无效部分的详细消息
//     */
//    public InvalidMediaTypeException(String mediaType, String message) {
//        super("Invalid media type \"" + mediaType + "\": " + message);
//        this.mediaType = mediaType;
//    }
//
//    /**
//     * 允许包装 {@link InvalidMimeTypeException} 的构造函数
//     */
//    public InvalidMediaTypeException(InvalidMimeTypeException ex) {
//        super(ex.getMessage(), ex);
//        this.mediaType = ex.getMimeType();
//    }
//
//    /**
//     * 返回有问题的媒体类型
//     */
//    public String getMediaType() {
//        return this.mediaType;
//    }
//}
