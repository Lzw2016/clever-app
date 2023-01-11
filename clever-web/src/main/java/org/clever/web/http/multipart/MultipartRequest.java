package org.clever.web.http.multipart;

import org.clever.util.MultiValueMap;
import org.clever.web.http.multipart.support.MultipartHttpServletRequest;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 该接口定义了为实际多部分请求公开的多部分请求访问操作。它由 {@link MultipartHttpServletRequest} 扩展。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/10 21:34 <br/>
 */
public interface MultipartRequest {
    /**
     * 返回包含此请求中包含的多部分文件的参数名称的 String 对象的 {@link java.util.Iterator}。
     * 这些是表单的字段名称（与普通参数一样），而不是原始文件名。
     *
     * @return 文件名
     */
    Iterator<String> getFileNames();

    /**
     * 返回此请求中上传文件的内容和描述，如果不存在则返回 {@code null}。
     *
     * @param name 一个字符串，指定多部分文件的参数名称
     * @return 以 {@link MultipartFile} 对象形式上传的内容
     */
    MultipartFile getFile(String name);

    /**
     * 返回本次请求中上传文件的内容和描述，不存在返回空列表
     *
     * @param name 一个字符串，指定多部分文件的参数名称
     * @return 以 {@link MultipartFile} 列表形式上传的内容
     */
    List<MultipartFile> getFiles(String name);

    /**
     * 返回此请求中包含的多部分文件的 {@link java.util.Map}
     *
     * @return 包含参数名称作为键和 {@link MultipartFile} 对象作为值的映射
     */
    Map<String, MultipartFile> getFileMap();

    /**
     * 返回此请求中包含的多部分文件的 {@link MultiValueMap}
     *
     * @return 包含参数名称作为键的映射，以及作为值的 {@link MultipartFile} 对象列表
     */
    MultiValueMap<String, MultipartFile> getMultiFileMap();

    /**
     * 判断指定请求部分的内容类型。
     *
     * @param paramOrFileName part name
     * @return 关联的内容类型，如果未定义，则为 {@code null}
     */
    String getMultipartContentType(String paramOrFileName);
}
