package org.clever.core.io;

import org.clever.util.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * 给定{@link InputStream}的资源实现。
 * 只有在没有其他具体资源实施适用的情况下才应使用。
 * 特别是，如果可能，首选{@link ByteArrayResource}或任何基于文件的资源实现。
 * 与其他资源实现不同，这是已打开资源的描述符，因此从{@link #isOpen()}返回true。
 * 如果需要将资源描述符保留在某个位置，或者需要多次读取流，请不要使用InputStreamResource
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 14:11 <br/>
 *
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileSystemResource
 * @see UrlResource
 */
public class InputStreamResource extends AbstractResource {
    private final InputStream inputStream;
    private final String description;
    private boolean read = false;

    /**
     * 创建新的InputStreamResource
     *
     * @param inputStream 要使用的InputStream
     */
    public InputStreamResource(InputStream inputStream) {
        this(inputStream, "resource loaded through InputStream");
    }

    /**
     * 创建新的InputStreamResource
     *
     * @param inputStream 要使用的InputStream
     * @param description InputStream的来源
     */
    public InputStreamResource(InputStream inputStream, String description) {
        Assert.notNull(inputStream, "InputStream must not be null");
        this.inputStream = inputStream;
        this.description = (description != null ? description : "");
    }

    /**
     * 此实现始终返回 {@code true}.
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * 此实现始终返回 {@code true}.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * 如果尝试多次读取底层流，此实现将引发IllegalStateException
     */
    @Override
    public InputStream getInputStream() throws IOException, IllegalStateException {
        if (this.read) {
            throw new IllegalStateException(
                    "InputStream has already been read - " + "do not use InputStreamResource if a stream needs to be read multiple times"
            );
        }
        this.read = true;
        return this.inputStream;
    }

    /**
     * 此实现返回包含传入描述（如果有）的描述
     */
    @Override
    public String getDescription() {
        return "InputStream resource [" + this.description + "]";
    }

    /**
     * 此实现比较底层InputStream
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof InputStreamResource && ((InputStreamResource) other).inputStream.equals(this.inputStream)));
    }

    /**
     * 此实现返回底层InputStream的哈希代码
     */
    @Override
    public int hashCode() {
        return this.inputStream.hashCode();
    }
}
