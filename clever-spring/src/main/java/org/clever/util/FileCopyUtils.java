package org.clever.util;

import java.io.*;
import java.nio.file.Files;

/**
 * 用于文件和流复制的简单实用方法。所有复制方法使用 4096 字节的块大小，完成后关闭所有受影响的流。
 * {@link StreamUtils}中可以找到该类中使流保持打开状态的复制方法的变体。
 *
 * <p>主要用于框架内，也适用于应用程序代码。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:55 <br/>
 *
 * @see StreamUtils
 */
public abstract class FileCopyUtils {
    /**
     * 复制字节时使用的默认缓冲区大小。
     */
    public static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

    //---------------------------------------------------------------------
    // Copy methods for java.io.File
    //---------------------------------------------------------------------

    /**
     * 将给定输入文件的内容复制到给定输出文件。
     *
     * @param in  要从中复制的文件
     * @param out 要复制到的文件
     * @return 复制的字节数
     * @throws IOException 在IO错误的情况下
     */
    public static int copy(File in, File out) throws IOException {
        Assert.notNull(in, "No input File specified");
        Assert.notNull(out, "No output File specified");
        return copy(Files.newInputStream(in.toPath()), Files.newOutputStream(out.toPath()));
    }

    /**
     * 将给定字节数组的内容复制到给定的输出文件。
     *
     * @param in  要从中复制的字节数组
     * @param out 要复制到的文件
     * @throws IOException 在IO错误的情况下
     */
    public static void copy(byte[] in, File out) throws IOException {
        Assert.notNull(in, "No input byte array specified");
        Assert.notNull(out, "No output File specified");
        copy(new ByteArrayInputStream(in), Files.newOutputStream(out.toPath()));
    }

    /**
     * 将给定输入文件的内容复制到新的字节数组中。
     *
     * @param in 要从中复制的文件
     * @return 已复制到的新字节数组
     * @throws IOException 在IO错误的情况下
     */
    public static byte[] copyToByteArray(File in) throws IOException {
        Assert.notNull(in, "No input File specified");
        return copyToByteArray(Files.newInputStream(in.toPath()));
    }

    //---------------------------------------------------------------------
    // Copy methods for java.io.InputStream / java.io.OutputStream
    //---------------------------------------------------------------------

    /**
     * 将给定InputStream的内容复制到给定OutputStream。
     * 完成后关闭两个流。
     *
     * @param in  要从中复制的流
     * @param out 要复制到的流
     * @return 复制的字节数
     * @throws IOException 在IO错误的情况下
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");
        try {
            return StreamUtils.copy(in, out);
        } finally {
            close(in);
            close(out);
        }
    }

    /**
     * 将给定字节数组的内容复制到给定的OutputStream。
     * 完成后关闭流。
     *
     * @param in  要从中复制的字节数组
     * @param out 要复制到的输出流
     * @throws IOException 在IO错误的情况下
     */
    public static void copy(byte[] in, OutputStream out) throws IOException {
        Assert.notNull(in, "No input byte array specified");
        Assert.notNull(out, "No OutputStream specified");
        try {
            out.write(in);
        } finally {
            close(out);
        }
    }

    /**
     * 将给定InputStream的内容复制到新的字节数组中。
     * 完成后关闭流。
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

    //---------------------------------------------------------------------
    // Copy methods for java.io.Reader / java.io.Writer
    //---------------------------------------------------------------------

    /**
     * 将给定读者的内容复制到给定作者。
     * 完成后关闭两者。
     *
     * @param in  要从中复制的读者
     * @param out 要复制到的作者
     * @return 复制的字符数
     * @throws IOException 在IO错误的情况下
     */
    public static int copy(Reader in, Writer out) throws IOException {
        Assert.notNull(in, "No Reader specified");
        Assert.notNull(out, "No Writer specified");
        try {
            int charCount = 0;
            char[] buffer = new char[BUFFER_SIZE];
            int charsRead;
            while ((charsRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, charsRead);
                charCount += charsRead;
            }
            out.flush();
            return charCount;
        } finally {
            close(in);
            close(out);
        }
    }

    /**
     * 将给定字符串的内容复制到给定的编写器。
     * 完成后关闭写入程序。
     *
     * @param in  要从中复制的字符串
     * @param out 要复制到的作者
     * @throws IOException 在IO错误的情况下
     */
    public static void copy(String in, Writer out) throws IOException {
        Assert.notNull(in, "No input String specified");
        Assert.notNull(out, "No Writer specified");
        try {
            out.write(in);
        } finally {
            close(out);
        }
    }

    /**
     * 将给定读取器的内容复制到字符串中。
     * 完成后关闭读卡器。
     *
     * @param in 要从中复制的读取器（可以为null或空）
     * @return 已复制到的字符串（可能为空）
     * @throws IOException 在IO错误的情况下
     */
    public static String copyToString(Reader in) throws IOException {
        if (in == null) {
            return "";
        }
        StringWriter out = new StringWriter(BUFFER_SIZE);
        copy(in, out);
        return out.toString();
    }

    /**
     * 尝试关闭提供的Closeable，默默地接受任何异常。
     *
     * @param closeable 可关闭到关闭
     */
    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            // ignore
        }
    }
}
