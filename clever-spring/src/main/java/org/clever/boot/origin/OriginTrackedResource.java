package org.clever.boot.origin;

import org.clever.core.io.Resource;
import org.clever.core.io.WritableResource;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

/**
 * 可用于将{@link Origin}信息添加到{@link Resource}或{@link WritableResource}的装饰器。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:56 <br/>
 *
 * @see #of(Resource, Origin)
 * @see #of(WritableResource, Origin)
 * @see OriginProvider
 */
public class OriginTrackedResource implements Resource, OriginProvider {
    private final Resource resource;
    private final Origin origin;

    /**
     * 创建新的 {@link OriginTrackedResource}
     *
     * @param resource 要跟踪的资源
     * @param origin   资源的来源
     */
    OriginTrackedResource(Resource resource, Origin origin) {
        Assert.notNull(resource, "Resource must not be null");
        this.resource = resource;
        this.origin = origin;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return getResource().getInputStream();
    }

    @Override
    public boolean exists() {
        return getResource().exists();
    }

    @Override
    public boolean isReadable() {
        return getResource().isReadable();
    }

    @Override
    public boolean isOpen() {
        return getResource().isOpen();
    }

    @Override
    public boolean isFile() {
        return getResource().isFile();
    }

    @Override
    public URL getURL() throws IOException {
        return getResource().getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return getResource().getURI();
    }

    @Override
    public File getFile() throws IOException {
        return getResource().getFile();
    }

    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        return getResource().readableChannel();
    }

    @Override
    public long contentLength() throws IOException {
        return getResource().contentLength();
    }

    @Override
    public long lastModified() throws IOException {
        return getResource().lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return getResource().createRelative(relativePath);
    }

    @Override
    public String getFilename() {
        return getResource().getFilename();
    }

    @Override
    public String getDescription() {
        return getResource().getDescription();
    }

    public Resource getResource() {
        return this.resource;
    }

    @Override
    public Origin getOrigin() {
        return this.origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OriginTrackedResource other = (OriginTrackedResource) obj;
        return this.resource.equals(other) && ObjectUtils.nullSafeEquals(this.origin, other.origin);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = this.resource.hashCode();
        result = prime * result + ObjectUtils.nullSafeHashCode(this.origin);
        return result;
    }

    @Override
    public String toString() {
        return this.resource.toString();
    }

    /**
     * 返回给定{@link WritableResource}的新{@link OriginProvider 原点跟踪}版本。
     *
     * @param resource 被跟踪的资源
     * @param origin   资源的来源
     * @return {@link OriginTrackedWritableResource} 实例
     */
    public static OriginTrackedWritableResource of(WritableResource resource, Origin origin) {
        return (OriginTrackedWritableResource) of((Resource) resource, origin);
    }

    /**
     * 返回给定{@link Resource}的新{@link OriginProvider 原点跟踪}版本。
     *
     * @param resource 被跟踪的资源
     * @param origin   资源的来源
     * @return {@link OriginTrackedResource} 实例
     */
    public static OriginTrackedResource of(Resource resource, Origin origin) {
        if (resource instanceof WritableResource) {
            return new OriginTrackedWritableResource((WritableResource) resource, origin);
        }
        return new OriginTrackedResource(resource, origin);
    }

    /**
     * {@link WritableResource}实例的{@link OriginTrackedResource}变体。
     */
    public static class OriginTrackedWritableResource extends OriginTrackedResource implements WritableResource {
        /**
         * 创建新的 {@link OriginTrackedWritableResource}
         *
         * @param resource 要跟踪的资源
         * @param origin   资源的来源
         */
        OriginTrackedWritableResource(WritableResource resource, Origin origin) {
            super(resource, origin);
        }

        @Override
        public WritableResource getResource() {
            return (WritableResource) super.getResource();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return getResource().getOutputStream();
        }
    }
}
