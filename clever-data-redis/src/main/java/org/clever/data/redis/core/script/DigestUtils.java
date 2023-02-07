package org.clever.data.redis.core.script;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 使用 {@link MessageDigest} 的实用程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:24 <br/>
 */
public abstract class DigestUtils {
    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    /**
     * 返回所提供数据的 SHA1
     *
     * @param data 要计算的数据，例如文件的内容
     * @return 人类可读的 SHA1
     */
    public static String sha1DigestAsHex(String data) {
        byte[] dataBytes = getDigest("SHA").digest(data.getBytes(UTF8_CHARSET));
        return new String(encodeHex(dataBytes));
    }

    private static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = HEX_CHARS[(0xF0 & data[i]) >>> 4];
            out[j++] = HEX_CHARS[0x0F & data[i]];
        }
        return out;
    }

    /**
     * 使用给定的算法创建一个新的 {@link MessageDigest}。必要的，因为 {@code MessageDigest} 不是线程安全的
     */
    @SuppressWarnings("SameParameterValue")
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + algorithm + "\"", ex);
        }
    }
}
