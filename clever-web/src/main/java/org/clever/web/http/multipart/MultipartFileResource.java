package org.clever.web.http.multipart;

import org.clever.core.io.AbstractResource;
import org.clever.util.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * 将 {@link MultipartFile} 改编为 {@link org.clever.core.io.Resource}，
 * 将内容公开为 {@code InputStream} 并覆盖 {@link #contentLength()} 以及 {@link #getFilename()} .
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/01 22:04 <br/>
 *
 * @see MultipartFile#getResource()
 */
class MultipartFileResource extends AbstractResource {
    private final MultipartFile multipartFile;

    public MultipartFileResource(MultipartFile multipartFile) {
        Assert.notNull(multipartFile, "MultipartFile must not be null");
        this.multipartFile = multipartFile;
    }

    /**
     * 此实现始终返回 {@code true}
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * 此实现始终返回 {@code true}
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public long contentLength() {
        return this.multipartFile.getSize();
    }

    @Override
    public String getFilename() {
        return this.multipartFile.getOriginalFilename();
    }

    /**
     * 如果多次尝试读取底层流，此实现会抛出 IllegalStateException。
     */
    @Override
    public InputStream getInputStream() throws IOException, IllegalStateException {
        return this.multipartFile.getInputStream();
    }

    /**
     * 此实现返回具有多部分名称的描述
     */
    @Override
    public String getDescription() {
        return "MultipartFile resource [" + this.multipartFile.getName() + "]";
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof MultipartFileResource && ((MultipartFileResource) other).multipartFile.equals(this.multipartFile)));
    }

    @Override
    public int hashCode() {
        return this.multipartFile.hashCode();
    }
}
