package org.clever.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;


/**
 * 表示 MIME 类型，最初在 RFC 2046 中定义，随后用于其他 Internet 协议，包括 HTTP。
 * <p>由 {@linkplain #getType() type} 和 {@linkplain #getSubtype() subtype} 组成。
 * 还具有使用 {@link #valueOf(String)} 从 {@code String} 解析 MIME 类型值的功能。
 * 有关更多解析选项，请参阅 {@link MimeTypeUtils}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 13:26 <br/>
 *
 * @see MimeTypeUtils
 */
public class MimeType implements Comparable<MimeType>, Serializable {
    private static final long serialVersionUID = 4085923477777865903L;

    protected static final String WILDCARD_TYPE = "*";
    private static final String PARAM_CHARSET = "charset";
    private static final BitSet TOKEN;

    static {
        // variable names refer to RFC 2616, section 2.2
        BitSet ctl = new BitSet(128);
        for (int i = 0; i <= 31; i++) {
            ctl.set(i);
        }
        ctl.set(127);
        BitSet separators = new BitSet(128);
        separators.set('(');
        separators.set(')');
        separators.set('<');
        separators.set('>');
        separators.set('@');
        separators.set(',');
        separators.set(';');
        separators.set(':');
        separators.set('\\');
        separators.set('\"');
        separators.set('/');
        separators.set('[');
        separators.set(']');
        separators.set('?');
        separators.set('=');
        separators.set('{');
        separators.set('}');
        separators.set(' ');
        separators.set('\t');
        TOKEN = new BitSet(128);
        TOKEN.set(0, 128);
        TOKEN.andNot(ctl);
        TOKEN.andNot(separators);
    }

    private final String type;
    private final String subtype;
    private final Map<String, String> parameters;
    private transient Charset resolvedCharset;
    private volatile String toStringValue;

    /**
     * 为给定的主要类型创建一个新的 {@code MimeType}
     * <p>{@linkplain #getSubtype() 子类型}设置为<code>"&42;"<code>，参数为空
     *
     * @param type 主要类型
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MimeType(String type) {
        this(type, WILDCARD_TYPE);
    }

    /**
     * 为给定的主要类型和子类型创建一个新的 {@code MimeType}
     * <p>参数为空
     *
     * @param type    主要类型
     * @param subtype subtype
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MimeType(String type, String subtype) {
        this(type, subtype, Collections.emptyMap());
    }

    /**
     * 为给定的类型、子类型和字符集创建一个新的 {@code MimeType}
     *
     * @param type    主要类型
     * @param subtype subtype
     * @param charset 字符集
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MimeType(String type, String subtype, Charset charset) {
        this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charset.name()));
        this.resolvedCharset = charset;
    }

    /**
     * 复制给定 {@code MimeType} 的类型、子类型和参数的复制构造函数，并允许设置指定的字符集
     *
     * @param other   另一个 MimeType
     * @param charset 字符集
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MimeType(MimeType other, Charset charset) {
        this(other.getType(), other.getSubtype(), addCharsetParameter(charset, other.getParameters()));
        this.resolvedCharset = charset;
    }

    /**
     * 复制给定 {@code MimeType} 的类型和子类型的复制构造函数，并允许使用不同的参数
     *
     * @param other      另一个 MimeType
     * @param parameters 参数（可能是{@code null}）
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MimeType(MimeType other, Map<String, String> parameters) {
        this(other.getType(), other.getSubtype(), parameters);
    }

    /**
     * 为给定的类型、子类型和参数创建一个新的 {@code MimeType}
     *
     * @param type       主要类型
     * @param subtype    subtype
     * @param parameters 参数（可能是{@code null}）
     * @throws IllegalArgumentException 如果任何参数包含非法字符
     */
    public MimeType(String type, String subtype, Map<String, String> parameters) {
        Assert.hasLength(type, "'type' must not be empty");
        Assert.hasLength(subtype, "'subtype' must not be empty");
        checkToken(type);
        checkToken(subtype);
        this.type = type.toLowerCase(Locale.ENGLISH);
        this.subtype = subtype.toLowerCase(Locale.ENGLISH);
        if (!CollectionUtils.isEmpty(parameters)) {
            Map<String, String> map = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ENGLISH);
            parameters.forEach((parameter, value) -> {
                checkParameters(parameter, value);
                map.put(parameter, value);
            });
            this.parameters = Collections.unmodifiableMap(map);
        } else {
            this.parameters = Collections.emptyMap();
        }
    }

    /**
     * 复制给定 {@code MimeType} 的类型、子类型和参数的复制构造函数，跳过在其他构造函数中执行的检查
     *
     * @param other 另一个 MimeType
     */
    protected MimeType(MimeType other) {
        this.type = other.type;
        this.subtype = other.subtype;
        this.parameters = other.parameters;
        this.resolvedCharset = other.resolvedCharset;
        this.toStringValue = other.toStringValue;
    }

    /**
     * 检查给定的令牌字符串是否存在非法字符，如 RFC 2616 第 2.2 节中所定义
     *
     * @throws IllegalArgumentException 如果是非法字符
     * @see <a href="https://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
     */
    private void checkToken(String token) {
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!TOKEN.get(ch)) {
                throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + token + "\"");
            }
        }
    }

    protected void checkParameters(String parameter, String value) {
        Assert.hasLength(parameter, "'parameter' must not be empty");
        Assert.hasLength(value, "'value' must not be empty");
        checkToken(parameter);
        if (PARAM_CHARSET.equals(parameter)) {
            if (this.resolvedCharset == null) {
                this.resolvedCharset = Charset.forName(unquote(value));
            }
        } else if (!isQuotedString(value)) {
            checkToken(value);
        }
    }

    private boolean isQuotedString(String s) {
        if (s.length() < 2) {
            return false;
        } else {
            return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
        }
    }

    protected String unquote(String s) {
        return (isQuotedString(s) ? s.substring(1, s.length() - 1) : s);
    }

    /**
     * 指示 {@linkplain #getType() type} 是否为通配符 <code>&42;<code>
     */
    public boolean isWildcardType() {
        return WILDCARD_TYPE.equals(getType());
    }

    /**
     * 表示{@linkplain #getSubtype() subtype}是通配符<code>&42;<code>还是通配符后跟后缀（例如 <code>&42;+xml<code>）
     *
     * @return 子类型是否为通配符
     */
    public boolean isWildcardSubtype() {
        return WILDCARD_TYPE.equals(getSubtype()) || getSubtype().startsWith("*+");
    }

    /**
     * 指示此 MIME 类型是否具体，即类型和子类型是否都不是通配符 <code>&42;<code>
     *
     * @return 此 MIME 类型是否具体
     */
    public boolean isConcrete() {
        return !isWildcardType() && !isWildcardSubtype();
    }

    /**
     * 返回主要类型
     */
    public String getType() {
        return this.type;
    }

    /**
     * 返回子类型
     */
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * 返回 RFC 6839 中定义的子类型后缀
     */
    public String getSubtypeSuffix() {
        int suffixIndex = this.subtype.lastIndexOf('+');
        // noinspection ConstantConditions
        if (suffixIndex != -1 && this.subtype.length() > suffixIndex) {
            return this.subtype.substring(suffixIndex + 1);
        }
        return null;
    }

    /**
     * 返回字符集，如 {@code charset} 参数所示（如果有）
     *
     * @return 字符集，或 {@code null} 如果不可用
     */
    public Charset getCharset() {
        return this.resolvedCharset;
    }

    /**
     * 给定参数名称，返回通用参数值
     *
     * @param name 参数名称
     * @return 参数值，如果不存在则为 {@code null}
     */
    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    /**
     * 返回所有通用参数值。
     *
     * @return 一个只读地图（可能是空的，永远不会 {@code null}）
     */
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    /**
     * Indicate whether this MIME Type includes the given MIME Type.
     * <p>比如{@code text/*}包括{@code text/plain}和{@code text/html}，
     * {@code application/*+xml}包括{@code application/soap+xml}等，这种方式是不对称的
     *
     * @param other 要与之比较的参考 MIME 类型
     * @return {@code true} 如果此 MIME 类型包含给定的 MIME 类型； {@code false} 否则
     */
    public boolean includes(MimeType other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType()) {
            // */* includes anything
            return true;
        } else if (getType().equals(other.getType())) {
            if (getSubtype().equals(other.getSubtype())) {
                return true;
            }
            if (isWildcardSubtype()) {
                // Wildcard with suffix, e.g. application/*+xml
                int thisPlusIdx = getSubtype().lastIndexOf('+');
                if (thisPlusIdx == -1) {
                    return true;
                } else {
                    // application/*+xml includes application/soap+xml
                    int otherPlusIdx = other.getSubtype().lastIndexOf('+');
                    if (otherPlusIdx != -1) {
                        String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
                        String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
                        String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
                        // noinspection RedundantIfStatement
                        if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Indicate whether this MIME Type is compatible with the given MIME Type.
     * <p>例如，{@code text/*} 与 {@code text/plain}、{@code text/html} 兼容，反之亦然。
     * 实际上，此方法类似于 {@link #includes}，只是它是对称的。
     *
     * @param other 要与之比较的参考 MIME 类型
     * @return {@code true} 如果此 MIME 类型与给定的 MIME 类型兼容； {@code false} 否则
     */
    public boolean isCompatibleWith(MimeType other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType() || other.isWildcardType()) {
            return true;
        } else if (getType().equals(other.getType())) {
            if (getSubtype().equals(other.getSubtype())) {
                return true;
            }
            if (isWildcardSubtype() || other.isWildcardSubtype()) {
                String thisSuffix = getSubtypeSuffix();
                String otherSuffix = other.getSubtypeSuffix();
                if (getSubtype().equals(WILDCARD_TYPE) || other.getSubtype().equals(WILDCARD_TYPE)) {
                    return true;
                } else if (isWildcardSubtype() && thisSuffix != null) {
                    return (thisSuffix.equals(other.getSubtype()) || thisSuffix.equals(otherSuffix));
                } else if (other.isWildcardSubtype() && otherSuffix != null) {
                    return (this.getSubtype().equals(otherSuffix) || otherSuffix.equals(thisSuffix));
                }
            }
        }
        return false;
    }

    /**
     * 类似于 {@link #equals(Object)} 但仅基于类型和子类型，即忽略参数。
     *
     * @param other 要比较的另一种 MIME 类型
     * @return 两种 MIME 类型是否具有相同的类型和子类型
     */
    public boolean equalsTypeAndSubtype(MimeType other) {
        if (other == null) {
            return false;
        }
        return this.type.equalsIgnoreCase(other.type) && this.subtype.equalsIgnoreCase(other.subtype);
    }

    /**
     * 与依赖于 {@link MimeType#equals(Object)} 的 {@link Collection#contains(Object)} 不同，此方法仅检查类型和子类型，但忽略其他参数
     *
     * @param mimeTypes 执行检查的 MIME 类型列表
     * @return 列表是否包含给定的 mime 类型
     */
    public boolean isPresentIn(Collection<? extends MimeType> mimeTypes) {
        for (MimeType mimeType : mimeTypes) {
            if (mimeType.equalsTypeAndSubtype(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MimeType)) {
            return false;
        }
        MimeType otherType = (MimeType) other;
        return (this.type.equalsIgnoreCase(otherType.type) && this.subtype.equalsIgnoreCase(otherType.subtype) && parametersAreEqual(otherType));
    }

    /**
     * 确定此 {@code MimeType} 和提供的 {@code MimeType} 中的参数是否相等，对 {@link Charset Charsets} 执行不区分大小写的比较
     */
    private boolean parametersAreEqual(MimeType other) {
        if (this.parameters.size() != other.parameters.size()) {
            return false;
        }
        for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
            String key = entry.getKey();
            if (!other.parameters.containsKey(key)) {
                return false;
            }
            if (PARAM_CHARSET.equals(key)) {
                if (!ObjectUtils.nullSafeEquals(getCharset(), other.getCharset())) {
                    return false;
                }
            } else if (!ObjectUtils.nullSafeEquals(entry.getValue(), other.parameters.get(key))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.subtype.hashCode();
        result = 31 * result + this.parameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String value = this.toStringValue;
        if (value == null) {
            StringBuilder builder = new StringBuilder();
            appendTo(builder);
            value = builder.toString();
            this.toStringValue = value;
        }
        return value;
    }

    protected void appendTo(StringBuilder builder) {
        builder.append(this.type);
        builder.append('/');
        builder.append(this.subtype);
        appendTo(this.parameters, builder);
    }

    private void appendTo(Map<String, String> map, StringBuilder builder) {
        map.forEach((key, val) -> {
            builder.append(';');
            builder.append(key);
            builder.append('=');
            builder.append(val);
        });
    }

    /**
     * 按字母顺序将此 MIME 类型与另一个进行比较。
     *
     * @param other 要比较的 MIME 类型
     * @see MimeTypeUtils#sortBySpecificity(List)
     */
    @Override
    public int compareTo(MimeType other) {
        int comp = getType().compareToIgnoreCase(other.getType());
        if (comp != 0) {
            return comp;
        }
        comp = getSubtype().compareToIgnoreCase(other.getSubtype());
        if (comp != 0) {
            return comp;
        }
        comp = getParameters().size() - other.getParameters().size();
        if (comp != 0) {
            return comp;
        }
        TreeSet<String> thisAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        thisAttributes.addAll(getParameters().keySet());
        TreeSet<String> otherAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        otherAttributes.addAll(other.getParameters().keySet());
        Iterator<String> thisAttributesIterator = thisAttributes.iterator();
        Iterator<String> otherAttributesIterator = otherAttributes.iterator();
        while (thisAttributesIterator.hasNext()) {
            String thisAttribute = thisAttributesIterator.next();
            String otherAttribute = otherAttributesIterator.next();
            comp = thisAttribute.compareToIgnoreCase(otherAttribute);
            if (comp != 0) {
                return comp;
            }
            if (PARAM_CHARSET.equals(thisAttribute)) {
                Charset thisCharset = getCharset();
                Charset otherCharset = other.getCharset();
                if (thisCharset != otherCharset) {
                    if (thisCharset == null) {
                        return -1;
                    }
                    if (otherCharset == null) {
                        return 1;
                    }
                    comp = thisCharset.compareTo(otherCharset);
                    if (comp != 0) {
                        return comp;
                    }
                }
            } else {
                String thisValue = getParameters().get(thisAttribute);
                String otherValue = other.getParameters().get(otherAttribute);
                if (otherValue == null) {
                    otherValue = "";
                }
                comp = thisValue.compareTo(otherValue);
                if (comp != 0) {
                    return comp;
                }
            }
        }
        return 0;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization, just initialize state after deserialization.
        ois.defaultReadObject();
        // Initialize transient fields.
        String charsetName = getParameter(PARAM_CHARSET);
        if (charsetName != null) {
            this.resolvedCharset = Charset.forName(unquote(charsetName));
        }
    }

    /**
     * 将给定的字符串值解析为 {@code MimeType} 对象，此方法名称遵循“valueOf”命名约定（由 {@link org.clever.core.convert.ConversionService} 支持
     *
     * @see MimeTypeUtils#parseMimeType(String)
     */
    public static MimeType valueOf(String value) {
        return MimeTypeUtils.parseMimeType(value);
    }

    private static Map<String, String> addCharsetParameter(Charset charset, Map<String, String> parameters) {
        Map<String, String> map = new LinkedHashMap<>(parameters);
        map.put(PARAM_CHARSET, charset.name());
        return map;
    }

    /**
     * 比较器按特异性顺序对 {@link MimeType MimeTypes} 进行排序。
     *
     * @param <T> 此比较器可以比较的 MIME 类型的类型
     */
    public static class SpecificityComparator<T extends MimeType> implements Comparator<T> {
        @Override
        public int compare(T mimeType1, T mimeType2) {
            if (mimeType1.isWildcardType() && !mimeType2.isWildcardType()) {  // */* < audio/*
                return 1;
            } else if (mimeType2.isWildcardType() && !mimeType1.isWildcardType()) {  // audio/* > */*
                return -1;
            } else if (!mimeType1.getType().equals(mimeType2.getType())) {  // audio/basic == text/html
                return 0;
            } else {  // mediaType1.getType().equals(mediaType2.getType())
                if (mimeType1.isWildcardSubtype() && !mimeType2.isWildcardSubtype()) {  // audio/* < audio/basic
                    return 1;
                } else if (mimeType2.isWildcardSubtype() && !mimeType1.isWildcardSubtype()) {  // audio/basic > audio/*
                    return -1;
                } else if (!mimeType1.getSubtype().equals(mimeType2.getSubtype())) {  // audio/basic == audio/wave
                    return 0;
                } else {  // mediaType2.getSubtype().equals(mediaType2.getSubtype())
                    return compareParameters(mimeType1, mimeType2);
                }
            }
        }

        protected int compareParameters(T mimeType1, T mimeType2) {
            int paramsSize1 = mimeType1.getParameters().size();
            int paramsSize2 = mimeType2.getParameters().size();
            return Integer.compare(paramsSize2, paramsSize1);  // audio/basic;level=1 < audio/basic
        }
    }
}

