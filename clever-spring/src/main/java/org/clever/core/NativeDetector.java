package org.clever.core;

/**
 * 用于检测GraalVM环境<br/>
 * 使用{@code -H:+InlineBeforeAnalysis}编译标识<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 11:39 <br/>
 */
public class NativeDetector {
    // See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
    private static final boolean imageCode = (System.getProperty("org.graalvm.nativeimage.imagecode") != null);

    /**
     * 当前是否是NativeImage环境中(主要是graalvm环境)
     */
    public static boolean inNativeImage() {
        return imageCode;
    }
}
