package org.clever.core.io;

import org.clever.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

/**
 * 用于将URL解析为文件引用的资源的抽象基类，例如{@link UrlResource}或{@link ClassPathResource}。
 * 检测URL中的“file”协议以及JBoss“vfs”协议，相应地解析文件系统引用
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:47 <br/>
 */
public abstract class AbstractFileResolvingResource extends AbstractResource {
    @Override
    public boolean exists() {
        try {
            URL url = getURL();
            if (ResourceUtils.isFileURL(url)) {
                // Proceed with file system resolution
                return getFile().exists();
            } else {
                // Try a URL connection content-length header
                URLConnection con = url.openConnection();
                customizeConnection(con);
                HttpURLConnection httpCon = (con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
                if (httpCon != null) {
                    int code = httpCon.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        return true;
                    } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                        return false;
                    }
                }
                if (con.getContentLengthLong() > 0) {
                    return true;
                }
                if (httpCon != null) {
                    // No HTTP OK status, and no content-length header: give up
                    httpCon.disconnect();
                    return false;
                } else {
                    // Fall back to stream existence: can we open the stream?
                    getInputStream().close();
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean isReadable() {
        try {
            return checkReadable(getURL());
        } catch (IOException ex) {
            return false;
        }
    }

    boolean checkReadable(URL url) {
        try {
            if (ResourceUtils.isFileURL(url)) {
                // Proceed with file system resolution
                File file = getFile();
                return (file.canRead() && !file.isDirectory());
            } else {
                // Try InputStream resolution for jar resources
                URLConnection con = url.openConnection();
                customizeConnection(con);
                if (con instanceof HttpURLConnection) {
                    HttpURLConnection httpCon = (HttpURLConnection) con;
                    int code = httpCon.getResponseCode();
                    if (code != HttpURLConnection.HTTP_OK) {
                        httpCon.disconnect();
                        return false;
                    }
                }
                long contentLength = con.getContentLengthLong();
                if (contentLength > 0) {
                    return true;
                } else if (contentLength == 0) {
                    // Empty file or directory -> not considered readable...
                    return false;
                } else {
                    // Fall back to stream existence: can we open the stream?
                    getInputStream().close();
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean isFile() {
        try {
            URL url = getURL();
            if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                return VfsResourceDelegate.getResource(url).isFile();
            }
            return ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol());
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 此实现返回基础类路径资源的文件引用，前提是它引用文件系统中的文件
     *
     * @see org.clever.util.ResourceUtils#getFile(java.net.URL, String)
     */
    @Override
    public File getFile() throws IOException {
        URL url = getURL();
        if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
            return VfsResourceDelegate.getResource(url).getFile();
        }
        return ResourceUtils.getFile(url, getDescription());
    }

    /**
     * 此实现确定基础文件(或jar文件，如果是jar/zip).
     */
    @Override
    protected File getFileForLastModifiedCheck() throws IOException {
        URL url = getURL();
        if (ResourceUtils.isJarURL(url)) {
            URL actualUrl = ResourceUtils.extractArchiveURL(url);
            if (actualUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                return VfsResourceDelegate.getResource(actualUrl).getFile();
            }
            return ResourceUtils.getFile(actualUrl, "Jar URL");
        } else {
            return getFile();
        }
    }

    /**
     * 此实现返回给定URI标识资源的文件引用，前提是它引用文件系统中的文件
     *
     * @see #getFile(URI)
     */
    protected boolean isFile(URI uri) {
        try {
            if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                return VfsResourceDelegate.getResource(uri).isFile();
            }
            return ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme());
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 此实现返回给定URI标识资源的文件引用，前提是它引用文件系统中的文件
     *
     * @see org.clever.util.ResourceUtils#getFile(java.net.URI, String)
     */
    protected File getFile(URI uri) throws IOException {
        if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
            return VfsResourceDelegate.getResource(uri).getFile();
        }
        return ResourceUtils.getFile(uri, getDescription());
    }

    /**
     * 此实现返回给定URI标识资源的FileChannel，前提是它引用文件系统中的文件
     *
     * @see #getFile()
     */
    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        try {
            // Try file system channel
            return FileChannel.open(getFile().toPath(), StandardOpenOption.READ);
        } catch (FileNotFoundException | NoSuchFileException ex) {
            // Fall back to InputStream adaptation in superclass
            return super.readableChannel();
        }
    }

    @Override
    public long contentLength() throws IOException {
        URL url = getURL();
        if (ResourceUtils.isFileURL(url)) {
            // Proceed with file system resolution
            File file = getFile();
            long length = file.length();
            if (length == 0L && !file.exists()) {
                throw new FileNotFoundException(
                        getDescription() + " cannot be resolved in the file system for checking its content length"
                );
            }
            return length;
        } else {
            // Try a URL connection content-length header
            URLConnection con = url.openConnection();
            customizeConnection(con);
            return con.getContentLengthLong();
        }
    }

    @Override
    public long lastModified() throws IOException {
        URL url = getURL();
        boolean fileCheck = false;
        if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url)) {
            // Proceed with file system resolution
            fileCheck = true;
            try {
                File fileToCheck = getFileForLastModifiedCheck();
                long lastModified = fileToCheck.lastModified();
                if (lastModified > 0L || fileToCheck.exists()) {
                    return lastModified;
                }
            } catch (FileNotFoundException ex) {
                // Defensively fall back to URL connection check instead
            }
        }
        // Try a URL connection last-modified header
        URLConnection con = url.openConnection();
        customizeConnection(con);
        long lastModified = con.getLastModified();
        if (fileCheck && lastModified == 0 && con.getContentLengthLong() <= 0) {
            throw new FileNotFoundException(
                    getDescription() + " cannot be resolved in the file system for checking its last-modified timestamp"
            );
        }
        return lastModified;
    }

    /**
     * 自定义给定的{@link URLConnection}，它是在{@link #exists()}、{@link #contentLength()}或{@link #lastModified()}调用过程中获得的。
     * 调用{@link ResourceUtils#useCachesIfNecessary(URLConnection)}，请使用{@link #customizeConnection(HttpURLConnection)}并委托定制连接。
     * 可以在子类中重写
     *
     * @param con 要自定义的URLConnection
     * @throws IOException 如果从URLConnection方法引发
     */
    protected void customizeConnection(URLConnection con) throws IOException {
        ResourceUtils.useCachesIfNecessary(con);
        if (con instanceof HttpURLConnection) {
            customizeConnection((HttpURLConnection) con);
        }
    }

    /**
     * 自定义给定的{@link HttpURLConnection}，该连接是在{@link #exists()}、{@link #contentLength()}或{@link #lastModified()}调用过程中获得的。
     * 默认情况下，设置请求方法“HEAD”。
     * 可以在子类中重写
     *
     * @param con 要自定义的HttpURLConnection
     * @throws IOException 如果从HttpURLConnection方法引发
     */
    protected void customizeConnection(HttpURLConnection con) throws IOException {
        con.setRequestMethod("HEAD");
    }

    /**
     * 内部委托类，避免运行时出现硬JBoss VFS API依赖
     */
    private static class VfsResourceDelegate {
        public static Resource getResource(URL url) throws IOException {
            return new VfsResource(VfsUtils.getRoot(url));
        }

        public static Resource getResource(URI uri) throws IOException {
            return new VfsResource(VfsUtils.getRoot(uri));
        }
    }
}
