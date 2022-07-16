package org.clever.core.io;

import org.clever.core.NestedIOException;
import org.clever.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 方便资源实现的基类，预实现典型行为。
 * “exists”方法将检查是否可以打开文件或InputStream；<br/>
 * 1. “isOpen”将始终返回false；<br/>
 * 2. “getURL”和“getFile”引发异常；<br/>
 * 3. “toString”将返回描述 <br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:25 <br/>
 */
public abstract class AbstractResource implements Resource {
    /**
     * 此实现检查文件是否可以打开，返回到是否可以打开InputStream。
     * 这将涵盖目录和内容资源
     */
    @Override
    public boolean exists() {
        // Try file existence: can we find the file in the file system?
        if (isFile()) {
            try {
                return getFile().exists();
            } catch (IOException ex) {
                Logger logger = LoggerFactory.getLogger(getClass());
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not retrieve File for existence check of " + getDescription(), ex);
                }
            }
        }
        // Fall back to stream existence: can we open the stream?
        try {
            getInputStream().close();
            return true;
        } catch (Throwable ex) {
            Logger logger = LoggerFactory.getLogger(getClass());
            if (logger.isDebugEnabled()) {
                logger.debug("Could not retrieve InputStream for existence check of " + getDescription(), ex);
            }
            return false;
        }
    }

    /**
     * 对于存在的资源，此实现始终返回true
     */
    @Override
    public boolean isReadable() {
        return exists();
    }

    /**
     * 此实现始终返回 {@code false}.
     */
    @Override
    public boolean isOpen() {
        return false;
    }

    /**
     * 此实现始终返回 {@code false}.
     */
    @Override
    public boolean isFile() {
        return false;
    }

    /**
     * 此实现引发FileNotFoundException，假设资源无法解析为URL
     */
    @Override
    public URL getURL() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
    }

    /**
     * 此实现基于返回的URL构建URI {@link #getURL()}
     */
    @Override
    public URI getURI() throws IOException {
        URL url = getURL();
        try {
            return ResourceUtils.toURI(url);
        } catch (URISyntaxException ex) {
            throw new NestedIOException("Invalid URI [" + url + "]", ex);
        }
    }

    /**
     * 此实现引发FileNotFoundException，假设资源无法解析为绝对文件路径
     */
    @Override
    public File getFile() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
    }

    /**
     * 此实现返回 {@link Channels#newChannel(InputStream)}<br/>
     */
    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        return Channels.newChannel(getInputStream());
    }

    /**
     * 此方法读取整个InputStream以确定内容长度。
     * 对于{@code InputStreamResource}的自定义子类，
     * 我们强烈建议使用更优化的实现覆盖此方法，
     * 例如检查文件长度，或者如果流只能读取一次，则可能只返回-1
     *
     * @see #getInputStream()
     */
    @Override
    public long contentLength() throws IOException {
        InputStream is = getInputStream();
        try {
            long size = 0;
            byte[] buf = new byte[256];
            int read;
            while ((read = is.read(buf)) != -1) {
                size += read;
            }
            return size;
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                Logger logger = LoggerFactory.getLogger(getClass());
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not close content-length InputStream for " + getDescription(), ex);
                }
            }
        }
    }

    /**
     * 此实现检查基础文件的时间戳（如果可用）
     *
     * @see #getFileForLastModifiedCheck()
     */
    @Override
    public long lastModified() throws IOException {
        File fileToCheck = getFileForLastModifiedCheck();
        long lastModified = fileToCheck.lastModified();
        if (lastModified == 0L && !fileToCheck.exists()) {
            throw new FileNotFoundException(
                    getDescription() + " cannot be resolved in the file system for checking its last-modified timestamp"
            );
        }
        return lastModified;
    }

    /**
     * 确定用于时间戳检查的文件
     * <p>默认实现委托给 {@link #getFile()}
     *
     * @return 用于时间戳检查的文件(从不为null)
     * @throws FileNotFoundException 如果资源无法解析为绝对文件路径
     * @throws IOException           在一般解决方案读取失败的情况下
     */
    protected File getFileForLastModifiedCheck() throws IOException {
        return getFile();
    }

    /**
     * 此实现引发FileNotFoundException，假设无法为此资源创建相对资源
     */
    @Override
    public Resource createRelative(String relativePath) throws IOException {
        throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
    }

    /**
     * 此实现始终返回{@code null},假设此资源类型没有文件名
     */
    @Override
    public String getFilename() {
        return null;
    }

    /**
     * 此实现比较描述字符串
     *
     * @see #getDescription()
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof Resource && ((Resource) other).getDescription().equals(getDescription())));
    }

    /**
     * 此实现返回描述的hashCode
     *
     * @see #getDescription()
     */
    @Override
    public int hashCode() {
        return getDescription().hashCode();
    }

    /**
     * 此实现返回此资源的描述
     *
     * @see #getDescription()
     */
    @Override
    public String toString() {
        return getDescription();
    }
}
