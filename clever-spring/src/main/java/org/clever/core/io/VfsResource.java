package org.clever.core.io;

import org.clever.core.NestedIOException;
import org.clever.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * 基于JBoss VFS的{@link Resource}实现。
 * 该类在JBoss 6+(org.JBoss.vfs)上支持VFS 3.x，与JBoss 7和WildFly 8+ 兼容
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:50 <br/>
 */
public class VfsResource extends AbstractResource {
    private final Object resource;

    /**
     * 创建一个新的VfsResource，包装给定的资源句柄
     *
     * @param resource 一个{@code org.jboss.vfs.VirtualFile}实例(非类型化，以避免对VFS API的静态依赖)
     */
    public VfsResource(Object resource) {
        Assert.notNull(resource, "VirtualFile must not be null");
        this.resource = resource;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VfsUtils.getInputStream(this.resource);
    }

    @Override
    public boolean exists() {
        return VfsUtils.exists(this.resource);
    }

    @Override
    public boolean isReadable() {
        return VfsUtils.isReadable(this.resource);
    }

    @Override
    public URL getURL() throws IOException {
        try {
            return VfsUtils.getURL(this.resource);
        } catch (Exception ex) {
            throw new NestedIOException("Failed to obtain URL for file " + this.resource, ex);
        }
    }

    @Override
    public URI getURI() throws IOException {
        try {
            return VfsUtils.getURI(this.resource);
        } catch (Exception ex) {
            throw new NestedIOException("Failed to obtain URI for " + this.resource, ex);
        }
    }

    @Override
    public File getFile() throws IOException {
        return VfsUtils.getFile(this.resource);
    }

    @Override
    public long contentLength() throws IOException {
        return VfsUtils.getSize(this.resource);
    }

    @Override
    public long lastModified() throws IOException {
        return VfsUtils.getLastModified(this.resource);
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        if (!relativePath.startsWith(".") && relativePath.contains("/")) {
            try {
                return new VfsResource(VfsUtils.getChild(this.resource, relativePath));
            } catch (IOException ex) {
                // fall back to getRelative
            }
        }
        return new VfsResource(VfsUtils.getRelative(new URL(getURL(), relativePath)));
    }

    @Override
    public String getFilename() {
        return VfsUtils.getName(this.resource);
    }

    @Override
    public String getDescription() {
        return "VFS resource [" + this.resource + "]";
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof VfsResource && this.resource.equals(((VfsResource) other).resource)));
    }

    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }
}
