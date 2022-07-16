package org.clever.boot.context.config;

import org.clever.core.io.FileSystemResource;
import org.clever.core.io.FileUrlResource;
import org.clever.core.io.Resource;
import org.clever.util.Assert;

import java.io.IOException;

/**
 * {@link ConfigDataResource} 由 {@link Resource} 支持。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:10 <br/>
 */
public class StandardConfigDataResource extends ConfigDataResource {
    private final StandardConfigDataReference reference;
    private final Resource resource;
    private final boolean emptyDirectory;

    /**
     * 创建新的 {@link StandardConfigDataResource}
     *
     * @param reference 资源引用
     * @param resource  基础资源
     */
    StandardConfigDataResource(StandardConfigDataReference reference, Resource resource) {
        this(reference, resource, false);
    }

    /**
     * 创建新的 {@link StandardConfigDataResource}
     *
     * @param reference      资源引用
     * @param resource       基础资源
     * @param emptyDirectory 如果资源是我们知道存在的空目录
     */
    StandardConfigDataResource(StandardConfigDataReference reference, Resource resource, boolean emptyDirectory) {
        Assert.notNull(reference, "Reference must not be null");
        Assert.notNull(resource, "Resource must not be null");
        this.reference = reference;
        this.resource = resource;
        this.emptyDirectory = emptyDirectory;
    }

    StandardConfigDataReference getReference() {
        return this.reference;
    }

    /**
     * 返回正在加载的底层 {@link Resource}
     *
     * @return 基础资源
     */
    public Resource getResource() {
        return this.resource;
    }

    /**
     * 如果资源不是特定于配置文件的，则返回配置文件或null。
     *
     * @return 配置文件或null
     */
    public String getProfile() {
        return this.reference.getProfile();
    }

    boolean isEmptyDirectory() {
        return this.emptyDirectory;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StandardConfigDataResource other = (StandardConfigDataResource) obj;
        return this.resource.equals(other.resource) && this.emptyDirectory == other.emptyDirectory;
    }

    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }

    @Override
    public String toString() {
        if (this.resource instanceof FileSystemResource || this.resource instanceof FileUrlResource) {
            try {
                return "file [" + this.resource.getFile().toString() + "]";
            } catch (IOException ignored) {
            }
        }
        return this.resource.toString();
    }
}
