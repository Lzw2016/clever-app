package org.clever.web.http;

import org.clever.util.*;
import org.clever.web.exception.InvalidMediaTypeException;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@link MimeType} 的子类，添加了对 HTTP 规范中定义的质量参数的支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 14:02 <br/>
 *
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1">HTTP 1.1: Semantics and Content, section 3.1.1.1</a>
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public class MediaType extends MimeType implements Serializable {
    private static final long serialVersionUID = 2069937152339670231L;
    /**
     * 包含所有媒体范围的公共常量媒体类型（即“&42;/&42;”）
     */
    public static final MediaType ALL;
    /**
     * 相当于 {@link MediaType#ALL} 的字符串
     */
    public static final String ALL_VALUE = "*/*";
    /**
     * {@code application/atom+xml} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_ATOM_XML;
    /**
     * 相当于 {@link MediaType#APPLICATION_ATOM_XML} 的字符串
     */
    public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";
    /**
     * {@code application/cbor} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_CBOR;
    /**
     * 相当于 {@link MediaType#APPLICATION_CBOR} 的字符串
     */
    public static final String APPLICATION_CBOR_VALUE = "application/cbor";
    /**
     * {@code application/x-www-form-urlencoded} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_FORM_URLENCODED;
    /**
     * 相当于 {@link MediaType#APPLICATION_FORM_URLENCODED} 的字符串
     */
    public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";
    /**
     * {@code application/graphql+json} 的公共常量媒体类型
     *
     * @see <a href="https://github.com/graphql/graphql-over-http">GraphQL over HTTP spec</a>
     */
    public static final MediaType APPLICATION_GRAPHQL;
    /**
     * 相当于 {@link MediaType#APPLICATION_GRAPHQL} 的字符串
     */
    public static final String APPLICATION_GRAPHQL_VALUE = "application/graphql+json";
    /**
     * {@code application/json} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_JSON;
    /**
     * 相当于 {@link MediaType#APPLICATION_JSON} 的字符串
     *
     * @see #APPLICATION_JSON_UTF8_VALUE
     */
    public static final String APPLICATION_JSON_VALUE = "application/json";
    /**
     * {@code application/json;charset=UTF-8} 的公共常量媒体类型
     *
     * @deprecated 支持 {@link #APPLICATION_JSON}，因为像 Chrome 这样的主要浏览器
     * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">现在符合规范</a>
     * 并在不需要 {@code charset=UTF-8} 参数的情况下正确解释 UTF-8 特殊字符
     */
    @Deprecated
    public static final MediaType APPLICATION_JSON_UTF8;
    /**
     * 相当于 {@link MediaType#APPLICATION_JSON_UTF8} 的字符串
     *
     * @deprecated 支持 {@link #APPLICATION_JSON_VALUE}，因为像 Chrome 这样的主要浏览器
     * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">现在符合规范</a>
     * 并在不需要 {@code charset=UTF-8} 参数的情况下正确解释 UTF-8 特殊字符
     */
    @Deprecated
    public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";
    /**
     * {@code application/octet-stream} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_OCTET_STREAM;
    /**
     * 相当于  的字符串
     * A String equivalent of {@link MediaType#APPLICATION_OCTET_STREAM}.
     */
    public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";
    /**
     * {@code application/pdf} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_PDF;
    /**
     * 相当于 {@link MediaType#APPLICATION_PDF} 的字符串
     */
    public static final String APPLICATION_PDF_VALUE = "application/pdf";
    /**
     * {@code application/problem+json} 的公共常量媒体类型
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
     * Problem Details for HTTP APIs, 6.1. application/problem+json</a>
     */
    public static final MediaType APPLICATION_PROBLEM_JSON;
    /**
     * 相当于 {@link MediaType#APPLICATION_PROBLEM_JSON} 的字符串
     */
    public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";
    /**
     * {@code application/problem+json} 的公共常量媒体类型
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
     * HTTP API 的问题详细信息，6.1。application/problem+json</a>
     * @deprecated 支持 {@link #APPLICATION_PROBLEM_JSON}，因为像 Chrome 这样的主要浏览器
     * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">现在符合规范</a>
     * 并在不需要 {@code charset=UTF-8} 参数的情况下正确解释 UTF-8 特殊字符
     */
    @Deprecated
    public static final MediaType APPLICATION_PROBLEM_JSON_UTF8;
    /**
     * 相当于 {@link MediaType#APPLICATION_PROBLEM_JSON_UTF8} 的字符串
     *
     * @deprecated 支持 {@link #APPLICATION_PROBLEM_JSON_VALUE}，因为像 Chrome 这样的主要浏览器
     * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">现在符合规范</a>
     * 并在不需要 {@code charset=UTF-8} 参数的情况下正确解释 UTF-8 特殊字符
     */
    @Deprecated
    public static final String APPLICATION_PROBLEM_JSON_UTF8_VALUE = "application/problem+json;charset=UTF-8";
    /**
     * {@code application/problem+xml} 的公共常量媒体类型
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.2">
     * Problem Details for HTTP APIs, 6.2. application/problem+xml</a>
     */
    public static final MediaType APPLICATION_PROBLEM_XML;
    /**
     * 相当于 {@link MediaType#APPLICATION_PROBLEM_XML} 的字符串
     */
    public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";
    /**
     * {@code application/rss+xml} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_RSS_XML;
    /**
     * 相当于 {@link MediaType#APPLICATION_RSS_XML} 的字符串
     */
    public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";
    /**
     * {@code application/x-ndjson} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_NDJSON;
    /**
     * 相当于 {@link MediaType#APPLICATION_NDJSON} 的字符串
     */
    public static final String APPLICATION_NDJSON_VALUE = "application/x-ndjson";
    /**
     * {@code application/stream+json} 的公共常量媒体类型
     *
     * @deprecated as of 5.3, see notice on {@link #APPLICATION_STREAM_JSON_VALUE}.
     */
    @Deprecated
    public static final MediaType APPLICATION_STREAM_JSON;
    /**
     * 相当于 {@link MediaType#APPLICATION_STREAM_JSON} 的字符串
     *
     * @deprecated 因为它源自 W3C 活动流规范，该规范具有更具体的用途，并且已被不同的 mime 类型所取代。
     * 使用 {@link #APPLICATION_NDJSON} 作为替代或任何其他行分隔的 JSON 格式（例如 JSON 行、JSON 文本序列）。
     */
    @Deprecated
    public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";
    /**
     * {@code application/xhtml+xml} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_XHTML_XML;
    /**
     * 相当于 {@link MediaType#APPLICATION_XHTML_XML} 的字符串
     */
    public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";
    /**
     * {@code application/xml} 的公共常量媒体类型
     */
    public static final MediaType APPLICATION_XML;
    /**
     * 相当于 {@link MediaType#APPLICATION_XML} 的字符串
     */
    public static final String APPLICATION_XML_VALUE = "application/xml";
    /**
     * {@code image/gif} 的公共常量媒体类型
     */
    public static final MediaType IMAGE_GIF;
    /**
     * 相当于 {@link MediaType#IMAGE_GIF} 的字符串
     */
    public static final String IMAGE_GIF_VALUE = "image/gif";
    /**
     * {@code image/jpeg} 的公共常量媒体类型
     */
    public static final MediaType IMAGE_JPEG;
    /**
     * 相当于 {@link MediaType#IMAGE_JPEG} 的字符串
     */
    public static final String IMAGE_JPEG_VALUE = "image/jpeg";
    /**
     * {@code image/png} 的公共常量媒体类型
     */
    public static final MediaType IMAGE_PNG;
    /**
     * 相当于 {@link MediaType#IMAGE_PNG} 的字符串
     */
    public static final String IMAGE_PNG_VALUE = "image/png";
    /**
     * {@code multipart/form-data} 的公共常量媒体类型
     */
    public static final MediaType MULTIPART_FORM_DATA;
    /**
     * 相当于 {@link MediaType#MULTIPART_FORM_DATA} 的字符串
     */
    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";
    /**
     * {@code multipart/mixed} 的公共常量媒体类型
     */
    public static final MediaType MULTIPART_MIXED;
    /**
     * 相当于 {@link MediaType#MULTIPART_MIXED} 的字符串
     */
    public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";
    /**
     * {@code multipart/related} 的公共常量媒体类型
     */
    public static final MediaType MULTIPART_RELATED;
    /**
     * 相当于 {@link MediaType#MULTIPART_RELATED} 的字符串
     */
    public static final String MULTIPART_RELATED_VALUE = "multipart/related";
    /**
     * {@code text/event-stream} 的公共常量媒体类型
     *
     * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
     */
    public static final MediaType TEXT_EVENT_STREAM;
    /**
     * 相当于 {@link MediaType#TEXT_EVENT_STREAM} 的字符串
     */
    public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";
    /**
     * {@code text/html} 的公共常量媒体类型
     */
    public static final MediaType TEXT_HTML;
    /**
     * 相当于 {@link MediaType#TEXT_HTML} 的字符串
     */
    public static final String TEXT_HTML_VALUE = "text/html";
    /**
     * {@code text/markdown} 的公共常量媒体类型
     */
    public static final MediaType TEXT_MARKDOWN;
    /**
     * 相当于 {@link MediaType#TEXT_MARKDOWN} 的字符串
     */
    public static final String TEXT_MARKDOWN_VALUE = "text/markdown";
    /**
     * {@code text/plain} 的公共常量媒体类型
     */
    public static final MediaType TEXT_PLAIN;
    /**
     * 相当于 {@link MediaType#TEXT_PLAIN} 的字符串
     */
    public static final String TEXT_PLAIN_VALUE = "text/plain";
    /**
     * {@code text/xml} 的公共常量媒体类型
     */
    public static final MediaType TEXT_XML;
    /**
     * 相当于 {@link MediaType#TEXT_XML} 的字符串
     */
    public static final String TEXT_XML_VALUE = "text/xml";
    private static final String PARAM_QUALITY_FACTOR = "q";

    static {
        // Not using "valueOf' to avoid static init cost
        ALL = new MediaType("*", "*");
        APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
        APPLICATION_CBOR = new MediaType("application", "cbor");
        APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
        APPLICATION_GRAPHQL = new MediaType("application", "graphql+json");
        APPLICATION_JSON = new MediaType("application", "json");
        APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);
        APPLICATION_NDJSON = new MediaType("application", "x-ndjson");
        APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
        APPLICATION_PDF = new MediaType("application", "pdf");
        APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");
        APPLICATION_PROBLEM_JSON_UTF8 = new MediaType("application", "problem+json", StandardCharsets.UTF_8);
        APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");
        APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
        APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");
        APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");
        APPLICATION_XML = new MediaType("application", "xml");
        IMAGE_GIF = new MediaType("image", "gif");
        IMAGE_JPEG = new MediaType("image", "jpeg");
        IMAGE_PNG = new MediaType("image", "png");
        MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
        MULTIPART_MIXED = new MediaType("multipart", "mixed");
        MULTIPART_RELATED = new MediaType("multipart", "related");
        TEXT_EVENT_STREAM = new MediaType("text", "event-stream");
        TEXT_HTML = new MediaType("text", "html");
        TEXT_MARKDOWN = new MediaType("text", "markdown");
        TEXT_PLAIN = new MediaType("text", "plain");
        TEXT_XML = new MediaType("text", "xml");
    }

    /**
     * 为给定的主要类型创建一个新的 {@code MediaType}。
     * <p>{@linkplain #getSubtype() subtype} 设置为“&42;”，参数为空。
     *
     * @param type 主要类型
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(String type) {
        super(type);
    }

    /**
     * 为给定的主要类型和子类型创建一个新的 {@code MediaType}。
     * <p>参数为空。
     *
     * @param type    主要类型
     * @param subtype subtype
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(String type, String subtype) {
        super(type, subtype, Collections.emptyMap());
    }

    /**
     * 为给定的类型、子类型和字符集创建一个新的 {@code MediaType}。
     *
     * @param type    主要类型
     * @param subtype subtype
     * @param charset 字符集
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(String type, String subtype, Charset charset) {
        super(type, subtype, charset);
    }

    /**
     * 为给定的类型、子类型和质量值创建一个新的 {@code MediaType}。
     *
     * @param type         主要类型
     * @param subtype      subtype
     * @param qualityValue 质量值
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(String type, String subtype, double qualityValue) {
        this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
    }

    /**
     * 复制给定 {@code MediaType} 的类型、子类型和参数的复制构造函数，并允许设置指定的字符集。
     *
     * @param other   另一种媒体类型
     * @param charset 字符集
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(MediaType other, Charset charset) {
        super(other, charset);
    }

    /**
     * 复制给定 {@code MediaType} 的类型和子类型的复制构造函数，并允许使用不同的参数
     *
     * @param other      另一种媒体类型
     * @param parameters 参数，可以是 {@code null}
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(MediaType other, Map<String, String> parameters) {
        super(other.getType(), other.getSubtype(), parameters);
    }

    /**
     * 为给定的类型、子类型和参数创建一个新的 {@code MediaType}
     *
     * @param type       主要类型
     * @param subtype    subtype
     * @param parameters 参数，可以是 {@code null}
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(String type, String subtype, Map<String, String> parameters) {
        super(type, subtype, parameters);
    }

    /**
     * 为给定的 {@link MimeType} 创建一个新的 {@code MediaType}。
     * 复制类型、子类型和参数信息，并执行特定于 {@code MediaType} 的参数检查。
     *
     * @param mimeType MIME 类型
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MediaType(MimeType mimeType) {
        super(mimeType);
        getParameters().forEach(this::checkParameters);
    }

    @Override
    protected void checkParameters(String parameter, String value) {
        super.checkParameters(parameter, value);
        if (PARAM_QUALITY_FACTOR.equals(parameter)) {
            value = unquote(value);
            double d = Double.parseDouble(value);
            Assert.isTrue(
                    d >= 0D && d <= 1D,
                    "Invalid quality value \"" + value + "\": should be between 0.0 and 1.0"
            );
        }
    }

    /**
     * 返回品质因数，如 {@code q} 参数所示（如果有）。默认为 {@code 1.0}
     *
     * @return 作为双值的品质因数
     */
    public double getQualityValue() {
        String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);
        return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
    }

    /**
     * 指示此 {@code MediaType} 是否包含给定的媒体类型
     * <p>比如{@code text/*}包括{@code text/plain}和{@code tex/*html}，
     * {@code application/*+xml}包括{@code application/soap+xml}等，这种方法不是对称的。
     * <p>只需调用 {@link MimeType#includes(MimeType)} 但使用 {@code MediaType} 参数声明以实现二进制向后兼容性。
     *
     * @param other 要比较的参考媒体类型
     * @return {@code true} 如果此媒体类型包含给定的媒体类型； {@code false} 否则
     */
    public boolean includes(MediaType other) {
        return super.includes(other);
    }

    /**
     * 指示此 {@code MediaType} 是否与给定的媒体类型兼容
     * <p>例如，{@code text/*} 与 {@code text/plain}、{@code text/html} 兼容，反之亦然。实际上，此方法类似于 {@link #includes}，只是它<b><b> 是对称的。
     * <p>只需调用 {@link MimeType#isCompatibleWith(MimeType)} 但使用 {@code MediaType} 参数声明以实现二进制向后兼容性。
     *
     * @param other 要比较的参考媒体类型
     * @return {@code true} 如果此媒体类型与给定的媒体类型兼容； {@code false} 否则
     */
    public boolean isCompatibleWith(MediaType other) {
        return super.isCompatibleWith(other);
    }

    /**
     * 返回具有给定 {@code MediaType} 质量值的此实例的副本
     *
     * @return 如果给定的 MediaType 没有质量值，则为同一实例，否则为新实例
     */
    public MediaType copyQualityValue(MediaType mediaType) {
        if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
            return this;
        }
        Map<String, String> params = new LinkedHashMap<>(getParameters());
        params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));
        return new MediaType(this, params);
    }

    /**
     * 返回此实例的副本，并移除其质量值
     *
     * @return 如果媒体类型不包含质量值，则为同一实例，否则为新实例
     */
    public MediaType removeQualityValue() {
        if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
            return this;
        }
        Map<String, String> params = new LinkedHashMap<>(getParameters());
        params.remove(PARAM_QUALITY_FACTOR);
        return new MediaType(this, params);
    }

    /**
     * 将给定的字符串值解析为 {@code MediaType} 对象，
     * 此方法名称遵循“valueOf”命名约定（由 {@link org.clever.core.convert.ConversionService} 支持。
     *
     * @param value 要解析的字符串
     * @throws InvalidMediaTypeException 如果无法解析媒体类型值
     * @see #parseMediaType(String)
     */
    public static MediaType valueOf(String value) {
        return parseMediaType(value);
    }

    /**
     * 尝试将给定的字符串解析为单个 {@code MediaType}，解析失败返回 null
     *
     * @param mediaType 要解析的字符串
     * @return 媒体类型，解析失败返回 null
     */
    public static MediaType tryParseMediaType(String mediaType) {
        if (StringUtils.isBlank(mediaType)) {
            return null;
        }
        try {
            return parseMediaType(mediaType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将给定的字符串解析为单个 {@code MediaType}
     *
     * @param mediaType 要解析的字符串
     * @return 媒体类型
     * @throws InvalidMediaTypeException 如果无法解析媒体类型值
     */
    public static MediaType parseMediaType(String mediaType) {
        MimeType type;
        try {
            type = MimeTypeUtils.parseMimeType(mediaType);
        } catch (InvalidMimeTypeException ex) {
            throw new InvalidMediaTypeException(ex);
        }
        try {
            return new MediaType(type);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMediaTypeException(mediaType, ex.getMessage());
        }
    }

    /**
     * 将逗号分隔的字符串解析为 {@code MediaType} 对象列表。
     * <p>此方法可用于解析 Accept 或 Content-Type 标头。
     *
     * @param mediaTypes 要解析的字符串
     * @return 媒体类型列表
     * @throws InvalidMediaTypeException 如果无法解析媒体类型值
     */
    public static List<MediaType> parseMediaTypes(String mediaTypes) {
        if (!StringUtils.hasLength(mediaTypes)) {
            return Collections.emptyList();
        }
        // Avoid using java.util.stream.Stream in hot paths
        List<String> tokenizedTypes = MimeTypeUtils.tokenize(mediaTypes);
        List<MediaType> result = new ArrayList<>(tokenizedTypes.size());
        for (String type : tokenizedTypes) {
            if (StringUtils.hasText(type)) {
                result.add(parseMediaType(type));
            }
        }
        return result;
    }

    /**
     * 将给定的（可能）逗号分隔字符串列表解析为 {@code MediaType} 对象列表。
     * <p>此方法可用于解析 Accept 或 Content-Type 标头
     *
     * @param mediaTypes 要解析的字符串
     * @return 媒体类型列表
     * @throws InvalidMediaTypeException 如果无法解析媒体类型值
     */
    public static List<MediaType> parseMediaTypes(List<String> mediaTypes) {
        if (CollectionUtils.isEmpty(mediaTypes)) {
            return Collections.emptyList();
        } else if (mediaTypes.size() == 1) {
            return parseMediaTypes(mediaTypes.get(0));
        } else {
            List<MediaType> result = new ArrayList<>(8);
            for (String mediaType : mediaTypes) {
                result.addAll(parseMediaTypes(mediaType));
            }
            return result;
        }
    }

    /**
     * 将给定的 MIME 类型重新创建为媒体类型
     */
    public static List<MediaType> asMediaTypes(List<MimeType> mimeTypes) {
        List<MediaType> mediaTypes = new ArrayList<>(mimeTypes.size());
        for (MimeType mimeType : mimeTypes) {
            mediaTypes.add(MediaType.asMediaType(mimeType));
        }
        return mediaTypes;
    }

    /**
     * 将给定的 MIME 类型重新创建为媒体类型
     */
    public static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType) {
            return (MediaType) mimeType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }

    /**
     * 返回给定 {@code MediaType} 对象列表的字符串表示形式。
     * <p>此方法可用于 {@code Accept} 或 {@code Content-Type} 标头。
     *
     * @param mediaTypes 创建字符串表示的媒体类型
     * @return 字符串表示
     */
    public static String toString(Collection<MediaType> mediaTypes) {
        return MimeTypeUtils.toString(mediaTypes);
    }

    /**
     * 按特异性对给定的 {@code MediaType} 对象列表进行排序。
     * <p>给定两种媒体类型：
     * <ol>
     * <li>如果任一媒体类型具有 {@linkplain #isWildcardType() 通配符类型}，则不带通配符的媒体类型排在另一个之前。</li>
     * <li>如果这两种媒体类型具有不同的{@linkplain #getType() types}，那么它们被认为是相等的并保持它们当前的顺序。</li>
     * <li>如果任一媒体类型具有 {@linkplain #isWildcardSubtype() 通配符子类型}，则不带通配符的媒体类型排在另一个之前。</li>
     * <li>如果这两种媒体类型具有不同的 {@linkplain #getSubtype() 子类型}，那么它们将被视为相等并保持其当前顺序。</li>
     * <li>如果两种媒体类型具有不同的{@linkplain #getQualityValue() 质量值}，则具有最高质量值的媒体类型排在另一种之前。</li>
     * <li>如果两种媒体类型具有不同数量的{@linkplain #getParameter(String) parameters}，则具有最多参数的媒体类型排在另一种之前。</li>
     * </ol>
     * <p>例如：
     * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
     * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
     * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
     * <blockquote>audio/basic == text/html</blockquote>
     * <blockquote>audio/basic == audio/wave</blockquote>
     *
     * @param mediaTypes 要排序的媒体类型列表
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics and Content, section 5.3.2</a>
     */
    public static void sortBySpecificity(List<MediaType> mediaTypes) {
        Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            mediaTypes.sort(SPECIFICITY_COMPARATOR);
        }
    }

    /**
     * 按质量值对给定的 {@code MediaType} 对象列表进行排序
     * <p>给定两种媒体类型：
     * <ol>
     * <li>如果两种媒体类型具有不同的{@linkplain #getQualityValue() 质量值}，则具有最高质量值的媒体类型排在另一种之前。</li>
     * <li>如果任一媒体类型具有 {@linkplain #isWildcardType() 通配符类型}，则不带通配符的媒体类型排在另一个之前。</li>
     * <li>如果这两种媒体类型具有不同的{@linkplain #getType() types}，那么它们被认为是相等的并保持它们当前的顺序。</li>
     * <li>如果任一媒体类型具有 {@linkplain #isWildcardSubtype() 通配符子类型}，则不带通配符的媒体类型排在另一个之前。</li>
     * <li>如果这两种媒体类型具有不同的 {@linkplain #getSubtype() 子类型}，那么它们将被视为相等并保持其当前顺序。</li>
     * <li>如果两种媒体类型具有不同数量的{@linkplain #getParameter(String) parameters}，则具有最多参数的媒体类型排在另一种之前。</li>
     * </ol>
     *
     * @param mediaTypes 要排序的媒体类型列表
     * @see #getQualityValue()
     */
    public static void sortByQualityValue(List<MediaType> mediaTypes) {
        Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            mediaTypes.sort(QUALITY_VALUE_COMPARATOR);
        }
    }

    /**
     * 按特异性作为主要标准，质量值作为次要标准，对给定的 {@code MediaType} 对象列表进行排序
     *
     * @see MediaType#sortBySpecificity(List)
     * @see MediaType#sortByQualityValue(List)
     */
    public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
        Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            mediaTypes.sort(MediaType.SPECIFICITY_COMPARATOR.thenComparing(MediaType.QUALITY_VALUE_COMPARATOR));
        }
    }

    /**
     * {@link #sortByQualityValue(List)} 使用的比较器。
     */
    public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = (mediaType1, mediaType2) -> {
        double quality1 = mediaType1.getQualityValue();
        double quality2 = mediaType2.getQualityValue();
        int qualityComparison = Double.compare(quality2, quality1);
        if (qualityComparison != 0) {
            return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
        } else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) {  // */* < audio/*
            return 1;
        } else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) {  // audio/* > */*
            return -1;
        } else if (!mediaType1.getType().equals(mediaType2.getType())) {  // audio/basic == text/html
            return 0;
        } else {  // mediaType1.getType().equals(mediaType2.getType())
            if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) {  // audio/* < audio/basic
                return 1;
            } else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) {  // audio/basic > audio/*
                return -1;
            } else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) {  // audio/basic == audio/wave
                return 0;
            } else {
                int paramsSize1 = mediaType1.getParameters().size();
                int paramsSize2 = mediaType2.getParameters().size();
                return Integer.compare(paramsSize2, paramsSize1);  // audio/basic;level=1 < audio/basic
            }
        }
    };

    /**
     * {@link #sortBySpecificity(List)} 使用的比较器。
     */
    public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MediaType>() {
        @Override
        protected int compareParameters(MediaType mediaType1, MediaType mediaType2) {
            double quality1 = mediaType1.getQualityValue();
            double quality2 = mediaType2.getQualityValue();
            int qualityComparison = Double.compare(quality2, quality1);
            if (qualityComparison != 0) {
                return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
            }
            return super.compareParameters(mediaType1, mediaType2);
        }
    };
}

