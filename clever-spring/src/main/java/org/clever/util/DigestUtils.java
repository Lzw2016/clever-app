package org.clever.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 计算摘要的其他方法。
 *
 * <p>主要用于框架内的内部使用；考虑
 * <a href="https://commons.apache.org/codec/">Apache Commons Codec</a>
 * 以获得更全面的摘要实用程序套件。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:28 <br/>
 */
public abstract class DigestUtils {
    private static final String MD5_ALGORITHM_NAME = "MD5";
    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * 计算给定字节的MD5摘要。
     *
     * @param bytes 计算摘要的字节数
     * @return MD5摘要
     */
    public static byte[] md5Digest(byte[] bytes) {
        return digest(MD5_ALGORITHM_NAME, bytes);
    }

    /**
     * 计算给定流的MD5摘要。
     * <p>此方法不会关闭输入流。
     *
     * @param inputStream 用于计算摘要的InputStream
     */
    public static byte[] md5Digest(InputStream inputStream) throws IOException {
        return digest(MD5_ALGORITHM_NAME, inputStream);
    }

    /**
     * 返回给定字节的MD5摘要的十六进制字符串表示形式。
     *
     * @param bytes 计算摘要的字节数
     * @return 十六进制摘要字符串
     */
    public static String md5DigestAsHex(byte[] bytes) {
        return digestAsHexString(MD5_ALGORITHM_NAME, bytes);
    }

    /**
     * 返回给定流的MD5摘要的十六进制字符串表示形式。
     * <p>此方法不会关闭输入流。
     *
     * @param inputStream 用于计算摘要的InputStream
     * @return 十六进制摘要字符串
     */
    public static String md5DigestAsHex(InputStream inputStream) throws IOException {
        return digestAsHexString(MD5_ALGORITHM_NAME, inputStream);
    }

    /**
     * 将给定字节的MD5摘要的十六进制字符串表示形式附加到给定的 {@link StringBuilder}.
     *
     * @param bytes   计算摘要的字节数
     * @param builder 要将摘要附加到的字符串生成器
     * @return 给定的字符串生成器
     */
    public static StringBuilder appendMd5DigestAsHex(byte[] bytes, StringBuilder builder) {
        return appendDigestAsHex(MD5_ALGORITHM_NAME, bytes, builder);
    }

    /**
     * 将给定inputStream的MD5摘要的十六进制字符串表示形式附加到给定的 {@link StringBuilder}.
     * <p>此方法不会关闭输入流。
     *
     * @param inputStream 用于计算摘要的inputStream
     * @param builder     要将摘要附加到的字符串生成器
     * @return 给定的字符串生成器
     */
    public static StringBuilder appendMd5DigestAsHex(InputStream inputStream, StringBuilder builder) throws IOException {
        return appendDigestAsHex(MD5_ALGORITHM_NAME, inputStream, builder);
    }

    /**
     * 使用给定的算法创建新的{@link MessageDigest}。
     * <p>必要，因为MessageDigest不是线程安全的。
     */
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + algorithm + "\"", ex);
        }
    }

    private static byte[] digest(String algorithm, byte[] bytes) {
        return getDigest(algorithm).digest(bytes);
    }

    private static byte[] digest(String algorithm, InputStream inputStream) throws IOException {
        MessageDigest messageDigest = getDigest(algorithm);
        if (inputStream instanceof UpdateMessageDigestInputStream) {
            ((UpdateMessageDigestInputStream) inputStream).updateMessageDigest(messageDigest);
            return messageDigest.digest();
        } else {
            final byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        }
    }

    private static String digestAsHexString(String algorithm, byte[] bytes) {
        char[] hexDigest = digestAsHexChars(algorithm, bytes);
        return new String(hexDigest);
    }

    private static String digestAsHexString(String algorithm, InputStream inputStream) throws IOException {
        char[] hexDigest = digestAsHexChars(algorithm, inputStream);
        return new String(hexDigest);
    }

    private static StringBuilder appendDigestAsHex(String algorithm, byte[] bytes, StringBuilder builder) {
        char[] hexDigest = digestAsHexChars(algorithm, bytes);
        return builder.append(hexDigest);
    }

    private static StringBuilder appendDigestAsHex(String algorithm, InputStream inputStream, StringBuilder builder) throws IOException {
        char[] hexDigest = digestAsHexChars(algorithm, inputStream);
        return builder.append(hexDigest);
    }

    private static char[] digestAsHexChars(String algorithm, byte[] bytes) {
        byte[] digest = digest(algorithm, bytes);
        return encodeHex(digest);
    }

    private static char[] digestAsHexChars(String algorithm, InputStream inputStream) throws IOException {
        byte[] digest = digest(algorithm, inputStream);
        return encodeHex(digest);
    }

    private static char[] encodeHex(byte[] bytes) {
        char[] chars = new char[32];
        for (int i = 0; i < chars.length; i = i + 2) {
            byte b = bytes[i / 2];
            chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i + 1] = HEX_CHARS[b & 0xf];
        }
        return chars;
    }
}
