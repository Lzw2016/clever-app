package org.clever.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于作为{@link InputStream}源的对象的简单接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 14:10 <br/>
 *
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 */
public interface InputStreamSource {
    /**
     * 返回基础资源内容的{@link InputStream}。
     * 每个调用都会创建一个新流。
     *
     * @return 基础资源的输入流(不能是null)
     * @throws java.io.FileNotFoundException 如果基础资源不存在
     * @throws IOException                   如果无法打开内容流
     * @see Resource#isReadable()
     */
    InputStream getInputStream() throws IOException;
}
