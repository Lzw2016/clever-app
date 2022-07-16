package org.clever.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 从底层资源的实际类型（如文件或类路径资源）抽象出来的资源描述符的接口。
 * 如果InputStream以物理形式存在，则可以为每个资源打开它，但可以仅为某些资源返回URL或文件句柄。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 14:13 <br/>
 *
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 */
public interface Resource extends InputStreamSource {
    /**
     * 确定此资源是否实际以物理形式存在。
     * 此方法执行确定的存在性检查，而资源句柄的存在仅保证描述符句柄有效
     */
    boolean exists();

    /**
     * 指示是否可以通过{@link #getInputStream()}读取此资源的非空内容。
     * 它严格地暗示了{@link #exists()}语义。
     * 请注意，实际内容读取在尝试时仍可能失败。
     * 但是，返回false是无法读取资源内容的明确标识
     *
     * @see #getInputStream()
     * @see #exists()
     */
    default boolean isReadable() {
        return exists();
    }

    /**
     * 指示此资源是否表示具有开放流的句柄。
     * 如果为true，则不能多次读取InputStream，必须读取并关闭该InputStream以避免资源泄漏。
     * 对于典型的资源描述符，将为false
     */
    default boolean isOpen() {
        return false;
    }

    /**
     * 确定此资源是否表示文件系统中的文件。
     * 返回值true标识(但不保证)调用{@link #getFile()}将成功
     *
     * @see #getFile()
     */
    default boolean isFile() {
        return false;
    }

    /**
     * 返回此资源的URL句柄
     *
     * @throws IOException 如果资源无法解析为URL
     */
    URL getURL() throws IOException;

    /**
     * 返回此资源的URI句柄
     *
     * @throws IOException 如果资源无法解析为URI
     */
    URI getURI() throws IOException;

    /**
     * 返回此资源的文件句柄
     *
     * @throws java.io.FileNotFoundException 如果无法将资源解析为绝对文件路径
     * @see #getInputStream()
     */
    File getFile() throws IOException;

    /**
     * 返回{@link ReadableByteChannel}<br/>
     * 每个调用都会创建一个新的通道。
     * 默认实现返回{@link Channels#newChannel(InputStream)}
     *
     * @return 基础资源的字节通道 (不能是null)
     * @throws java.io.FileNotFoundException 如果基础资源不存在
     * @throws IOException                   如果无法打开内容频道
     * @see #getInputStream()
     */
    default ReadableByteChannel readableChannel() throws IOException {
        return Channels.newChannel(getInputStream());
    }

    /**
     * 确定此资源的内容长度
     *
     * @throws IOException 如果无法解析资源(在文件系统中或作为其他已知的物理资源类型)
     */
    long contentLength() throws IOException;

    /**
     * 确定此资源的上次修改时间戳
     *
     * @throws IOException 如果无法解析资源(在文件系统中或作为其他已知的物理资源类型)
     */
    long lastModified() throws IOException;

    /**
     * 创建相对于此资源的资源
     *
     * @param relativePath 相对路径(相对于此资源)
     * @return 相对资源的资源句柄
     * @throws IOException 如果无法确定相对资源
     */
    Resource createRelative(String relativePath) throws IOException;

    /**
     * 确定此资源的文件名, 如果此类型的资源没有文件名返回null
     */
    String getFilename();

    /**
     * 返回此资源的描述，用于处理资源时的错误输出，
     * 鼓励实现从其{@code toString}方法返回此值。
     *
     * @see Object#toString()
     */
    String getDescription();
}
