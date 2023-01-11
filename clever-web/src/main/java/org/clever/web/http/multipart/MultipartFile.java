package org.clever.web.http.multipart;

import org.clever.core.io.InputStreamSource;
import org.clever.core.io.Resource;
import org.clever.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 在多部分请求中收到的上传文件的表示。
 *
 * <p>文件内容要么存储在内存中，要么临时存储在磁盘上。
 * 在任何一种情况下，用户都负责根据需要将文件内容复制到会话级或持久存储。
 * 临时存储将在请求处理结束时被清除。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/01 22:03 <br/>
 */
public interface MultipartFile extends InputStreamSource {
    /**
     * 以多部分形式返回参数的名称。
     *
     * @return the name of the parameter (never {@code null} or empty)
     */
    String getName();

    /**
     * 返回客户端文件系统中的原始文件名。
     * <p>这可能包含路径信息，具体取决于所使用的浏览器，但它通常不会与 Opera 以外的任何其他浏览器一起使用。
     * <p><strong>注意: </strong>请记住，此文件名由客户提供，不应盲目使用。
     * 除了不使用目录部分外，文件名还可以包含诸如“..”等可被恶意使用的字符。
     * 建议不要直接使用此文件名。如有必要，最好生成一个唯一的并将其保存在某处以供参考。
     *
     * @return 原始文件名，如果在多部分表单中没有选择文件，则为空字符串，如果未定义或不可用，则为 {@code null}
     * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
     * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">无限制文件上传</a>
     */
    String getOriginalFilename();

    /**
     * 返回文件的内容类型。
     *
     * @return 内容类型，或 {@code null} 如果未定义（或未在多部分表单中选择文件）
     */
    String getContentType();

    /**
     * 返回上传的文件是否为空，即多部分表单中没有选择文件或选择的文件没有内容。
     */
    boolean isEmpty();

    /**
     * 以字节为单位返回文件的大小。
     *
     * @return 文件的大小，如果为空则为 0
     */
    long getSize();

    /**
     * 将文件的内容作为字节数组返回。
     *
     * @return 文件的内容为字节，如果为空则为空字节数组
     * @throws IOException 如果出现访问错误（如果临时存储失败）
     */
    byte[] getBytes() throws IOException;

    /**
     * 返回一个 InputStream 以从中读取文件的内容。
     * <p>用户负责关闭返回的流。
     *
     * @return 文件的内容作为流，如果为空则为空流
     * @throws IOException 如果出现访问错误（如果临时存储失败）
     */
    @Override
    InputStream getInputStream() throws IOException;

    /**
     * 返回此 MultipartFile 的资源表示。
     * 这可以用作 {@code RestTemplate} 或 {@code WebClient} 的输入，以公开内容长度和文件名以及 InputStream。
     *
     * @return 这个 MultipartFile 适应了 Resource 契约
     */
    default Resource getResource() {
        return new MultipartFileResource(this);
    }

    /**
     * 将接收到的文件传输到给定的目标文件。
     * <p>这可能会在文件系统中移动文件，在文件系统中复制文件，或者将内存保存的内容保存到目标文件。
     * 如果目标文件已经存在，它将首先被删除。
     * <p>如果目标文件已在文件系统中移动，则此操作之后无法再次调用。因此，只需调用此方法一次即可使用任何存储机制。
     * <p><b>注意:</b> 根据底层提供者的不同，临时存储可能依赖于容器，包括此处指定的相对目标的基本目录（例如，使用 Servlet 3.0 多部分处理）。
     * 对于绝对目标，目标文件可能会被重命名从其临时位置移动或新复制，即使临时副本已存在。
     *
     * @param dest 目标文件（通常是绝对的）
     * @throws IOException           在读取或写入错误的情况下
     * @throws IllegalStateException 如果文件已经在文件系统中移动并且不再可用于另一次传输
     * @see javax.servlet.http.Part#write(String)
     */
    void transferTo(File dest) throws IOException, IllegalStateException;

    /**
     * 将接收到的文件传输到给定的目标文件。
     * <p>默认实现只是复制文件输入流。
     *
     * @see #getInputStream()
     * @see #transferTo(File)
     */
    default void transferTo(Path dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
    }
}
