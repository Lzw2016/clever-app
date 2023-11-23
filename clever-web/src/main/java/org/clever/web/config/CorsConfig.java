package org.clever.web.config;

import lombok.Getter;
import lombok.Setter;
import org.clever.util.CollectionUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.clever.web.http.HttpMethod;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/23 10:16 <br/>
 */
public class CorsConfig {
    public static final String PREFIX = WebConfig.PREFIX + ".cors";
    /**
     * 表示所有的 origins、methods、headers 的通配符
     */
    public static final String ALL = "*";

    private static final List<String> ALL_LIST = Collections.singletonList(ALL);
    private static final OriginPattern ALL_PATTERN = new OriginPattern("*");
    private static final List<OriginPattern> ALL_PATTERN_LIST = Collections.singletonList(ALL_PATTERN);
    private static final List<String> DEFAULT_PERMIT_ALL = Collections.singletonList(ALL);
    private static final List<HttpMethod> DEFAULT_METHODS = Collections.unmodifiableList(
            Arrays.asList(HttpMethod.GET, HttpMethod.HEAD)
    );
    private static final List<String> DEFAULT_PERMIT_METHODS = Collections.unmodifiableList(
            Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name())
    );

    /**
     * 启用 CorsFilter
     */
    @Setter
    @Getter
    private boolean enable = false;
    /**
     * 支持跨域的path(支持AntPath风格，默认：“/**”)
     */
    @Setter
    @Getter
    private List<String> pathPattern = Collections.singletonList("/**");
    /**
     * 允许的域(不支持匹配符，如："*")
     */
    @Getter
    private List<String> allowedOrigins;
    /**
     * 允许的域(支持匹配符，如："*")
     */
    private List<OriginPattern> allowedOriginPatterns;
    /**
     * 允许客户端能发送的Http Method，"*"为所有Method
     */
    @Getter
    private List<String> allowedMethods;
    /**
     * 允许客户端能发送的Http Method(解析后的HttpMethod对象)
     */
    private List<HttpMethod> resolvedMethods = DEFAULT_METHODS;
    /**
     * 允许客户端能发送的Http Header，"*"为所有Header
     */
    @Getter
    private List<String> allowedHeaders;
    /**
     * 允许客户端能获取的的Http Header，不支持"*"
     */
    @Getter
    private List<String> exposedHeaders;
    /**
     * 是否允许凭证
     */
    @Getter
    private Boolean allowCredentials;
    /**
     * 配置客户端可以缓存pre-flight请求的响应的持续时间
     */
    @Getter
    private Long maxAge;

    public CorsConfig() {
    }

    public CorsConfig(CorsConfig other) {
        this.enable = other.enable;
        this.pathPattern = other.pathPattern;
        this.allowedOrigins = other.allowedOrigins;
        this.allowedOriginPatterns = other.allowedOriginPatterns;
        this.allowedMethods = other.allowedMethods;
        this.resolvedMethods = other.resolvedMethods;
        this.allowedHeaders = other.allowedHeaders;
        this.exposedHeaders = other.exposedHeaders;
        this.allowCredentials = other.allowCredentials;
        this.maxAge = other.maxAge;
    }

    private String trimTrailingSlash(String origin) {
        return (origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin);
    }

    public void setAllowedOrigins(List<String> origins) {
        this.allowedOrigins = (origins == null ? null : origins.stream()
                .filter(Objects::nonNull).map(this::trimTrailingSlash).collect(Collectors.toList()));
    }

    public void addAllowedOrigin(String origin) {
        if (origin == null) {
            return;
        }
        if (this.allowedOrigins == null) {
            this.allowedOrigins = new ArrayList<>(4);
        } else if (this.allowedOrigins == DEFAULT_PERMIT_ALL && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
            setAllowedOrigins(DEFAULT_PERMIT_ALL);
        }
        origin = trimTrailingSlash(origin);
        this.allowedOrigins.add(origin);
    }

    public CorsConfig setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        if (allowedOriginPatterns == null) {
            this.allowedOriginPatterns = null;
        } else {
            this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns.size());
            for (String patternValue : allowedOriginPatterns) {
                addAllowedOriginPattern(patternValue);
            }
        }
        return this;
    }

    public List<String> getAllowedOriginPatterns() {
        if (this.allowedOriginPatterns == null) {
            return null;
        }
        return this.allowedOriginPatterns.stream().map(OriginPattern::getDeclaredPattern).collect(Collectors.toList());
    }

    public void addAllowedOriginPattern(String originPattern) {
        if (originPattern == null) {
            return;
        }
        if (this.allowedOriginPatterns == null) {
            this.allowedOriginPatterns = new ArrayList<>(4);
        }
        originPattern = trimTrailingSlash(originPattern);
        this.allowedOriginPatterns.add(new OriginPattern(originPattern));
        if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
            this.allowedOrigins = null;
        }
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = (allowedMethods != null ? new ArrayList<>(allowedMethods) : null);
        if (!CollectionUtils.isEmpty(allowedMethods)) {
            this.resolvedMethods = new ArrayList<>(allowedMethods.size());
            for (String method : allowedMethods) {
                if (ALL.equals(method)) {
                    this.resolvedMethods = null;
                    break;
                }
                this.resolvedMethods.add(HttpMethod.resolve(method));
            }
        } else {
            this.resolvedMethods = DEFAULT_METHODS;
        }
    }

    public void addAllowedMethod(HttpMethod method) {
        addAllowedMethod(method.name());
    }

    public void addAllowedMethod(String method) {
        if (StringUtils.hasText(method)) {
            if (this.allowedMethods == null) {
                this.allowedMethods = new ArrayList<>(4);
                this.resolvedMethods = new ArrayList<>(4);
            } else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
                setAllowedMethods(DEFAULT_PERMIT_METHODS);
            }
            this.allowedMethods.add(method);
            if (ALL.equals(method)) {
                this.resolvedMethods = null;
            } else if (this.resolvedMethods != null) {
                this.resolvedMethods.add(HttpMethod.resolve(method));
            }
        }
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
    }

    public void addAllowedHeader(String allowedHeader) {
        if (this.allowedHeaders == null) {
            this.allowedHeaders = new ArrayList<>(4);
        } else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
            setAllowedHeaders(DEFAULT_PERMIT_ALL);
        }
        this.allowedHeaders.add(allowedHeader);
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
    }

    public void addExposedHeader(String exposedHeader) {
        if (this.exposedHeaders == null) {
            this.exposedHeaders = new ArrayList<>(4);
        }
        this.exposedHeaders.add(exposedHeader);
    }

    public void setAllowCredentials(Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge.getSeconds();
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * 默认情况下，CorsConfig 不允许任何跨域请求，必须明确配置。
     * 使用此方法可以切换到默认值，这些默认值允许对 GET、HEAD 和 POST 的所有跨域请求，但不会覆盖任何已设置的值。
     * 以下默认值适用于未设置的值：
     * <pre>
     * 1.允许所有具有 CORS 规范中定义的特殊值“*”的来源。仅当 origins 和 originPatterns 均未设置时才设置
     * 2.允许“简单”方法 GET、HEAD 和 POST
     * 3.允许所有 Header
     * 4.将 maxAge 设置为 1800 秒（30 分钟）
     * </pre>
     */
    public CorsConfig applyPermitDefaultValues() {
        if (this.allowedOrigins == null && this.allowedOriginPatterns == null) {
            this.allowedOrigins = DEFAULT_PERMIT_ALL;
        }
        if (this.allowedMethods == null) {
            this.allowedMethods = DEFAULT_PERMIT_METHODS;
            this.resolvedMethods = DEFAULT_PERMIT_METHODS.stream().map(HttpMethod::resolve).collect(Collectors.toList());
        }
        if (this.allowedHeaders == null) {
            this.allowedHeaders = DEFAULT_PERMIT_ALL;
        }
        if (this.maxAge == null) {
            this.maxAge = 1800L;
        }
        return this;
    }

    public void validateAllowCredentials() {
        if (this.allowCredentials == Boolean.TRUE && this.allowedOrigins != null && this.allowedOrigins.contains(ALL)) {
            throw new IllegalArgumentException(
                    "When allowCredentials is true, allowedOrigins cannot contain the special value \"*\" " +
                            "since that cannot be set on the \"Access-Control-Allow-Origin\" response header. " +
                            "To allow credentials to a set of origins, list them explicitly " +
                            "or consider using \"allowedOriginPatterns\" instead.");
        }
    }

    /**
     * 将提供的 CorsConfig 的非空属性与此属性组合。
     * 当组合单个值（如 allowCredentials 或 maxAge）时，此属性将被非空的其他属性（如果有）覆盖。
     * allowedOrigins、allowedMethods、allowedHeaders 或 exposedHeaders 等列表的组合是以一种加法方式完成的。
     * 例如，将 ["GET", "POST"] 与 ["PATCH"] 组合会产生 ["GET", "POST", "PATCH"]。
     * 但是，将 ["GET"、"POST"] 与 [""*"] 组合会导致 [""*"]。
     * 另请注意，由 applyPermitDefaultValues() 设置的默认许可值会被任何明确定义的值覆盖。
     */
    public CorsConfig combine(CorsConfig other) {
        if (other == null) {
            return this;
        }
        // Bypass setAllowedOrigins to avoid re-compiling patterns
        CorsConfig config = new CorsConfig(this);
        List<String> origins = combine(getAllowedOrigins(), other.getAllowedOrigins());
        List<OriginPattern> patterns = combinePatterns(this.allowedOriginPatterns, other.allowedOriginPatterns);
        config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);
        config.allowedOriginPatterns = patterns;
        config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
        config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
        config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));
        Boolean allowCredentials = other.getAllowCredentials();
        if (allowCredentials != null) {
            config.setAllowCredentials(allowCredentials);
        }
        Long maxAge = other.getMaxAge();
        if (maxAge != null) {
            config.setMaxAge(maxAge);
        }
        return config;
    }

    private List<String> combine(List<String> source, List<String> other) {
        if (other == null) {
            return (source != null ? source : Collections.emptyList());
        }
        if (source == null) {
            return other;
        }
        if (source == DEFAULT_PERMIT_ALL || source == DEFAULT_PERMIT_METHODS) {
            return other;
        }
        if (other == DEFAULT_PERMIT_ALL || other == DEFAULT_PERMIT_METHODS) {
            return source;
        }
        if (source.contains(ALL) || other.contains(ALL)) {
            return ALL_LIST;
        }
        Set<String> combined = new LinkedHashSet<>(source.size() + other.size());
        combined.addAll(source);
        combined.addAll(other);
        return new ArrayList<>(combined);
    }

    private List<OriginPattern> combinePatterns(List<OriginPattern> source, List<OriginPattern> other) {
        if (other == null) {
            return (source != null ? source : Collections.emptyList());
        }
        if (source == null) {
            return other;
        }
        if (source.contains(ALL_PATTERN) || other.contains(ALL_PATTERN)) {
            return ALL_PATTERN_LIST;
        }
        Set<OriginPattern> combined = new LinkedHashSet<>(source.size() + other.size());
        combined.addAll(source);
        combined.addAll(other);
        return new ArrayList<>(combined);
    }

    public String checkOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return null;
        }
        String originToCheck = trimTrailingSlash(origin);
        if (!ObjectUtils.isEmpty(this.allowedOrigins)) {
            if (this.allowedOrigins.contains(ALL)) {
                validateAllowCredentials();
                return ALL;
            }
            for (String allowedOrigin : this.allowedOrigins) {
                if (originToCheck.equalsIgnoreCase(allowedOrigin)) {
                    return origin;
                }
            }
        }
        if (!ObjectUtils.isEmpty(this.allowedOriginPatterns)) {
            for (OriginPattern p : this.allowedOriginPatterns) {
                if (p.getDeclaredPattern().equals(ALL) || p.getPattern().matcher(originToCheck).matches()) {
                    return origin;
                }
            }
        }
        return null;
    }

    public List<HttpMethod> checkHttpMethod(HttpMethod requestMethod) {
        if (requestMethod == null) {
            return null;
        }
        if (this.resolvedMethods == null) {
            return Collections.singletonList(requestMethod);
        }
        return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
    }

    public List<String> checkHeaders(List<String> requestHeaders) {
        if (requestHeaders == null) {
            return null;
        }
        if (requestHeaders.isEmpty()) {
            return Collections.emptyList();
        }
        if (ObjectUtils.isEmpty(this.allowedHeaders)) {
            return null;
        }
        boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
        List<String> result = new ArrayList<>(requestHeaders.size());
        for (String requestHeader : requestHeaders) {
            if (StringUtils.hasText(requestHeader)) {
                requestHeader = requestHeader.trim();
                if (allowAnyHeader) {
                    result.add(requestHeader);
                } else {
                    for (String allowedHeader : this.allowedHeaders) {
                        if (requestHeader.equalsIgnoreCase(allowedHeader)) {
                            result.add(requestHeader);
                            break;
                        }
                    }
                }
            }
        }
        return (result.isEmpty() ? null : result);
    }

    /**
     * 包含用户声明的模式（例如“https:.domain.com”）和从中派生的正则表达式 {@link Pattern}
     * <p>
     * 作者：lizw <br/>
     * 创建时间：2022/12/23 11:10 <br/>
     */
    @Getter
    public static class OriginPattern {
        private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

        private final String declaredPattern;
        private final Pattern pattern;

        public OriginPattern(String declaredPattern) {
            this.declaredPattern = declaredPattern;
            this.pattern = initPattern(declaredPattern);
        }

        private static Pattern initPattern(String patternValue) {
            String portList = null;
            Matcher matcher = PORTS_PATTERN.matcher(patternValue);
            if (matcher.matches()) {
                patternValue = matcher.group(1);
                portList = matcher.group(2);
            }
            patternValue = "\\Q" + patternValue + "\\E";
            patternValue = patternValue.replace("*", "\\E.*\\Q");
            if (portList != null) {
                patternValue += (portList.equals(ALL) ? "(:\\d+)?" : ":(" + portList.replace(',', '|') + ")");
            }
            return Pattern.compile(patternValue);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || !getClass().equals(other.getClass())) {
                return false;
            }
            return ObjectUtils.nullSafeEquals(this.declaredPattern, ((OriginPattern) other).declaredPattern);
        }

        @Override
        public int hashCode() {
            return this.declaredPattern.hashCode();
        }

        @Override
        public String toString() {
            return this.declaredPattern;
        }
    }
}
