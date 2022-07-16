package org.clever.core.io;

import org.clever.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 给定字节数组的资源实现。
 * 为给定字节数组创建{@link ByteArrayInputStream}。
 * 用于从任何给定字节数组加载内容，而不必求助于单一用途的{@link InputStreamResource}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/11 10:23 <br/>
 *
 * @see java.io.ByteArrayInputStream
 * @see InputStreamResource
 */
public class ByteArrayResource extends AbstractResource {
    private final byte[] byteArray;
    private final String description;

    /**
     * 新建{@code ByteArrayResource}
     *
     * @param byteArray 要换行的字节数组
     */
    public ByteArrayResource(byte[] byteArray) {
        this(byteArray, "resource loaded from byte array");
    }

    /**
     * 新建{@code ByteArrayResource}
     *
     * @param byteArray   要换行的字节数组
     * @param description 字节数组的来源
     */
    public ByteArrayResource(byte[] byteArray, String description) {
        Assert.notNull(byteArray, "Byte array must not be null");
        this.byteArray = byteArray;
        this.description = (description != null ? description : "");
    }

    /**
     * 返回基础字节数组
     */
    public final byte[] getByteArray() {
        return this.byteArray;
    }

    /**
     * 此实现始终返回 {@code true}.
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * 此实现返回基础字节数组的长度
     */
    @Override
    public long contentLength() {
        return this.byteArray.length;
    }

    /**
     * 此实现为基础字节数组返回ByteArrayInputStream
     *
     * @see java.io.ByteArrayInputStream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.byteArray);
    }

    /**
     * 此实现返回包含传入描述（如果有）的描述。
     */
    @Override
    public String getDescription() {
        return "Byte array resource [" + this.description + "]";
    }

    /**
     * 此实现比较底层字节数组
     *
     * @see java.util.Arrays#equals(byte[], byte[])
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof ByteArrayResource && Arrays.equals(((ByteArrayResource) other).byteArray, this.byteArray)));
    }

    /**
     * 此实现基于底层字节数组返回哈希代码
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.byteArray);
    }
}
