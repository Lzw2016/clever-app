package org.clever.core.io;

import org.clever.util.Assert;
import org.clever.util.ResourceUtils;
import org.clever.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * {@code java.net.URL} 资源实现，支持解析为URL，在使用{@code "file:"}协议的情况下也支持解析为文件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:59 <br/>
 *
 * @see java.net.URL
 */
public class UrlResource extends AbstractFileResolvingResource {
    /**
     * 原始URI（如果可用）；用于URI和文件访问
     */
    private final URI uri;
    /**
     * 原始URL，用于实际访问
     */
    private final URL url;
    /**
     * 清理的URL（使用规范化路径），用于比较
     */
    private volatile URL cleanedUrl;

    /**
     * 基于给定的URI对象创建新的UrlResource
     *
     * @param uri URI
     * @throws MalformedURLException 如果给定的URL路径无效
     */
    public UrlResource(URI uri) throws MalformedURLException {
        Assert.notNull(uri, "URI must not be null");
        this.uri = uri;
        this.url = uri.toURL();
    }

    /**
     * 基于给定的URL对象创建新的URL资源
     *
     * @param url URL
     */
    public UrlResource(URL url) {
        Assert.notNull(url, "URL must not be null");
        this.uri = null;
        this.url = url;
    }

    /**
     * 基于URL路径创建新的{@code UrlResource}。
     * 注意：如有必要，需要对给定路径进行预编码
     *
     * @param path URL路径
     * @throws MalformedURLException 如果给定的URL路径无效
     * @see java.net.URL#URL(String)
     */
    public UrlResource(String path) throws MalformedURLException {
        Assert.notNull(path, "Path must not be null");
        this.uri = null;
        this.url = new URL(path);
        this.cleanedUrl = getCleanedUrl(this.url, path);
    }

    /**
     * Create a new {@code UrlResource} based on a URI specification.
     * <p>The given parts will automatically get encoded if necessary.
     *
     * @param protocol 要使用的URL协议(例如，"jar"或"file" - 不带冒号): 也称为"scheme"
     * @param location 位置(例如，该协议中的文件路径): 也称为“方案特定部分”
     * @throws MalformedURLException 如果给定的URL规范无效
     * @see java.net.URI#URI(String, String, String)
     */
    public UrlResource(String protocol, String location) throws MalformedURLException {
        this(protocol, location, null);
    }

    /**
     * 基于URI规范创建新的UrlResource。
     * 如有必要，将自动对给定部分进行编码。
     *
     * @param protocol 要使用的URL协议(例如，"jar"或"file" - 不带冒号): 也称为"scheme"
     * @param location 位置(例如，该协议中的文件路径): 也称为“方案特定部分”
     * @param fragment 该位置内的片段(例如，HTML页面上的锚定，如下所示，在"#"分隔符后)
     * @throws MalformedURLException 如果给定的URL规范无效
     * @see java.net.URI#URI(String, String, String)
     */
    public UrlResource(String protocol, String location, String fragment) throws MalformedURLException {
        try {
            this.uri = new URI(protocol, location, fragment);
            this.url = this.uri.toURL();
        } catch (URISyntaxException ex) {
            MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
            exToThrow.initCause(ex);
            throw exToThrow;
        }
    }

    /**
     * 确定给定原始URL的已清理URL
     *
     * @param originalUrl  原始URL
     * @param originalPath 原始URL路径
     * @return 已清理的URL(可能是原始URL原样)
     * @see org.clever.util.StringUtils#cleanPath
     */
    private static URL getCleanedUrl(URL originalUrl, String originalPath) {
        String cleanedPath = StringUtils.cleanPath(originalPath);
        if (!cleanedPath.equals(originalPath)) {
            try {
                return new URL(cleanedPath);
            } catch (MalformedURLException ex) {
                // Cleaned URL path cannot be converted to URL -> take original URL.
            }
        }
        return originalUrl;
    }

    /**
     * 延迟确定给定原始URL的已清理URL
     *
     * @see #getCleanedUrl(URL, String)
     */
    private URL getCleanedUrl() {
        URL cleanedUrl = this.cleanedUrl;
        if (cleanedUrl != null) {
            return cleanedUrl;
        }
        cleanedUrl = getCleanedUrl(this.url, (this.uri != null ? this.uri : this.url).toString());
        this.cleanedUrl = cleanedUrl;
        return cleanedUrl;
    }

    /**
     * 此实现为给定URL打开一个InputStream。
     * 它将{@code useCaches}标志设置为false，主要是为了避免在Windows上锁定jar文件。
     *
     * @see java.net.URL#openConnection()
     * @see java.net.URLConnection#setUseCaches(boolean)
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        URLConnection con = this.url.openConnection();
        ResourceUtils.useCachesIfNecessary(con);
        try {
            return con.getInputStream();
        } catch (IOException ex) {
            // Close the HTTP connection (if applicable).
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).disconnect();
            }
            throw ex;
        }
    }

    /**
     * 此实现返回基础URL引用
     */
    @Override
    public URL getURL() {
        return this.url;
    }

    /**
     * 如果可能的话，此实现直接返回底层URI
     */
    @Override
    public URI getURI() throws IOException {
        if (this.uri != null) {
            return this.uri;
        } else {
            return super.getURI();
        }
    }

    @Override
    public boolean isFile() {
        if (this.uri != null) {
            return super.isFile(this.uri);
        } else {
            return super.isFile();
        }
    }

    /**
     * 此实现返回基础URL/URI的文件引用，前提是它引用文件系统中的文件。
     *
     * @see org.clever.util.ResourceUtils#getFile(java.net.URL, String)
     */
    @Override
    public File getFile() throws IOException {
        if (this.uri != null) {
            return super.getFile(this.uri);
        } else {
            return super.getFile();
        }
    }

    /**
     * 此实现创建一个{@code UrlResource}，委托{@link #createRelativeURL(String)}来适应相对路径。
     *
     * @see #createRelativeURL(String)
     */
    @Override
    public Resource createRelative(String relativePath) throws MalformedURLException {
        return new UrlResource(createRelativeURL(relativePath));
    }

    /**
     * 此委托创建{@code java.net.URL}，应用相对于此资源描述符的基础URL路径的给定路径。前导斜杠将被删除；"#"符号将被编码
     *
     * @see #createRelative(String)
     * @see java.net.URL#URL(java.net.URL, String)
     */
    protected URL createRelativeURL(String relativePath) throws MalformedURLException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        // # can appear in filenames, java.net.URL should not treat it as a fragment
        relativePath = StringUtils.replace(relativePath, "#", "%23");
        // Use the URL constructor for applying the relative path as a URL spec
        return new URL(this.url, relativePath);
    }

    /**
     * 此实现返回此URL引用的文件的名称
     *
     * @see java.net.URL#getPath()
     */
    @Override
    public String getFilename() {
        return StringUtils.getFilename(getCleanedUrl().getPath());
    }

    /**
     * 此实现返回包含URL的描述
     */
    @Override
    public String getDescription() {
        return "URL [" + this.url + "]";
    }

    /**
     * 此实现比较底层URL引用
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof UrlResource && getCleanedUrl().equals(((UrlResource) other).getCleanedUrl())));
    }

    /**
     * 此实现返回基础URL引用的哈希代码
     */
    @Override
    public int hashCode() {
        return getCleanedUrl().hashCode();
    }
}
