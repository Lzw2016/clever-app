package org.clever.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * {@link java.io.InputStream}的扩展，允许优化消息摘要的实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:29 <br/>
 */
abstract class UpdateMessageDigestInputStream extends InputStream {
    /**
     * 使用此流中的其余字节更新消息摘要。
     * <p>使用这种方法更为优化，因为它避免了为每个调用创建新的字节数组。
     *
     * @param messageDigest 要更新的消息摘要
     * @throws IOException 从{@link #read()}传播时
     */
    public void updateMessageDigest(MessageDigest messageDigest) throws IOException {
        int data;
        while ((data = read()) != -1) {
            messageDigest.update((byte) data);
        }
    }

    /**
     * 使用此流中的下一个len字节更新消息摘要。
     * <p>使用这种方法更为优化，因为它避免了为每个调用创建新的字节数组。
     *
     * @param messageDigest 要更新的消息摘要
     * @param len           从该流中读取并用于更新消息摘要的字节数
     * @throws IOException 从{@link #read()}传播时
     */
    public void updateMessageDigest(MessageDigest messageDigest, int len) throws IOException {
        int data;
        int bytesRead = 0;
        while (bytesRead < len && (data = read()) != -1) {
            messageDigest.update((byte) data);
            bytesRead++;
        }
    }
}
