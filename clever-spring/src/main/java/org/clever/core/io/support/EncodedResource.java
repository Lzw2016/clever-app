package org.clever.core.io.support;

import org.clever.core.io.InputStreamSource;
import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * 将资源描述符与用于从资源中读取的特定编码或字符集相结合的持有者
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 22:14 <br/>
 *
 * @see Resource#getInputStream()
 * @see java.io.Reader
 * @see java.nio.charset.Charset
 */
public class EncodedResource implements InputStreamSource {
    private final Resource resource;
    private final String encoding;
    private final Charset charset;

    /**
     * 为给定资源创建新的EncodedResource，而不指定显式编码或字符集
     *
     * @param resource 要保留的资源（从不为null）
     */
    public EncodedResource(Resource resource) {
        this(resource, null, null);
    }

    /**
     * 使用指定的编码为给定资源创建新的EncodedResource
     *
     * @param resource 要保留的资源（从不为null）
     * @param encoding 用于读取资源的编码
     */
    public EncodedResource(Resource resource, String encoding) {
        this(resource, encoding, null);
    }

    /**
     * 使用指定的字符集为给定资源创建新的EncodedResource
     *
     * @param resource 要保留的资源（从不为null）
     * @param charset  用于读取资源的字符集
     */
    public EncodedResource(Resource resource, Charset charset) {
        this(resource, null, charset);
    }

    private EncodedResource(Resource resource, String encoding, Charset charset) {
        super();
        Assert.notNull(resource, "Resource must not be null");
        this.resource = resource;
        this.encoding = encoding;
        this.charset = charset;
    }

    /**
     * 返回此EncodedResource持有的资源
     */
    public final Resource getResource() {
        return this.resource;
    }

    /**
     * 返回用于从资源读取的编码，如果未指定，则返回null
     */
    public final String getEncoding() {
        return this.encoding;
    }

    /**
     * 返回用于读取资源的字符集，如果未指定，则返回null
     */
    public final Charset getCharset() {
        return this.charset;
    }

    /**
     * 确定是否需要读取器而不是InputStream，即是否指定了编码或字符集
     *
     * @see #getReader()
     * @see #getInputStream()
     */
    public boolean requiresReader() {
        return (this.encoding != null || this.charset != null);
    }

    /**
     * 打开{@code java.io.Reader}指定资源的读取器，使用指定的字符集或编码（如果有）
     *
     * @throws IOException 如果打开读卡器失败
     * @see #requiresReader()
     * @see #getInputStream()
     */
    public Reader getReader() throws IOException {
        if (this.charset != null) {
            return new InputStreamReader(this.resource.getInputStream(), this.charset);
        } else if (this.encoding != null) {
            return new InputStreamReader(this.resource.getInputStream(), this.encoding);
        } else {
            return new InputStreamReader(this.resource.getInputStream());
        }
    }

    /**
     * 打开指定资源的InputStream，忽略任何指定的字符集或编码
     *
     * @throws IOException 如果打开InputStream失败
     * @see #requiresReader()
     * @see #getReader()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return this.resource.getInputStream();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EncodedResource)) {
            return false;
        }
        EncodedResource otherResource = (EncodedResource) other;
        return (this.resource.equals(otherResource.resource)
                && ObjectUtils.nullSafeEquals(this.charset, otherResource.charset)
                && ObjectUtils.nullSafeEquals(this.encoding, otherResource.encoding));
    }

    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }

    @Override
    public String toString() {
        return this.resource.toString();
    }
}
