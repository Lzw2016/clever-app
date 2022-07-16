package org.clever.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * 支持写入的资源的扩展接口。提供{@link #getOutputStream()}访问器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:23 <br/>
 *
 * @see java.io.OutputStream
 */
public interface WritableResource extends Resource {
    /**
     * 指示是否可以通过{@link #getOutputStream()}写入此资源的内容。
     * 请注意，实际内容写入在尝试时仍可能失败。
     * 返回值false是无法修改资源内容的明确标识。
     *
     * @see #getOutputStream()
     * @see #isReadable()
     */
    default boolean isWritable() {
        return true;
    }

    /**
     * 返回基础资源的{@link OutputStream}，允许(覆盖)写入其内容
     *
     * @throws IOException 如果流无法打开
     * @see #getInputStream()
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * 返回 {@link WritableByteChannel}<br/>
     * 每个调用都会创建一个新的通道。
     * 默认实现返回{@link Channels#newChannel(OutputStream)}
     *
     * @return 基础资源的字节通道(不能是null)
     * @throws java.io.FileNotFoundException 如果基础资源不存在
     * @throws IOException                   如果无法打开内容通道
     * @see #getOutputStream()
     */
    default WritableByteChannel writableChannel() throws IOException {
        return Channels.newChannel(getOutputStream());
    }
}
