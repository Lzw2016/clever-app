package org.clever.web.utils;

import org.clever.util.*;
import org.clever.web.utils.HierarchicalUriComponents.PathComponent;
import org.clever.web.utils.UriComponents.UriTemplateVariables;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link UriComponents} 的生成器
 *
 * <p>典型用法包括：
 * <ol>
 * <li>使用一个静态工厂方法（例如{@link #fromPath(String)} 或 {@link #fromUri(URI)}）创建{@code UriComponentsBuilder}</li>
 * <li>通过相应的方法设置各种URI组件：{@link #scheme(String)}, {@link #userInfo(String)},
 * {@link #host(String)}, {@link #port(int)}, {@link #path(String)}, {@link #pathSegment(String...)},
 * {@link #queryParam(String, Object...)}, and {@link #fragment(String)}</li>
 * <li>使用{@link #build()}方法生成{@link UriComponents}实例。</li>
 * </ol>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 16:06 <br/>
 *
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 */
public class UriComponentsBuilder implements UriBuilder, Cloneable {
    private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");
    private static final String SCHEME_PATTERN = "([^:/?#]+):";
    private static final String HTTP_PATTERN = "(?i)(http|https):";
    private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";
    private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";
    private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}:.]*[%\\p{Alnum}]*]";
    private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";
    private static final String PORT_PATTERN = "(\\{[^}]+\\}?|[^/?#]*)";
    private static final String PATH_PATTERN = "([^?#]*)";
    private static final String QUERY_PATTERN = "([^#]*)";
    private static final String LAST_PATTERN = "(.*)";
    // Regex patterns that matches URIs. See RFC 3986, appendix B
    private static final Pattern URI_PATTERN = Pattern.compile(
            "^(" + SCHEME_PATTERN + ")?"
                    + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN
                    + "(:" + PORT_PATTERN + ")?" + ")?" + PATH_PATTERN
                    + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?"
    );
    private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
            "^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?"
                    + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?"
                    + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?"
                    + "(#" + LAST_PATTERN + ")?"
    );
    private static final String FORWARDED_VALUE = "\"?([^;,\"]+)\"?";
    // private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("(?i:host)=" + FORWARDED_VALUE);
    // private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("(?i:proto)=" + FORWARDED_VALUE);
    // private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=" + FORWARDED_VALUE);
    private static final Object[] EMPTY_VALUES = new Object[0];

    private String scheme;
    private String ssp;
    private String userInfo;
    private String host;
    private String port;
    private CompositePathComponentBuilder pathBuilder;
    private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    private String fragment;
    private final Map<String, Object> uriVariables = new HashMap<>(4);
    private boolean encodeTemplate;
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * 默认构造函数。受保护以防止直接实例化。
     *
     * @see #newInstance()
     * @see #fromPath(String)
     * @see #fromUri(URI)
     */
    protected UriComponentsBuilder() {
        this.pathBuilder = new CompositePathComponentBuilder();
    }

    /**
     * 创建给定UriComponentsBuilder的深度副本。
     *
     * @param other the other builder to copy from
     */
    protected UriComponentsBuilder(UriComponentsBuilder other) {
        this.scheme = other.scheme;
        this.ssp = other.ssp;
        this.userInfo = other.userInfo;
        this.host = other.host;
        this.port = other.port;
        this.pathBuilder = other.pathBuilder.cloneBuilder();
        this.uriVariables.putAll(other.uriVariables);
        this.queryParams.addAll(other.queryParams);
        this.fragment = other.fragment;
        this.encodeTemplate = other.encodeTemplate;
        this.charset = other.charset;
    }


    // Factory methods

    /**
     * 创建一个新的空生成器。
     *
     * @return new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder newInstance() {
        return new UriComponentsBuilder();
    }

    /**
     * 创建使用给定路径初始化的生成器。
     *
     * @param path 要初始化的路径
     * @return new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder fromPath(String path) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.path(path);
        return builder;
    }

    /**
     * 创建从给定的{@code URI}初始化的生成器。
     * <p><strong>注：</strong> 生成的生成器中的组件将采用完全编码(raw)形式，
     * 进一步的更改还必须提供完全编码的值，例如通过{@link UriUtils}中的方法。
     * 此外，请使用值为“true”的{@link #build(boolean)}来构建{@link UriComponents}实例，以指示组件已编码。
     *
     * @param uri 要初始化的URI
     * @return new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder fromUri(URI uri) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.uri(uri);
        return builder;
    }

    /**
     * 创建使用给定URI字符串初始化的生成器。
     * <p><strong>注：</strong> 保留字符的存在会阻止正确解析URI字符串。
     * 例如，如果查询参数包含{@code '='}或{@code '&'}个字符，则无法明确分析查询字符串。
     * 此类值应替换为URI变量，以实现正确的解析：
     * <pre class="code">
     * String uriString = &quot;/hotels/42?filter={value}&quot;;
     * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
     * </pre>
     *
     * @param uri 要初始化的URI字符串
     * @return new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder fromUriString(String uri) {
        Assert.notNull(uri, "URI must not be null");
        Matcher matcher = URI_PATTERN.matcher(uri);
        if (matcher.matches()) {
            UriComponentsBuilder builder = new UriComponentsBuilder();
            String scheme = matcher.group(2);
            String userInfo = matcher.group(5);
            String host = matcher.group(6);
            String port = matcher.group(8);
            String path = matcher.group(9);
            String query = matcher.group(11);
            String fragment = matcher.group(13);
            boolean opaque = false;
            if (StringUtils.hasLength(scheme)) {
                String rest = uri.substring(scheme.length());
                if (!rest.startsWith(":/")) {
                    opaque = true;
                }
            }
            builder.scheme(scheme);
            if (opaque) {
                String ssp = uri.substring(scheme.length() + 1);
                if (StringUtils.hasLength(fragment)) {
                    ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
                }
                builder.schemeSpecificPart(ssp);
            } else {
                if (StringUtils.hasLength(scheme) && scheme.startsWith("http") && !StringUtils.hasLength(host)) {
                    throw new IllegalArgumentException("[" + uri + "] is not a valid HTTP URL");
                }
                builder.userInfo(userInfo);
                builder.host(host);
                if (StringUtils.hasLength(port)) {
                    builder.port(port);
                }
                builder.path(path);
                builder.query(query);
            }
            if (StringUtils.hasText(fragment)) {
                builder.fragment(fragment);
            }
            return builder;
        } else {
            throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
        }
    }

    /**
     * 从给定的HTTP URL字符串创建URI组件生成器。
     * <p><strong>注：</strong> 保留字符的存在会阻止正确解析URI字符串。
     * 例如，如果查询参数包含{@code '='}或{@code '&'}个字符，则无法明确分析查询字符串。
     * 此类值应替换为URI变量，以实现正确的解析：
     * <pre class="code">
     * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
     * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
     * </pre>
     *
     * @param httpUrl 源URI
     * @return URI的URI组件
     */
    public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
        Assert.notNull(httpUrl, "HTTP URL must not be null");
        Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
        if (matcher.matches()) {
            UriComponentsBuilder builder = new UriComponentsBuilder();
            String scheme = matcher.group(1);
            builder.scheme(scheme != null ? scheme.toLowerCase() : null);
            builder.userInfo(matcher.group(4));
            String host = matcher.group(5);
            if (StringUtils.hasLength(scheme) && !StringUtils.hasLength(host)) {
                throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
            }
            builder.host(host);
            String port = matcher.group(7);
            if (StringUtils.hasLength(port)) {
                builder.port(port);
            }
            builder.path(matcher.group(8));
            builder.query(matcher.group(10));
            String fragment = matcher.group(12);
            if (StringUtils.hasText(fragment)) {
                builder.fragment(fragment);
            }
            return builder;
        } else {
            throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
        }
    }

    /**
     * 通过解析HTTP请求的“Origin”头来创建实例
     *
     * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
     */
    public static UriComponentsBuilder fromOriginHeader(String origin) {
        Matcher matcher = URI_PATTERN.matcher(origin);
        if (matcher.matches()) {
            UriComponentsBuilder builder = new UriComponentsBuilder();
            String scheme = matcher.group(2);
            String host = matcher.group(6);
            String port = matcher.group(8);
            if (StringUtils.hasLength(scheme)) {
                builder.scheme(scheme);
            }
            builder.host(host);
            if (StringUtils.hasLength(port)) {
                builder.port(port);
            }
            return builder;
        } else {
            throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
        }
    }

    // Encode methods

    /**
     * 请求在构建时对URI模板进行预编码，并在展开时对URI变量进行单独编码。
     * <p>与{@link UriComponents#encode()}相比，此方法对URI模板具有相同的效果，
     * 即通过用转义的八位字节替换非ASCII和非法（在URI组件类型内）字符来编码每个URI组件。
     * 然而，URI变量的编码更为严格，也通过转义具有保留含义的字符。
     * <p>在大多数情况下，此方法更可能产生预期结果，因为将URI变量视为要完全编码的不透明数据，
     * 而｛{@link UriComponents#encode()}在有意扩展包含保留字符的URI变量时非常有用。
     * <p>例如“；”在路径上是合法的，但具有保留的含义。此方法将URI变量中的“；”替换为“%3B”，但不在URI模板中。
     * 相反，{@link UriComponents#encode()}从不替换“；”，因为它是路径中的合法字符。
     * <p>当根本不扩展URI变量时，最好使用{@link UriComponents#encode()}，因为这也会对任何偶然看起来像URI变量的内容进行编码。
     */
    public final UriComponentsBuilder encode() {
        return encode(StandardCharsets.UTF_8);
    }

    /**
     * {@link #encode()} 的变体，其字符集不是“UTF-8”。
     *
     * @param charset 用于编码的字符集
     */
    public UriComponentsBuilder encode(Charset charset) {
        this.encodeTemplate = true;
        this.charset = charset;
        return this;
    }

    // Build methods

    /**
     * 使用此生成器中包含的各种组件生成 {@code UriComponents} 实例。
     *
     * @return URI组件
     */
    public UriComponents build() {
        return build(false);
    }

    /**
     * {@link #build()} 的变体，用于在组件已完全编码时创建{@link UriComponents}实例。
     * 例如，如果生成器是通过{@link UriComponentsBuilder#fromUri(URI)}创建的，则这非常有用。
     *
     * @param encoded 此生成器中的组件是否已编码
     * @return URI组件
     * @throws IllegalArgumentException 如果任何组件包含本应编码的非法字符
     */
    public UriComponents build(boolean encoded) {
        return buildInternal(
                encoded ?
                        EncodingHint.FULLY_ENCODED :
                        (this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE)
        );
    }

    private UriComponents buildInternal(EncodingHint hint) {
        UriComponents result;
        if (this.ssp != null) {
            result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
        } else {
            HierarchicalUriComponents uric = new HierarchicalUriComponents(
                    this.scheme, this.fragment, this.userInfo, this.host, this.port,
                    this.pathBuilder.build(), this.queryParams, hint == EncodingHint.FULLY_ENCODED
            );
            result = (hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric);
        }
        if (!this.uriVariables.isEmpty()) {
            result = result.expand(name -> this.uriVariables.getOrDefault(name, UriTemplateVariables.SKIP_VALUE));
        }
        return result;
    }

    /**
     * 构建一个{@code UriComponents}实例，并用映射中的值替换URI模板变量。
     * 这是一个快捷方法，它结合了对{@link #build()}和{@link UriComponents#expand(Map)}的调用。
     *
     * @param uriVariables URI变量的映射
     * @return 具有扩展值的URI组件
     */
    public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
        return build().expand(uriVariables);
    }

    /**
     * 构建一个{@code UriComponents}实例，并用数组中的值替换URI模板变量。
     * 这是一个快捷方法，它组合了对{@link #build()}和{@link UriComponents#expand(Object...)}的调用。
     *
     * @param uriVariableValues URI变量值
     * @return 具有扩展值的URI组件
     */
    public UriComponents buildAndExpand(Object... uriVariableValues) {
        return build().expand(uriVariableValues);
    }

    @Override
    public URI build(Object... uriVariables) {
        return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
    }

    @Override
    public URI build(Map<String, ?> uriVariables) {
        return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
    }

    /**
     * 生成URI字符串。
     * <p>实际上，构建、编码和返回字符串表示的快捷方式：
     * <pre class="code">
     * String uri = builder.build().encode().toUriString()
     * </pre>
     * <p>然而，如果已提供{@link #uriVariables(Map) URI变量}，
     * 则URI模板与URI变量分开进行预编码（有关详细信息，请参阅{@link #encode()}），即等同于：
     * <pre>
     * String uri = builder.encode().build().toUriString()
     * </pre>
     *
     * @see UriComponents#toUriString()
     */
    public String toUriString() {
        return (this.uriVariables.isEmpty() ? build().encode().toUriString() : buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString());
    }

    // Instance methods

    /**
     * 从给定URI的组件初始化此生成器的组件。
     *
     * @param uri URI
     * @return UriComponentsBuilder
     */
    public UriComponentsBuilder uri(URI uri) {
        Assert.notNull(uri, "URI must not be null");
        this.scheme = uri.getScheme();
        if (uri.isOpaque()) {
            this.ssp = uri.getRawSchemeSpecificPart();
            resetHierarchicalComponents();
        } else {
            if (uri.getRawUserInfo() != null) {
                this.userInfo = uri.getRawUserInfo();
            }
            if (uri.getHost() != null) {
                this.host = uri.getHost();
            }
            if (uri.getPort() != -1) {
                this.port = String.valueOf(uri.getPort());
            }
            if (StringUtils.hasLength(uri.getRawPath())) {
                this.pathBuilder = new CompositePathComponentBuilder();
                this.pathBuilder.addPath(uri.getRawPath());
            }
            if (StringUtils.hasLength(uri.getRawQuery())) {
                this.queryParams.clear();
                query(uri.getRawQuery());
            }
            resetSchemeSpecificPart();
        }
        if (uri.getRawFragment() != null) {
            this.fragment = uri.getRawFragment();
        }
        return this;
    }

    /**
     * 根据给定{@link UriComponents}实例的值设置或附加此生成器的各个URI组件。
     * <p>对于每个组件的语义（即set vs append），请检查该类上的生成器方法。
     * 例如，{@link #host(String)}设置，而{@link #path(String)}追加。
     *
     * @param uriComponents 要从中复制的UriComponents
     * @return UriComponentsBuilder
     */
    public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
        Assert.notNull(uriComponents, "UriComponents must not be null");
        uriComponents.copyToUriComponentsBuilder(this);
        return this;
    }

    @Override
    public UriComponentsBuilder scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * 设置URI方案特定部分。调用此方法时，将覆盖 {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
     * {@linkplain #port(int) port}, {@linkplain #path(String) path}, and  {@link #query(String) query}
     *
     * @param ssp URI方案特定部分可以包含URI模板参数
     * @return UriComponentsBuilder
     */
    public UriComponentsBuilder schemeSpecificPart(String ssp) {
        this.ssp = ssp;
        resetHierarchicalComponents();
        return this;
    }

    @Override
    public UriComponentsBuilder userInfo(String userInfo) {
        this.userInfo = userInfo;
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder host(String host) {
        this.host = host;
        if (host != null) {
            resetSchemeSpecificPart();
        }
        return this;
    }

    @Override
    public UriComponentsBuilder port(int port) {
        Assert.isTrue(port >= -1, "Port must be >= -1");
        this.port = String.valueOf(port);
        if (port > -1) {
            resetSchemeSpecificPart();
        }
        return this;
    }

    @Override
    public UriComponentsBuilder port(String port) {
        this.port = port;
        if (port != null) {
            resetSchemeSpecificPart();
        }
        return this;
    }

    @Override
    public UriComponentsBuilder path(String path) {
        this.pathBuilder.addPath(path);
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
        this.pathBuilder.addPathSegments(pathSegments);
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder replacePath(String path) {
        this.pathBuilder = new CompositePathComponentBuilder();
        if (path != null) {
            this.pathBuilder.addPath(path);
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder query(String query) {
        if (query != null) {
            Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
            while (matcher.find()) {
                String name = matcher.group(1);
                String eq = matcher.group(2);
                String value = matcher.group(3);
                queryParam(name, (value != null ? value : (StringUtils.hasLength(eq) ? "" : null)));
            }
            resetSchemeSpecificPart();
        } else {
            this.queryParams.clear();
        }
        return this;
    }

    @Override
    public UriComponentsBuilder replaceQuery(String query) {
        this.queryParams.clear();
        if (query != null) {
            query(query);
            resetSchemeSpecificPart();
        }
        return this;
    }

    @Override
    public UriComponentsBuilder queryParam(String name, Object... values) {
        Assert.notNull(name, "Name must not be null");
        if (!ObjectUtils.isEmpty(values)) {
            for (Object value : values) {
                String valueAsString = getQueryParamValue(value);
                this.queryParams.add(name, valueAsString);
            }
        } else {
            this.queryParams.add(name, null);
        }
        resetSchemeSpecificPart();
        return this;
    }

    private String getQueryParamValue(Object value) {
        if (value != null) {
            return (value instanceof Optional ? ((Optional<?>) value).map(Object::toString).orElse(null) : value.toString());
        }
        return null;
    }

    @Override
    public UriComponentsBuilder queryParam(String name, Collection<?> values) {
        return queryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
    }

    @Override
    public UriComponentsBuilder queryParamIfPresent(String name, Object value) {
        Optional.of(value).ifPresent(o -> {
            if (o instanceof Collection) {
                queryParam(name, (Collection<?>) o);
            } else {
                queryParam(name, o);
            }
        });
        return this;
    }

    @Override
    public UriComponentsBuilder queryParams(MultiValueMap<String, String> params) {
        if (params != null) {
            this.queryParams.addAll(params);
            resetSchemeSpecificPart();
        }
        return this;
    }

    @Override
    public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
        Assert.notNull(name, "Name must not be null");
        this.queryParams.remove(name);
        if (!ObjectUtils.isEmpty(values)) {
            queryParam(name, values);
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder replaceQueryParam(String name, Collection<?> values) {
        return replaceQueryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
    }

    @Override
    public UriComponentsBuilder replaceQueryParams(MultiValueMap<String, String> params) {
        this.queryParams.clear();
        if (params != null) {
            this.queryParams.putAll(params);
        }
        return this;
    }

    @Override
    public UriComponentsBuilder fragment(String fragment) {
        if (fragment != null) {
            Assert.hasLength(fragment, "Fragment must not be empty");
            this.fragment = fragment;
        } else {
            this.fragment = null;
        }
        return this;
    }

    /**
     * 配置要在生成时展开的URI变量。
     * <p>所提供的变量可以是所有所需变量的子集。在构建时，扩展可用的URI占位符，而未解析的URI占位符保留在原地，以后仍然可以扩展。
     * <p>与{@link UriComponents#expand(Map)}或{@link #buildAndExpand(Map)}不同，
     * 当您需要提供URI变量而不构建{@link UriComponents}实例，或者可能预先扩展一些共享的默认值（例如主机和端口）时，此方法非常有用。
     *
     * @param uriVariables 要使用的URI变量
     * @return UriComponentsBuilder
     */
    public UriComponentsBuilder uriVariables(Map<String, Object> uriVariables) {
        this.uriVariables.putAll(uriVariables);
        return this;
    }

    private void adaptForwardedHost(String rawValue) {
        int portSeparatorIdx = rawValue.lastIndexOf(':');
        int squareBracketIdx = rawValue.lastIndexOf(']');
        if (portSeparatorIdx > squareBracketIdx) {
            if (squareBracketIdx == -1 && rawValue.indexOf(':') != portSeparatorIdx) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + rawValue);
            }
            host(rawValue.substring(0, portSeparatorIdx));
            port(Integer.parseInt(rawValue.substring(portSeparatorIdx + 1)));
        } else {
            host(rawValue);
            port(null);
        }
    }

    private void resetHierarchicalComponents() {
        this.userInfo = null;
        this.host = null;
        this.port = null;
        this.pathBuilder = new CompositePathComponentBuilder();
        this.queryParams.clear();
    }

    private void resetSchemeSpecificPart() {
        this.ssp = null;
    }

    /**
     * Object的{@code clone()}方法的公共声明。委托给 {@link #cloneBuilder()}
     */
    @Override
    public Object clone() {
        return cloneBuilder();
    }

    /**
     * 克隆此 {@code UriComponentsBuilder}.
     *
     * @return 克隆的 {@code UriComponentsBuilder} 对象
     */
    public UriComponentsBuilder cloneBuilder() {
        return new UriComponentsBuilder(this);
    }

    private interface PathComponentBuilder {
        PathComponent build();

        PathComponentBuilder cloneBuilder();
    }

    private static class CompositePathComponentBuilder implements PathComponentBuilder {
        private final Deque<PathComponentBuilder> builders = new ArrayDeque<>();

        public void addPathSegments(String... pathSegments) {
            if (!ObjectUtils.isEmpty(pathSegments)) {
                PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
                FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
                if (psBuilder == null) {
                    psBuilder = new PathSegmentComponentBuilder();
                    this.builders.add(psBuilder);
                    if (fpBuilder != null) {
                        fpBuilder.removeTrailingSlash();
                    }
                }
                psBuilder.append(pathSegments);
            }
        }

        public void addPath(String path) {
            if (StringUtils.hasText(path)) {
                PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
                FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
                if (psBuilder != null) {
                    path = (path.startsWith("/") ? path : "/" + path);
                }
                if (fpBuilder == null) {
                    fpBuilder = new FullPathComponentBuilder();
                    this.builders.add(fpBuilder);
                }
                fpBuilder.append(path);
            }
        }

        @SuppressWarnings("unchecked")
        private <T> T getLastBuilder(Class<T> builderClass) {
            if (!this.builders.isEmpty()) {
                PathComponentBuilder last = this.builders.getLast();
                if (builderClass.isInstance(last)) {
                    return (T) last;
                }
            }
            return null;
        }

        @Override
        public PathComponent build() {
            int size = this.builders.size();
            List<PathComponent> components = new ArrayList<>(size);
            for (PathComponentBuilder componentBuilder : this.builders) {
                PathComponent pathComponent = componentBuilder.build();
                if (pathComponent != null) {
                    components.add(pathComponent);
                }
            }
            if (components.isEmpty()) {
                return HierarchicalUriComponents.NULL_PATH_COMPONENT;
            }
            if (components.size() == 1) {
                return components.get(0);
            }
            return new HierarchicalUriComponents.PathComponentComposite(components);
        }

        @Override
        public CompositePathComponentBuilder cloneBuilder() {
            CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
            for (PathComponentBuilder builder : this.builders) {
                compositeBuilder.builders.add(builder.cloneBuilder());
            }
            return compositeBuilder;
        }
    }

    private static class FullPathComponentBuilder implements PathComponentBuilder {
        private final StringBuilder path = new StringBuilder();

        public void append(String path) {
            this.path.append(path);
        }

        @Override
        public HierarchicalUriComponents.PathComponent build() {
            if (this.path.length() == 0) {
                return null;
            }
            String sanitized = getSanitizedPath(this.path);
            return new HierarchicalUriComponents.FullPathComponent(sanitized);
        }

        private static String getSanitizedPath(final StringBuilder path) {
            int index = path.indexOf("//");
            if (index >= 0) {
                StringBuilder sanitized = new StringBuilder(path);
                while (index != -1) {
                    sanitized.deleteCharAt(index);
                    index = sanitized.indexOf("//", index);
                }
                return sanitized.toString();
            }
            return path.toString();
        }

        public void removeTrailingSlash() {
            int index = this.path.length() - 1;
            if (this.path.charAt(index) == '/') {
                this.path.deleteCharAt(index);
            }
        }

        @Override
        public FullPathComponentBuilder cloneBuilder() {
            FullPathComponentBuilder builder = new FullPathComponentBuilder();
            builder.append(this.path.toString());
            return builder;
        }
    }

    private static class PathSegmentComponentBuilder implements PathComponentBuilder {
        private final List<String> pathSegments = new ArrayList<>();

        public void append(String... pathSegments) {
            for (String pathSegment : pathSegments) {
                if (StringUtils.hasText(pathSegment)) {
                    this.pathSegments.add(pathSegment);
                }
            }
        }

        @Override
        public PathComponent build() {
            return (this.pathSegments.isEmpty() ? null : new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
        }

        @Override
        public PathSegmentComponentBuilder cloneBuilder() {
            PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
            builder.pathSegments.addAll(this.pathSegments);
            return builder;
        }
    }

    private enum EncodingHint {ENCODE_TEMPLATE, FULLY_ENCODED, NONE}
}
