package org.clever.util;

import java.io.*;
import java.nio.charset.Charset;

/**
 * 处理流的简单实用方法。此类的复制方法与{@link FileCopyUtils}中定义的复制方法类似，
 * 只是完成后所有受影响的流都保持打开状态。所有复制方法都使用4096字节的块大小。
 *
 * <p>主要用于框架内，也适用于应用程序代码。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:56 <br/>
 *
 * @see FileCopyUtils
 */
public abstract class StreamUtils {
    /**
     * 复制字节时使用的默认缓冲区大小。
     */
    public static final int BUFFER_SIZE = 4096;
    private static final byte[] EMPTY_CONTENT = new byte[0];

    /**
     * 将给定InputStream的内容复制到新的字节数组中。
     * <p>完成后保持流打开。
     *
     * @param in 要从中复制的流（可以为null或空）
     * @return 已复制到的新字节数组（可能为空）
     * @throws IOException 在IO错误的情况下
     */
    public static byte[] copyToByteArray(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
        copy(in, out);
        return out.toByteArray();
    }

    /**
     * 将给定InputStream的内容复制到字符串中。
     * <p>完成后保持流打开。
     *
     * @param in      要从中复制的InputStream（可以为null或空）
     * @param charset 用于解码字节的字符集
     * @return 已复制到的字符串（可能为空）
     * @throws IOException 在IO错误的情况下
     */
    public static String copyToString(InputStream in, Charset charset) throws IOException {
        if (in == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(BUFFER_SIZE);
        InputStreamReader reader = new InputStreamReader(in, charset);
        char[] buffer = new char[BUFFER_SIZE];
        int charsRead;
        while ((charsRead = reader.read(buffer)) != -1) {
            out.append(buffer, 0, charsRead);
        }
        return out.toString();
    }

    /**
     * 将给定ByteArrayOutputStream的内容复制到字符串中。
     * <p>这是{@code new String(baos.toByteArray(), charset)}的更有效等效值。
     *
     * @param baos    要复制到字符串中的ByteArrayOutputStream
     * @param charset 用于解码字节的字符集
     * @return 已复制到的字符串（可能为空）
     */
    public static String copyToString(ByteArrayOutputStream baos, Charset charset) {
        Assert.notNull(baos, "No ByteArrayOutputStream specified");
        Assert.notNull(charset, "No Charset specified");
        try {
            // Can be replaced with toString(Charset) call in Java 10+
            return baos.toString(charset.name());
        } catch (UnsupportedEncodingException ex) {
            // Should never happen
            throw new IllegalArgumentException("Invalid charset name: " + charset, ex);
        }
    }

    /**
     * 将给定字节数组的内容复制到给定的OutputStream。
     * <p>完成后保持流打开。
     *
     * @param in  要从中复制的字节数组
     * @param out 要复制到的输出流
     * @throws IOException 在IO错误的情况下
     */
    public static void copy(byte[] in, OutputStream out) throws IOException {
        Assert.notNull(in, "No input byte array specified");
        Assert.notNull(out, "No OutputStream specified");
        out.write(in);
        out.flush();
    }

    /**
     * 将给定字符串的内容复制到给定的OutputStream。
     * <p>完成后保持流打开。
     *
     * @param in      要从中复制的字符串
     * @param charset 字符集
     * @param out     要复制到的输出流
     * @throws IOException 在IO错误的情况下
     */
    public static void copy(String in, Charset charset, OutputStream out) throws IOException {
        Assert.notNull(in, "No input String specified");
        Assert.notNull(charset, "No Charset specified");
        Assert.notNull(out, "No OutputStream specified");
        Writer writer = new OutputStreamWriter(out, charset);
        writer.write(in);
        writer.flush();
    }

    /**
     * 将给定InputStream的内容复制到给定OutputStream。
     * <p>完成后使两条流都打开。
     *
     * @param in  要从中复制的输入流
     * @param out 要复制到的输出流
     * @return 复制的字节数
     * @throws IOException 在IO错误的情况下
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");
        int byteCount = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

    /**
     * 将给定InputStream的内容范围复制到给定OutputStream。
     * <p>如果指定的范围超过InputStream的长度，则复制到流的末尾，并返回复制的实际字节数。
     * <p>完成后使两条流都打开。
     *
     * @param in    要从中复制的输入流
     * @param out   要复制到的输出流
     * @param start 开始复制的位置
     * @param end   结束复制的位置
     * @return 复制的字节数
     * @throws IOException 在IO错误的情况下
     */
    public static long copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");
        long skipped = in.skip(start);
        if (skipped < start) {
            throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required");
        }
        long bytesToCopy = end - start + 1;
        byte[] buffer = new byte[(int) Math.min(StreamUtils.BUFFER_SIZE, bytesToCopy)];
        while (bytesToCopy > 0) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            } else if (bytesRead <= bytesToCopy) {
                out.write(buffer, 0, bytesRead);
                bytesToCopy -= bytesRead;
            } else {
                out.write(buffer, 0, (int) bytesToCopy);
                bytesToCopy = 0;
            }
        }
        return (end - start + 1 - bytesToCopy);
    }

    /**
     * 排出给定输入流的剩余内容。
     * <p>完成后保持输入流打开。
     *
     * @param in 要排出的输入流
     * @return 读取的字节数
     * @throws IOException 在IO错误的情况下
     */
    public static int drain(InputStream in) throws IOException {
        Assert.notNull(in, "No InputStream specified");
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        int byteCount = 0;
        while ((bytesRead = in.read(buffer)) != -1) {
            byteCount += bytesRead;
        }
        return byteCount;
    }

    /**
     * 返回有效的空InputStream。
     *
     * @return 基于空字节数组的ByteArrayInputStream
     */
    public static InputStream emptyInput() {
        return new ByteArrayInputStream(EMPTY_CONTENT);
    }

    /**
     * 返回给定InputStream的变体，其中调用{@link InputStream#close() close()}无效。
     *
     * @param in 要装饰的输入流
     * @return 忽略关闭调用的InputStream版本
     */
    public static InputStream nonClosing(InputStream in) {
        Assert.notNull(in, "No InputStream specified");
        return new NonClosingInputStream(in);
    }

    /**
     * 返回给定OutputStream的变体，其中调用{@link OutputStream#close() close()}无效。
     *
     * @param out 要装饰的输出流
     * @return OutputStream的一个版本，忽略关闭调用
     */
    public static OutputStream nonClosing(OutputStream out) {
        Assert.notNull(out, "No OutputStream specified");
        return new NonClosingOutputStream(out);
    }

    private static class NonClosingInputStream extends FilterInputStream {
        public NonClosingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class NonClosingOutputStream extends FilterOutputStream {
        public NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int let) throws IOException {
            // It is critical that we override this method for performance
            this.out.write(b, off, let);
        }

        @Override
        public void close() throws IOException {
        }
    }
}
