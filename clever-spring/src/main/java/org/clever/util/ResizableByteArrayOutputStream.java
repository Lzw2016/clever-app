package org.clever.util;

import java.io.ByteArrayOutputStream;

/**
 * {@link java.io.ByteArrayOutputStream} 的扩展：
 * <ul>
 * <li>有 {@link ResizableByteArrayOutputStream#grow(int)} 和 {@link ResizableByteArrayOutputStream#resize(int)} 方法来更好地控制内部缓冲区的大小<li>
 * <li>默认情况下具有更高的初始容量(256)</li>
 * </ul>
 *
 * <p>此类已被 {@link FastByteArrayOutputStream} 取代，用于 Spring 的内部使用，
 * 其中不需要对 {@link ByteArrayOutputStream} 的可分配性（因为 {@link FastByteArrayOutputStream} 在缓冲区调整大小管理方面更有效，
 * 但不会扩展标准 {@link ByteArrayOutputStream}）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/23 19:38 <br/>
 *
 * @see FastByteArrayOutputStream
 */
public class ResizableByteArrayOutputStream extends ByteArrayOutputStream {
    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    /**
     * 创建一个新的 <code>ResizableByteArrayOutputStream</code>
     * 默认初始容量为 256 字节
     */
    public ResizableByteArrayOutputStream() {
        super(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * 创建一个新的 <code>ResizableByteArrayOutputStream</code>
     * 具有指定的初始容量。
     *
     * @param initialCapacity 以字节为单位的初始缓冲区大小
     */
    public ResizableByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * 将内部缓冲区大小调整为指定容量
     *
     * @param targetCapacity 所需的缓冲区大小
     * @throws IllegalArgumentException 如果给定的容量小于缓冲区中已存储内容的实际大小
     * @see ResizableByteArrayOutputStream#size()
     */
    public synchronized void resize(int targetCapacity) {
        Assert.isTrue(targetCapacity >= this.count, "New capacity must not be smaller than current size");
        byte[] resizedBuffer = new byte[targetCapacity];
        System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
        this.buf = resizedBuffer;
    }

    /**
     * 增加内部缓冲区大小
     *
     * @param additionalCapacity 添加到当前缓冲区大小的字节数
     * @see ResizableByteArrayOutputStream#size()
     */
    public synchronized void grow(int additionalCapacity) {
        Assert.isTrue(additionalCapacity >= 0, "Additional capacity must be 0 or higher");
        if (this.count + additionalCapacity > this.buf.length) {
            int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
            resize(newCapacity);
        }
    }

    /**
     * 返回此流的内部缓冲区的当前大小
     */
    public synchronized int capacity() {
        return this.buf.length;
    }
}
