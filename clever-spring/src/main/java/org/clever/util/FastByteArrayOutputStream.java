package org.clever.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * {@link java.io.ByteArrayOutputStream} 的快速替代方法。
 * 请注意，此变体<i>不</i>扩展{@code ByteArrayOutputStream}，不像它的兄弟{@link ResizableByteArrayOutputStream}。
 *
 * <p>与 {@link java.io.ByteArrayOutputStream} 不同，
 * 此实现由 {@code byte[]} 的 {@link java.util.ArrayDeque} 支持，而不是 1 个不断调整大小的 {@code byte[]}。
 * 它在扩展时不会复制缓冲区。
 *
 * <p>初始缓冲区仅在首次写入流时创建。
 * 如果使用 {@link #writeTo(OutputStream)} 方法提取其内容，则也不会复制内部缓冲区。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/23 19:35 <br/>
 *
 * @see ResizableByteArrayOutputStream
 */
public class FastByteArrayOutputStream extends OutputStream {
    private static final int DEFAULT_BLOCK_SIZE = 256;
    // 用于存储内容字节的缓冲区
    private final Deque<byte[]> buffers = new ArrayDeque<>();
    // 分配第一个 byte[] 时使用的大小（以字节为单位）
    private final int initialBlockSize;
    // 分配下一个 byte[] 时使用的大小（以字节为单位）
    private int nextBlockSize = 0;
    // 先前缓冲区中的字节数。 （当前缓冲区中的字节数在“索引”中。）
    private int alreadyBufferedSize = 0;
    // 在 buffers.getLast() 中找到的 byte[] 中的索引接下来要写入
    private int index = 0;
    // 流关闭了吗？
    private boolean closed = false;

    /**
     * 创建一个新的 <code>FastByteArrayOutputStream</code>
     * 默认初始容量为 256 字节
     */
    public FastByteArrayOutputStream() {
        this(DEFAULT_BLOCK_SIZE);
    }

    /**
     * 创建一个新的 <code>FastByteArrayOutputStream</code>
     * 具有指定的初始容量
     *
     * @param initialBlockSize the initial buffer size in bytes
     */
    public FastByteArrayOutputStream(int initialBlockSize) {
        Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
        this.initialBlockSize = initialBlockSize;
        this.nextBlockSize = initialBlockSize;
    }

    // Overridden methods

    @Override
    public void write(int datum) throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        } else {
            if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
                addBuffer(1);
            }
            // store the byte
            this.buffers.getLast()[this.index++] = (byte) datum;
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (offset < 0 || offset + length > data.length || length < 0) {
            throw new IndexOutOfBoundsException();
        } else if (this.closed) {
            throw new IOException("Stream closed");
        } else {
            if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
                addBuffer(length);
            }
            if (this.index + length > this.buffers.getLast().length) {
                int pos = offset;
                do {
                    if (this.index == this.buffers.getLast().length) {
                        addBuffer(length);
                    }
                    int copyLength = this.buffers.getLast().length - this.index;
                    if (length < copyLength) {
                        copyLength = length;
                    }
                    System.arraycopy(data, pos, this.buffers.getLast(), this.index, copyLength);
                    pos += copyLength;
                    this.index += copyLength;
                    length -= copyLength;
                }
                while (length > 0);
            } else {
                // 复制到子数组
                System.arraycopy(data, offset, this.buffers.getLast(), this.index, length);
                this.index += length;
            }
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    /**
     * 使用平台的默认字符集将缓冲区的内容转换为字符串解码字节。新的 {@code String} 的长度是字符集的函数，因此可能不等于缓冲区的大小。
     * <p>此方法始终将格式错误的输入和不可映射的字符序列替换为平台默认字符集的默认替换字符串。
     * 当需要对解码过程进行更多控制时，应使用 {@linkplain java.nio.charset.CharsetDecoder} 类。
     *
     * @return 从缓冲区内容解码的字符串
     */
    @Override
    public String toString() {
        return new String(toByteArrayUnsafe());
    }

    // Custom methods

    /**
     * 返回存储在此 <code>FastByteArrayOutputStream</code> 中的字节数。
     */
    public int size() {
        return (this.alreadyBufferedSize + this.index);
    }

    /**
     * 将流的数据转换为字节数组并返回字节数组。
     * <p>还用字节数组替换内部结构以节省内存：如果字节数组无论如何都被创建，我们不妨使用它。
     * 这种方法还意味着，如果在中间没有任何写入的情况下调用此方法两次，则第二次调用是空操作。
     * <p>此方法是“不安全的”，因为它返回内部缓冲区。
     * 调用者不应修改返回的缓冲区。
     *
     * @return 此输出流的当前内容，作为字节数组
     * @see #size()
     * @see #toByteArray()
     */
    public byte[] toByteArrayUnsafe() {
        int totalSize = size();
        if (totalSize == 0) {
            return new byte[0];
        }
        resize(totalSize);
        return this.buffers.getFirst();
    }

    /**
     * 创建一个新分配的字节数组。
     * <p>它的大小是这个输出流的当前大小，它将包含内部缓冲区的有效内容。
     *
     * @return 此输出流的当前内容，作为字节数组
     * @see #size()
     * @see #toByteArrayUnsafe()
     */
    public byte[] toByteArray() {
        byte[] bytesUnsafe = toByteArrayUnsafe();
        return bytesUnsafe.clone();
    }

    /**
     * 重置此 <code>FastByteArrayOutputStream</code> 的内容
     * <p>输出流中当前累积的所有输出都将被丢弃。
     * 输出流可以再次使用。
     */
    public void reset() {
        this.buffers.clear();
        this.nextBlockSize = this.initialBlockSize;
        this.closed = false;
        this.index = 0;
        this.alreadyBufferedSize = 0;
    }

    /**
     * Get an {@link InputStream} to retrieve the data in this OutputStream.
     * <p>请注意，如果在 OutputStream 上调用了任何方法(包括但不限于任何写入方法，{@link #reset()}、{@link #toByteArray()} 和 {@link #toByteArrayUnsafe()})，
     * 则 {@link java.io.InputStream} 的行为是未定义的。
     *
     * @return 此输出流内容的 {@link InputStream}
     */
    public InputStream getInputStream() {
        return new FastByteArrayInputStream(this);
    }

    /**
     * 将缓冲区内容写入给定的输出流
     *
     * @param out 要写入的输出流
     */
    public void writeTo(OutputStream out) throws IOException {
        Iterator<byte[]> it = this.buffers.iterator();
        while (it.hasNext()) {
            byte[] bytes = it.next();
            if (it.hasNext()) {
                out.write(bytes, 0, bytes.length);
            } else {
                out.write(bytes, 0, this.index);
            }
        }
    }

    /**
     * 将内部缓冲区大小调整为指定容量
     *
     * @param targetCapacity 所需的缓冲区大小
     * @throws IllegalArgumentException 如果给定的容量小于缓冲区中存储的内容的实际大小
     * @see FastByteArrayOutputStream#size()
     */
    public void resize(int targetCapacity) {
        Assert.isTrue(targetCapacity >= size(), "New capacity must not be smaller than current size");
        if (this.buffers.peekFirst() == null) {
            this.nextBlockSize = targetCapacity - size();
        } else if (size() == targetCapacity && this.buffers.getFirst().length == targetCapacity) {
            // do nothing - already at the targetCapacity
        } else {
            int totalSize = size();
            byte[] data = new byte[targetCapacity];
            int pos = 0;
            Iterator<byte[]> it = this.buffers.iterator();
            while (it.hasNext()) {
                byte[] bytes = it.next();
                if (it.hasNext()) {
                    System.arraycopy(bytes, 0, data, pos, bytes.length);
                    pos += bytes.length;
                } else {
                    System.arraycopy(bytes, 0, data, pos, this.index);
                }
            }
            this.buffers.clear();
            this.buffers.add(data);
            this.index = totalSize;
            this.alreadyBufferedSize = 0;
        }
    }

    /**
     * 创建一个新的缓冲区并将其存储在 ArrayDeque 中
     * <p>添加至少可以存储 {@code minCapacity} 字节的新缓冲区
     */
    private void addBuffer(int minCapacity) {
        if (this.buffers.peekLast() != null) {
            this.alreadyBufferedSize += this.index;
            this.index = 0;
        }
        if (this.nextBlockSize < minCapacity) {
            this.nextBlockSize = nextPowerOf2(minCapacity);
        }
        this.buffers.add(new byte[this.nextBlockSize]);
        // block size doubles each time
        this.nextBlockSize *= 2;
    }

    /**
     * 获取一个数字的 2 的下一个幂（例如，119 的 2 的下一个幂是 128）
     */
    private static int nextPowerOf2(int val) {
        val--;
        val = (val >> 1) | val;
        val = (val >> 2) | val;
        val = (val >> 4) | val;
        val = (val >> 8) | val;
        val = (val >> 16) | val;
        val++;
        return val;
    }

    /**
     * 从给定读取的 {@link java.io.InputStream} 的实现 <code>FastByteArrayOutputStream</code>.
     */
    private static final class FastByteArrayInputStream extends UpdateMessageDigestInputStream {
        private final FastByteArrayOutputStream fastByteArrayOutputStream;
        private final Iterator<byte[]> buffersIterator;
        private byte[] currentBuffer;
        private int currentBufferLength = 0;
        private int nextIndexInCurrentBuffer = 0;
        private int totalBytesRead = 0;

        /**
         * 创建一个新的 <code>FastByteArrayOutputStreamInputStream</code> 由给定的 <code>FastByteArrayOutputStream</code> 支持
         */
        public FastByteArrayInputStream(FastByteArrayOutputStream fastByteArrayOutputStream) {
            this.fastByteArrayOutputStream = fastByteArrayOutputStream;
            this.buffersIterator = fastByteArrayOutputStream.buffers.iterator();
            if (this.buffersIterator.hasNext()) {
                this.currentBuffer = this.buffersIterator.next();
                if (this.currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
                    this.currentBufferLength = fastByteArrayOutputStream.index;
                } else {
                    this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
                }
            }
        }

        @Override
        public int read() {
            if (this.currentBuffer == null) {
                // This stream doesn't have any data in it...
                return -1;
            } else {
                if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                    this.totalBytesRead++;
                    return this.currentBuffer[this.nextIndexInCurrentBuffer++] & 0xFF;
                } else {
                    if (this.buffersIterator.hasNext()) {
                        this.currentBuffer = this.buffersIterator.next();
                        updateCurrentBufferLength();
                        this.nextIndexInCurrentBuffer = 0;
                    } else {
                        this.currentBuffer = null;
                    }
                    return read();
                }
            }
        }

        @Override
        public int read(byte[] b) {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            } else {
                if (this.currentBuffer == null) {
                    // This stream doesn't have any data in it...
                    return -1;
                } else {
                    if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                        int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
                        System.arraycopy(this.currentBuffer, this.nextIndexInCurrentBuffer, b, off, bytesToCopy);
                        this.totalBytesRead += bytesToCopy;
                        this.nextIndexInCurrentBuffer += bytesToCopy;
                        int remaining = read(b, off + bytesToCopy, len - bytesToCopy);
                        return bytesToCopy + Math.max(remaining, 0);
                    } else {
                        if (this.buffersIterator.hasNext()) {
                            this.currentBuffer = this.buffersIterator.next();
                            updateCurrentBufferLength();
                            this.nextIndexInCurrentBuffer = 0;
                        } else {
                            this.currentBuffer = null;
                        }
                        return read(b, off, len);
                    }
                }
            }
        }

        @Override
        public long skip(long n) throws IOException {
            if (n > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("n exceeds maximum (" + Integer.MAX_VALUE + "): " + n);
            } else if (n == 0) {
                return 0;
            } else if (n < 0) {
                throw new IllegalArgumentException("n must be 0 or greater: " + n);
            }
            int len = (int) n;
            if (this.currentBuffer == null) {
                // This stream doesn't have any data in it...
                return 0;
            } else {
                if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                    int bytesToSkip = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
                    this.totalBytesRead += bytesToSkip;
                    this.nextIndexInCurrentBuffer += bytesToSkip;
                    return (bytesToSkip + skip(len - bytesToSkip));
                } else {
                    if (this.buffersIterator.hasNext()) {
                        this.currentBuffer = this.buffersIterator.next();
                        updateCurrentBufferLength();
                        this.nextIndexInCurrentBuffer = 0;
                    } else {
                        this.currentBuffer = null;
                    }
                    return skip(len);
                }
            }
        }

        @Override
        public int available() {
            return (this.fastByteArrayOutputStream.size() - this.totalBytesRead);
        }

        /**
         * 使用此流中的剩余字节更新消息摘要
         *
         * @param messageDigest 要更新的消息摘要
         */
        @Override
        public void updateMessageDigest(MessageDigest messageDigest) {
            updateMessageDigest(messageDigest, available());
        }

        /**
         * 使用此流中的下一个 len 字节更新消息摘要。
         * 避免创建新的字节数组并使用内部缓冲区来提高性能。
         *
         * @param messageDigest 要更新的消息摘要
         * @param len           从此流中读取并用于更新消息摘要的字节数
         */
        @Override
        public void updateMessageDigest(MessageDigest messageDigest, int len) {
            if (this.currentBuffer == null) {
                // This stream doesn't have any data in it...
                return;
            } else if (len == 0) {
                return;
            } else if (len < 0) {
                throw new IllegalArgumentException("len must be 0 or greater: " + len);
            } else {
                if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                    int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
                    messageDigest.update(this.currentBuffer, this.nextIndexInCurrentBuffer, bytesToCopy);
                    this.nextIndexInCurrentBuffer += bytesToCopy;
                    updateMessageDigest(messageDigest, len - bytesToCopy);
                } else {
                    if (this.buffersIterator.hasNext()) {
                        this.currentBuffer = this.buffersIterator.next();
                        updateCurrentBufferLength();
                        this.nextIndexInCurrentBuffer = 0;
                    } else {
                        this.currentBuffer = null;
                    }
                    updateMessageDigest(messageDigest, len);
                }
            }
        }

        private void updateCurrentBufferLength() {
            if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
                this.currentBufferLength = this.fastByteArrayOutputStream.index;
            } else {
                this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
            }
        }
    }
}
