package org.clever.web.http;

import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;
import org.clever.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 用于从 {@link Resource} 句柄或文件名解析 {@link MediaType} 对象的工厂委托
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 14:38 <br/>
 */
public final class MediaTypeFactory {
    private static final String MIME_TYPES_FILE_NAME = "/org/clever/http/mime.types";
    private static final MultiValueMap<String, MediaType> fileExtensionToMediaTypes = parseMimeTypes();

    private MediaTypeFactory() {
    }

    /**
     * 解析在资源中找到的 {@code mime.types} 文件。格式为：
     * <code>
     * # 注释以 '#' 开头<br>
     * # 格式为 <mime类型> <空格分隔的文件扩展名><br>
     * # 例如: <br>
     * text/plain    txt text<br>
     * # 这会将 file.txt 和 file.text 映射到mime类型 “text/plain”<br>
     * </code>
     *
     * @return 多值映射，将媒体类型映射到文件扩展名
     */
    private static MultiValueMap<String, MediaType> parseMimeTypes() {
        InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME);
        Assert.state(is != null, MIME_TYPES_FILE_NAME + " not found in classpath");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
            MultiValueMap<String, MediaType> result = new LinkedMultiValueMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
                MediaType mediaType = MediaType.parseMediaType(tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
                    result.add(fileExtension, mediaType);
                }
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + MIME_TYPES_FILE_NAME, ex);
        }
    }

    /**
     * 如果可能，确定给定资源的媒体类型
     *
     * @param resource 内省的资源
     * @return 相应的媒体类型，或者 {@code null} 如果没有找到
     */
    public static Optional<MediaType> getMediaType(Resource resource) {
        return Optional.ofNullable(resource).map(Resource::getFilename).flatMap(MediaTypeFactory::getMediaType);
    }

    /**
     * 如果可能，确定给定文件名的媒体类型
     *
     * @param filename 文件名加扩展名
     * @return 相应的媒体类型，或者 {@code null} 如果没有找到
     */
    public static Optional<MediaType> getMediaType(String filename) {
        return getMediaTypes(filename).stream().findFirst();
    }

    /**
     * 如果可能，确定给定文件名的媒体类型
     *
     * @param filename 文件名加扩展名
     * @return 相应的媒体类型，如果没有找到则为空列表
     */
    public static List<MediaType> getMediaTypes(String filename) {
        List<MediaType> mediaTypes = null;
        String ext = StringUtils.getFilenameExtension(filename);
        if (ext != null) {
            mediaTypes = fileExtensionToMediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
        }
        return (mediaTypes != null ? mediaTypes : Collections.emptyList());
    }
}
