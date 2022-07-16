package org.clever.core.io;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.*;

/**
 * {@link Resource}实现{@code java.io.File}和{@code java.nio.file.Path}系统目标的路径句柄。
 * 支持解析为文件和{@code URL}。实现扩展的{@link WritableResource}接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:36 <br/>
 *
 * @see #FileSystemResource(String)
 * @see #FileSystemResource(File)
 * @see #FileSystemResource(Path)
 * @see java.io.File
 * @see java.nio.file.Files
 */
public class FileSystemResource extends AbstractResource implements WritableResource {
    private final String path;
    private final File file;
    private final Path filePath;

    /**
     * 新建 {@code FileSystemResource} 从文件路径<br/>
     * 注意：当通过{@link #createRelative}构建相对资源时，此处指定的资源基路径是否以斜杠结尾会有所不同。
     *
     * @param path 文件路径
     * @see #FileSystemResource(Path)
     */
    public FileSystemResource(String path) {
        Assert.notNull(path, "Path must not be null");
        this.path = StringUtils.cleanPath(path);
        this.file = new File(path);
        this.filePath = this.file.toPath();
    }

    /**
     * 从{@link File}句柄创建新的{@code FileSystemResource}
     *
     * @param file a File handle
     * @see #FileSystemResource(Path)
     * @see #getFile()
     */
    public FileSystemResource(File file) {
        Assert.notNull(file, "File must not be null");
        this.path = StringUtils.cleanPath(file.getPath());
        this.file = file;
        this.filePath = file.toPath();
    }

    /**
     * 从{@link Path}句柄创建新的{@code FileSystemResource}
     *
     * @param filePath 文件的路径句柄
     * @see #FileSystemResource(File)
     */
    public FileSystemResource(Path filePath) {
        Assert.notNull(filePath, "Path must not be null");
        this.path = StringUtils.cleanPath(filePath.toString());
        this.file = null;
        this.filePath = filePath;
    }

    /**
     * 从{@link FileSystem}句柄创建一个新的{@code FileSystemResource}，定位指定的路径。
     *
     * @param fileSystem 要在其中定位路径的文件系统
     * @param path       文件路径
     * @see #FileSystemResource(File)
     */
    public FileSystemResource(FileSystem fileSystem, String path) {
        Assert.notNull(fileSystem, "FileSystem must not be null");
        Assert.notNull(path, "Path must not be null");
        this.path = StringUtils.cleanPath(path);
        this.file = null;
        this.filePath = fileSystem.getPath(this.path).normalize();
    }

    /**
     * 返回此资源的文件路径
     */
    public final String getPath() {
        return this.path;
    }

    /**
     * 此实现返回基础文件是否存在
     *
     * @see java.io.File#exists()
     */
    @Override
    public boolean exists() {
        return (this.file != null ? this.file.exists() : Files.exists(this.filePath));
    }

    /**
     * 此实现检查基础文件是否标记为可读(并对应于包含内容的实际文件，而不是目录)
     *
     * @see java.io.File#canRead()
     * @see java.io.File#isDirectory()
     */
    @Override
    public boolean isReadable() {
        return this.file != null ?
                this.file.canRead() && !this.file.isDirectory() :
                Files.isReadable(this.filePath) && !Files.isDirectory(this.filePath);
    }

    /**
     * 此实现为基础文件打开NIO文件流
     *
     * @see java.io.FileInputStream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return Files.newInputStream(this.filePath);
        } catch (NoSuchFileException ex) {
            throw new FileNotFoundException(ex.getMessage());
        }
    }

    /**
     * 此实现检查基础文件是否标记为可写(并对应于包含内容的实际文件，而不是目录)
     *
     * @see java.io.File#canWrite()
     * @see java.io.File#isDirectory()
     */
    @Override
    public boolean isWritable() {
        return (this.file != null ? this.file.canWrite() && !this.file.isDirectory() :
                Files.isWritable(this.filePath) && !Files.isDirectory(this.filePath));
    }

    /**
     * 此实现为基础文件打开一个FileOutputStream
     *
     * @see java.io.FileOutputStream
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(this.filePath);
    }

    /**
     * 此实现返回基础文件的URL
     *
     * @see java.io.File#toURI()
     */
    @Override
    public URL getURL() throws IOException {
        return (this.file != null ? this.file.toURI().toURL() : this.filePath.toUri().toURL());
    }

    /**
     * 此实现返回基础文件的URI
     *
     * @see java.io.File#toURI()
     */
    @Override
    public URI getURI() {
        return (this.file != null ? this.file.toURI() : this.filePath.toUri());
    }

    /**
     * 此实现始终指示一个文件
     */
    @Override
    public boolean isFile() {
        return true;
    }

    /**
     * 此实现返回基础文件引用
     */
    @Override
    public File getFile() {
        return (this.file != null ? this.file : this.filePath.toFile());
    }

    /**
     * 此实现为基础文件打开一个FileChannel
     *
     * @see java.nio.channels.FileChannel
     */
    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        try {
            return FileChannel.open(this.filePath, StandardOpenOption.READ);
        } catch (NoSuchFileException ex) {
            throw new FileNotFoundException(ex.getMessage());
        }
    }

    /**
     * 此实现为基础文件打开一个FileChannel
     *
     * @see java.nio.channels.FileChannel
     */
    @Override
    public WritableByteChannel writableChannel() throws IOException {
        return FileChannel.open(this.filePath, StandardOpenOption.WRITE);
    }

    /**
     * 此实现返回基础File/Path长度
     */
    @Override
    public long contentLength() throws IOException {
        if (this.file != null) {
            long length = this.file.length();
            if (length == 0L && !this.file.exists()) {
                throw new FileNotFoundException(
                        getDescription() + " cannot be resolved in the file system for checking its content length"
                );
            }
            return length;
        } else {
            try {
                return Files.size(this.filePath);
            } catch (NoSuchFileException ex) {
                throw new FileNotFoundException(ex.getMessage());
            }
        }
    }

    /**
     * 此实现返回上次修改的基础File/Path时间
     */
    @Override
    public long lastModified() throws IOException {
        if (this.file != null) {
            return super.lastModified();
        } else {
            try {
                return Files.getLastModifiedTime(this.filePath).toMillis();
            } catch (NoSuchFileException ex) {
                throw new FileNotFoundException(ex.getMessage());
            }
        }
    }

    /**
     * 此实现创建一个FileSystemResource，应用相对于此资源描述符的基础文件路径的给定路径
     *
     * @see org.clever.util.StringUtils#applyRelativePath(String, String)
     */
    @Override
    public Resource createRelative(String relativePath) {
        String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
        return this.file != null ?
                new FileSystemResource(pathToUse) :
                new FileSystemResource(this.filePath.getFileSystem(), pathToUse);
    }

    /**
     * 此实现返回文件名
     *
     * @see java.io.File#getName()
     */
    @Override
    public String getFilename() {
        return (this.file != null ? this.file.getName() : this.filePath.getFileName().toString());
    }

    /**
     * 此实现返回包含文件绝对路径的描述
     *
     * @see java.io.File#getAbsolutePath()
     */
    @Override
    public String getDescription() {
        return "file [" + (this.file != null ? this.file.getAbsolutePath() : this.filePath.toAbsolutePath()) + "]";
    }

    /**
     * 此实现比较底层文件引用
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof FileSystemResource && this.path.equals(((FileSystemResource) other).path)));
    }

    /**
     * 此实现返回基础文件引用的hashCode
     */
    @Override
    public int hashCode() {
        return this.path.hashCode();
    }
}
