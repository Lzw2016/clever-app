//package org.clever.web.http;
//
//import org.clever.util.Assert;
//import org.clever.util.ObjectUtils;
//import org.clever.util.StreamUtils;
//
//import java.io.ByteArrayOutputStream;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeParseException;
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import static java.nio.charset.StandardCharsets.ISO_8859_1;
//import static java.nio.charset.StandardCharsets.UTF_8;
//import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
//
///**
// * RFC 6266中定义的内容处置类型和参数的表示
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/12/25 15:22 <br/>
// *
// * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
// */
//public final class ContentDisposition {
//    private final static Pattern BASE64_ENCODED_PATTERN = Pattern.compile("=\\?([0-9a-zA-Z-_]+)\\?B\\?([+/0-9a-zA-Z]+=*)\\?=");
//    private static final String INVALID_HEADER_FIELD_PARAMETER_FORMAT = "Invalid header field parameter format (as defined in RFC 5987)";
//
//    private final String type;
//    private final String name;
//    private final String filename;
//    private final Charset charset;
//    private final Long size;
//    private final ZonedDateTime creationDate;
//    private final ZonedDateTime modificationDate;
//    private final ZonedDateTime readDate;
//
//    /**
//     * 私有构造函数。请参阅此类中的静态工厂方法
//     */
//    private ContentDisposition(String type,
//                               String name,
//                               String filename,
//                               Charset charset,
//                               Long size,
//                               ZonedDateTime creationDate,
//                               ZonedDateTime modificationDate,
//                               ZonedDateTime readDate) {
//        this.type = type;
//        this.name = name;
//        this.filename = filename;
//        this.charset = charset;
//        this.size = size;
//        this.creationDate = creationDate;
//        this.modificationDate = modificationDate;
//        this.readDate = readDate;
//    }
//
//    /**
//     * 返回 {@link #getType() type} 是否为 {@literal "attachment"}
//     */
//    public boolean isAttachment() {
//        return (this.type != null && this.type.equalsIgnoreCase("attachment"));
//    }
//
//    /**
//     * 返回 {@link #getType() type} 是否为 {@literal "form-data"}。
//     */
//    public boolean isFormData() {
//        return (this.type != null && this.type.equalsIgnoreCase("form-data"));
//    }
//
//    /**
//     * 返回 {@link #getType() type} 是否为 {@literal "inline"}
//     */
//    public boolean isInline() {
//        return (this.type != null && this.type.equalsIgnoreCase("inline"));
//    }
//
//    /**
//     * 返回处置类型
//     *
//     * @see #isAttachment()
//     * @see #isFormData()
//     * @see #isInline()
//     */
//    public String getType() {
//        return this.type;
//    }
//
//    /**
//     * 返回 {@literal name} 参数的值，如果未定义则返回 {@code null}
//     */
//    public String getName() {
//        return this.name;
//    }
//
//    /**
//     * 返回 {@literal filename} 参数的值，可能从基于 RFC 2047 的 BASE64 编码解码，
//     * 或 {@literal filename} 参数的值，可能按照 RFC 5987 中的定义解码。
//     */
//    public String getFilename() {
//        return this.filename;
//    }
//
//    /**
//     * 返回 {@literal filename} 参数中定义的字符集，如果未定义则返回 {@code null}。
//     */
//    public Charset getCharset() {
//        return this.charset;
//    }
//
//    /**
//     * 返回 {@literal size} 参数的值，如果未定义则返回 {@code null}。
//     *
//     * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>,将在未来的版本中删除。
//     */
//    @Deprecated
//    public Long getSize() {
//        return this.size;
//    }
//
//    /**
//     * 返回 {@literal creation-date} 参数的值，如果未定义则返回 {@code null}。
//     *
//     * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>,将在未来的版本中删除。
//     */
//    @Deprecated
//    public ZonedDateTime getCreationDate() {
//        return this.creationDate;
//    }
//
//    /**
//     * 返回 {@literal modification-date} 参数的值，如果未定义则返回 {@code null}。
//     *
//     * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>,将在未来的版本中删除。
//     */
//    @Deprecated
//    public ZonedDateTime getModificationDate() {
//        return this.modificationDate;
//    }
//
//    /**
//     * 返回 {@literal read-date} 参数的值，如果未定义则返回 {@code null}
//     *
//     * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>,将在未来的版本中删除。
//     */
//    @Deprecated
//    public ZonedDateTime getReadDate() {
//        return this.readDate;
//    }
//
//    @Override
//    public boolean equals(Object other) {
//        if (this == other) {
//            return true;
//        }
//        if (!(other instanceof ContentDisposition)) {
//            return false;
//        }
//        ContentDisposition otherCd = (ContentDisposition) other;
//        return (ObjectUtils.nullSafeEquals(this.type, otherCd.type) &&
//                ObjectUtils.nullSafeEquals(this.name, otherCd.name) &&
//                ObjectUtils.nullSafeEquals(this.filename, otherCd.filename) &&
//                ObjectUtils.nullSafeEquals(this.charset, otherCd.charset) &&
//                ObjectUtils.nullSafeEquals(this.size, otherCd.size) &&
//                ObjectUtils.nullSafeEquals(this.creationDate, otherCd.creationDate) &&
//                ObjectUtils.nullSafeEquals(this.modificationDate, otherCd.modificationDate) &&
//                ObjectUtils.nullSafeEquals(this.readDate, otherCd.readDate));
//    }
//
//    @Override
//    public int hashCode() {
//        int result = ObjectUtils.nullSafeHashCode(this.type);
//        result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
//        result = 31 * result + ObjectUtils.nullSafeHashCode(this.filename);
//        result = 31 * result + ObjectUtils.nullSafeHashCode(this.charset);
//        result = 31 * result + ObjectUtils.nullSafeHashCode(this.size);
//        result = 31 * result + (this.creationDate != null ? this.creationDate.hashCode() : 0);
//        result = 31 * result + (this.modificationDate != null ? this.modificationDate.hashCode() : 0);
//        result = 31 * result + (this.readDate != null ? this.readDate.hashCode() : 0);
//        return result;
//    }
//
//    /**
//     * 返回 RFC 6266 中定义的此内容配置的标头值。
//     *
//     * @see #parse(String)
//     */
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        if (this.type != null) {
//            sb.append(this.type);
//        }
//        if (this.name != null) {
//            sb.append("; name=\"");
//            sb.append(this.name).append('\"');
//        }
//        if (this.filename != null) {
//            if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
//                sb.append("; filename=\"");
//                sb.append(escapeQuotationsInFilename(this.filename)).append('\"');
//            } else {
//                sb.append("; filename*=");
//                sb.append(encodeFilename(this.filename, this.charset));
//            }
//        }
//        if (this.size != null) {
//            sb.append("; size=");
//            sb.append(this.size);
//        }
//        if (this.creationDate != null) {
//            sb.append("; creation-date=\"");
//            sb.append(RFC_1123_DATE_TIME.format(this.creationDate));
//            sb.append('\"');
//        }
//        if (this.modificationDate != null) {
//            sb.append("; modification-date=\"");
//            sb.append(RFC_1123_DATE_TIME.format(this.modificationDate));
//            sb.append('\"');
//        }
//        if (this.readDate != null) {
//            sb.append("; read-date=\"");
//            sb.append(RFC_1123_DATE_TIME.format(this.readDate));
//            sb.append('\"');
//        }
//        return sb.toString();
//    }
//
//    /**
//     * 为 {@literal "attachment"} 类型的 {@code ContentDisposition} 返回一个构建器。
//     */
//    public static Builder attachment() {
//        return builder("attachment");
//    }
//
//    /**
//     * 为 {@literal "form-data"} 类型的 {@code ContentDisposition} 返回一个构建器。
//     */
//    public static Builder formData() {
//        return builder("form-data");
//    }
//
//    /**
//     * 为 {@literal "inline"} 类型的 {@code ContentDisposition} 返回一个构建器。
//     */
//    public static Builder inline() {
//        return builder("inline");
//    }
//
//    /**
//     * 返回 {@code ContentDisposition} 的构建器
//     *
//     * @param type 处置类型，例如 {@literal inline}、{@literal attachment} 或 {@literal form-data}
//     * @return the builder
//     */
//    public static Builder builder(String type) {
//        return new BuilderImpl(type);
//    }
//
//    /**
//     * 返回空内容处置。
//     */
//    public static ContentDisposition empty() {
//        return new ContentDisposition("", null, null, null, null, null, null, null);
//    }
//
//    /**
//     * 解析 RFC 2183 中定义的 {@literal Content-Disposition} 标头值。
//     *
//     * @param contentDisposition {@literal Content-Disposition} 标头值
//     * @return 解析的内容处置
//     * @see #toString()
//     */
//    public static ContentDisposition parse(String contentDisposition) {
//        List<String> parts = tokenize(contentDisposition);
//        String type = parts.get(0);
//        String name = null;
//        String filename = null;
//        Charset charset = null;
//        Long size = null;
//        ZonedDateTime creationDate = null;
//        ZonedDateTime modificationDate = null;
//        ZonedDateTime readDate = null;
//        for (int i = 1; i < parts.size(); i++) {
//            String part = parts.get(i);
//            int eqIndex = part.indexOf('=');
//            if (eqIndex != -1) {
//                String attribute = part.substring(0, eqIndex);
//                String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ? part.substring(eqIndex + 2, part.length() - 1) : part.substring(eqIndex + 1));
//                if (attribute.equals("name")) {
//                    name = value;
//                } else if (attribute.equals("filename*")) {
//                    int idx1 = value.indexOf('\'');
//                    int idx2 = value.indexOf('\'', idx1 + 1);
//                    if (idx1 != -1 && idx2 != -1) {
//                        charset = Charset.forName(value.substring(0, idx1).trim());
//                        Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
//                                "Charset should be UTF-8 or ISO-8859-1");
//                        filename = decodeFilename(value.substring(idx2 + 1), charset);
//                    } else {
//                        // US ASCII
//                        filename = decodeFilename(value, StandardCharsets.US_ASCII);
//                    }
//                } else if (attribute.equals("filename") && (filename == null)) {
//                    if (value.startsWith("=?")) {
//                        Matcher matcher = BASE64_ENCODED_PATTERN.matcher(value);
//                        if (matcher.find()) {
//                            String match1 = matcher.group(1);
//                            String match2 = matcher.group(2);
//                            filename = new String(Base64.getDecoder().decode(match2), Charset.forName(match1));
//                        } else {
//                            filename = value;
//                        }
//                    } else {
//                        filename = value;
//                    }
//                } else if (attribute.equals("size")) {
//                    size = Long.parseLong(value);
//                } else if (attribute.equals("creation-date")) {
//                    try {
//                        creationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
//                    } catch (DateTimeParseException ex) {
//                        // ignore
//                    }
//                } else if (attribute.equals("modification-date")) {
//                    try {
//                        modificationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
//                    } catch (DateTimeParseException ex) {
//                        // ignore
//                    }
//                } else if (attribute.equals("read-date")) {
//                    try {
//                        readDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
//                    } catch (DateTimeParseException ex) {
//                        // ignore
//                    }
//                }
//            } else {
//                throw new IllegalArgumentException("Invalid content disposition format");
//            }
//        }
//        return new ContentDisposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
//    }
//
//    private static List<String> tokenize(String headerValue) {
//        int index = headerValue.indexOf(';');
//        String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
//        if (type.isEmpty()) {
//            throw new IllegalArgumentException("Content-Disposition header must not be empty");
//        }
//        List<String> parts = new ArrayList<>();
//        parts.add(type);
//        if (index >= 0) {
//            do {
//                int nextIndex = index + 1;
//                boolean quoted = false;
//                boolean escaped = false;
//                while (nextIndex < headerValue.length()) {
//                    char ch = headerValue.charAt(nextIndex);
//                    if (ch == ';') {
//                        if (!quoted) {
//                            break;
//                        }
//                    } else if (!escaped && ch == '"') {
//                        quoted = !quoted;
//                    }
//                    escaped = (!escaped && ch == '\\');
//                    nextIndex++;
//                }
//                String part = headerValue.substring(index + 1, nextIndex).trim();
//                if (!part.isEmpty()) {
//                    parts.add(part);
//                }
//                index = nextIndex;
//            }
//            while (index < headerValue.length());
//        }
//        return parts;
//    }
//
//    /**
//     * 按照RFC 5987中的描述解码给定的报头字段参数。
//     * <p>仅支持US-ASCII、UTF-8和ISO-8859-1字符集。
//     *
//     * @param filename 文件名
//     * @param charset  文件名的字符集
//     * @return 编码的头字段参数
//     * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
//     */
//    private static String decodeFilename(String filename, Charset charset) {
//        Assert.notNull(filename, "'input' String` should not be null");
//        Assert.notNull(charset, "'charset' should not be null");
//        byte[] value = filename.getBytes(charset);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        int index = 0;
//        while (index < value.length) {
//            byte b = value[index];
//            if (isRFC5987AttrChar(b)) {
//                baos.write((char) b);
//                index++;
//            } else if (b == '%' && index < value.length - 2) {
//                char[] array = new char[]{(char) value[index + 1], (char) value[index + 2]};
//                try {
//                    baos.write(Integer.parseInt(String.valueOf(array), 16));
//                } catch (NumberFormatException ex) {
//                    throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT, ex);
//                }
//                index += 3;
//            } else {
//                throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT);
//            }
//        }
//        return StreamUtils.copyToString(baos, charset);
//    }
//
//    private static boolean isRFC5987AttrChar(byte c) {
//        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
//                c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
//                c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
//    }
//
//    private static String escapeQuotationsInFilename(String filename) {
//        if (filename.indexOf('"') == -1 && filename.indexOf('\\') == -1) {
//            return filename;
//        }
//        boolean escaped = false;
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < filename.length(); i++) {
//            char c = filename.charAt(i);
//            if (!escaped && c == '"') {
//                sb.append("\\\"");
//            } else {
//                sb.append(c);
//            }
//            escaped = (!escaped && c == '\\');
//        }
//        // Remove backslash at the end.
//        if (escaped) {
//            sb.deleteCharAt(sb.length() - 1);
//        }
//        return sb.toString();
//    }
//
//    /**
//     * 按照RFC 5987中的描述对给定的头字段参数进行编码。
//     *
//     * @param input   标头字段参数
//     * @param charset 头字段参数字符串的字符集，仅支持US-ASCII、UTF-8和ISO-8859-1字符集
//     * @return 编码的头字段参数
//     * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
//     */
//    private static String encodeFilename(String input, Charset charset) {
//        Assert.notNull(input, "`input` is required");
//        Assert.notNull(charset, "`charset` is required");
//        Assert.isTrue(!StandardCharsets.US_ASCII.equals(charset), "ASCII does not require encoding");
//        Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Only UTF-8 and ISO-8859-1 supported.");
//        byte[] source = input.getBytes(charset);
//        int len = source.length;
//        StringBuilder sb = new StringBuilder(len << 1);
//        sb.append(charset.name());
//        sb.append("''");
//        for (byte b : source) {
//            if (isRFC5987AttrChar(b)) {
//                sb.append((char) b);
//            } else {
//                sb.append('%');
//                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
//                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
//                sb.append(hex1);
//                sb.append(hex2);
//            }
//        }
//        return sb.toString();
//    }
//
//    /**
//     * {@code ContentDisposition} 的可变构建器。
//     */
//    public interface Builder {
//        /**
//         * 设置 {@literal name} 参数的值。
//         */
//        Builder name(String name);
//
//        /**
//         * 设置 {@literal filename} 参数的值。
//         * 给定的文件名将被格式化为引号字符串，如 RFC 2616 第 2.2 节中所定义，
//         * 文件名值中的任何引号字符都将使用反斜杠进行转义，例如{@code "foo\"bar.txt"} 变成 {@code "foo\\\"bar.txt"}。
//         */
//        Builder filename(String filename);
//
//        /**
//         * 设置将按照 RFC 5987 中定义的方式进行编码的 {@code filename} 的值。仅支持 US-ASCII、UTF-8 和 ISO-8859-1 字符集。
//         * <p><strong>注意：</strong> 不要将此用于 {@code "multipartform-data"} 请求，因为
//         * <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, 第 4.2 节</a>
//         * 以及 RFC 5987 提到它不适用于多部分请求。
//         */
//        Builder filename(String filename, Charset charset);
//
//        /**
//         * 设置 {@literal size} 参数的值。
//         *
//         * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>, 将在未来的版本中删除。
//         */
//        @Deprecated
//        Builder size(Long size);
//
//        /**
//         * 设置 {@literal creation-date} 参数的值。
//         *
//         * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>, 将在未来的版本中删除。
//         */
//        @Deprecated
//        Builder creationDate(ZonedDateTime creationDate);
//
//        /**
//         * 设置 {@literal modification-date} 参数的值。
//         *
//         * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>, 将在未来的版本中删除。
//         */
//        @Deprecated
//        Builder modificationDate(ZonedDateTime modificationDate);
//
//        /**
//         * 设置 {@literal read-date} 参数的值。
//         *
//         * @deprecated <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, 附录 B</a>, 将在未来的版本中删除。
//         */
//        @Deprecated
//        Builder readDate(ZonedDateTime readDate);
//
//        /**
//         * 构建内容配置。
//         */
//        ContentDisposition build();
//    }
//
//    private static class BuilderImpl implements Builder {
//        private final String type;
//        private String name;
//        private String filename;
//        private Charset charset;
//        private Long size;
//        private ZonedDateTime creationDate;
//        private ZonedDateTime modificationDate;
//        private ZonedDateTime readDate;
//
//        public BuilderImpl(String type) {
//            Assert.hasText(type, "'type' must not be not empty");
//            this.type = type;
//        }
//
//        @Override
//        public Builder name(String name) {
//            this.name = name;
//            return this;
//        }
//
//        @Override
//        public Builder filename(String filename) {
//            Assert.hasText(filename, "No filename");
//            this.filename = filename;
//            return this;
//        }
//
//        @Override
//        public Builder filename(String filename, Charset charset) {
//            Assert.hasText(filename, "No filename");
//            this.filename = filename;
//            this.charset = charset;
//            return this;
//        }
//
//        @Override
//        public Builder size(Long size) {
//            this.size = size;
//            return this;
//        }
//
//        @Override
//        public Builder creationDate(ZonedDateTime creationDate) {
//            this.creationDate = creationDate;
//            return this;
//        }
//
//        @Override
//        public Builder modificationDate(ZonedDateTime modificationDate) {
//            this.modificationDate = modificationDate;
//            return this;
//        }
//
//        @Override
//        public Builder readDate(ZonedDateTime readDate) {
//            this.readDate = readDate;
//            return this;
//        }
//
//        @Override
//        public ContentDisposition build() {
//            return new ContentDisposition(
//                    this.type, this.name, this.filename, this.charset,
//                    this.size, this.creationDate, this.modificationDate, this.readDate
//            );
//        }
//    }
//}
