package org.clever.scripting.support;

import org.clever.core.io.Resource;
import org.clever.core.io.support.EncodedResource;
import org.clever.scripting.ScriptSource;
import org.clever.util.Assert;
import org.clever.util.FileCopyUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;

/**
 * {@link ScriptSource} 实现基于 {@link Resource} 抽象。
 * 从底层资源的 {@link Resource#getFile() File} 或 {@link Resource#getInputStream() InputStream} 加载脚本文本，并跟踪文件的最后修改时间戳（如果可能）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:20 <br/>
 *
 * @see org.clever.core.io.Resource#getInputStream()
 * @see org.clever.core.io.Resource#getFile()
 * @see org.clever.core.io.ResourceLoader
 */
public class ResourceScriptSource implements ScriptSource {
    /**
     * 子类可用的记录器
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private EncodedResource resource;
    private long lastModified = -1;
    private final Object lastModifiedMonitor = new Object();

    /**
     * 为给定的资源创建一个新的 ResourceScriptSource
     *
     * @param resource 从中加载脚本的 EncodedResource
     */
    public ResourceScriptSource(EncodedResource resource) {
        Assert.notNull(resource, "Resource must not be null");
        this.resource = resource;
    }

    /**
     * 为给定的资源创建一个新的 ResourceScriptSource
     *
     * @param resource 从中加载脚本的资源（使用 UTF-8 编码）
     */
    public ResourceScriptSource(Resource resource) {
        Assert.notNull(resource, "Resource must not be null");
        this.resource = new EncodedResource(resource, "UTF-8");
    }

    /**
     * 返回 {@link Resource} 以从中加载脚本
     */
    public final Resource getResource() {
        return this.resource.getResource();
    }

    /**
     * 设置用于读取脚本资源的编码。
     * <p>常规资源的默认值为“UTF-8”。{@code null} 值表示平台默认值。
     */
    public void setEncoding(String encoding) {
        this.resource = new EncodedResource(this.resource.getResource(), encoding);
    }

    @Override
    public String getScriptAsString() throws IOException {
        synchronized (this.lastModifiedMonitor) {
            this.lastModified = retrieveLastModifiedTime();
        }
        Reader reader = this.resource.getReader();
        return FileCopyUtils.copyToString(reader);
    }

    @Override
    public boolean isModified() {
        synchronized (this.lastModifiedMonitor) {
            return (this.lastModified < 0 || retrieveLastModifiedTime() > this.lastModified);
        }
    }

    /**
     * 检索基础资源的当前最后修改时间戳
     *
     * @return 当前时间戳，如果无法确定则为 0
     */
    protected long retrieveLastModifiedTime() {
        try {
            return getResource().lastModified();
        } catch (IOException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(getResource() + " could not be resolved in the file system - " + "current timestamp not available for script modification check", ex);
            }
            return 0;
        }
    }

    @Override
    public String suggestedClassName() {
        String filename = getResource().getFilename();
        return (filename != null ? StringUtils.stripFilenameExtension(filename) : null);
    }

    @Override
    public String toString() {
        return this.resource.toString();
    }
}
