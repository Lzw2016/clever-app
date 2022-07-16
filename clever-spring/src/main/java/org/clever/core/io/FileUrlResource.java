package org.clever.core.io;

import org.clever.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * {@link UrlResource}的子类，假定文件解析，达到为其实现{@link WritableResource}接口的程度。
 * 此资源变量还缓存来自{@link #getFile()}的已解析文件句柄。
 * 这是{@link DefaultResourceLoader}为“file:...”解析的类URL位置，允许向下转换为其可写资源。
 * 或者，用于从文件句柄或NIO {@link java.nio.file.Path}。文件路径，请考虑使用{@link FileSystemResource}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:00 <br/>
 */
public class FileUrlResource extends UrlResource implements WritableResource {
    private volatile File file;

    /**
     * 基于给定的URL对象创建新的{@code FileUrlResource}。
     * 请注意，这并不强制将“file”作为URL协议。如果已知协议可解析为文件，则可用于此目的。
     *
     * @param url URL
     * @see ResourceUtils#isFileURL(URL)
     * @see #getFile()
     */
    public FileUrlResource(URL url) {
        super(url);
    }

    /**
     * 使用URL协议“file”，基于给定的文件位置创建新的{@code FileUrlResource}。
     * 如有必要，将自动对给定部分进行编码
     *
     * @param location 位置（即该协议中的文件路径）
     * @throws MalformedURLException 如果给定的URL规范无效
     * @see UrlResource#UrlResource(String, String)
     * @see ResourceUtils#URL_PROTOCOL_FILE
     */
    public FileUrlResource(String location) throws MalformedURLException {
        super(ResourceUtils.URL_PROTOCOL_FILE, location);
    }

    @Override
    public File getFile() throws IOException {
        File file = this.file;
        if (file != null) {
            return file;
        }
        file = super.getFile();
        this.file = file;
        return file;
    }

    @Override
    public boolean isWritable() {
        try {
            File file = getFile();
            return (file.canWrite() && !file.isDirectory());
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(getFile().toPath());
    }

    @Override
    public WritableByteChannel writableChannel() throws IOException {
        return FileChannel.open(getFile().toPath(), StandardOpenOption.WRITE);
    }

    @Override
    public Resource createRelative(String relativePath) throws MalformedURLException {
        return new FileUrlResource(createRelativeURL(relativePath));
    }
}
