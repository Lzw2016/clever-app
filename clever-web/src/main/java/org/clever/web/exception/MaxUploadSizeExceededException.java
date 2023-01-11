package org.clever.web.exception;

/**
 * 当上传超过允许的最大上传大小时抛出 MultipartException 子类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/11 15:37 <br/>
 */
public class MaxUploadSizeExceededException extends MultipartException {
    private final long maxUploadSize;

    /**
     * MaxUploadSizeExceededException 的构造函数
     *
     * @param maxUploadSize 允许的最大上传大小，如果大小限制未知，则为 -1
     */
    public MaxUploadSizeExceededException(long maxUploadSize) {
        this(maxUploadSize, null);
    }

    /**
     * MaxUploadSizeExceededException 的构造函数
     *
     * @param maxUploadSize 允许的最大上传大小，如果大小限制未知，则为 -1
     * @param ex            使用中的多部分解析 API 的根本原因
     */
    public MaxUploadSizeExceededException(long maxUploadSize, Throwable ex) {
        super("Maximum upload size " + (maxUploadSize >= 0 ? "of " + maxUploadSize + " bytes " : "") + "exceeded", ex);
        this.maxUploadSize = maxUploadSize;
    }

    /**
     * 返回允许的最大上传大小，如果大小限制未知，则返回 -1。
     */
    public long getMaxUploadSize() {
        return this.maxUploadSize;
    }
}
