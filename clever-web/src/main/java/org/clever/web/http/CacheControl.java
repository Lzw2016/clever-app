package org.clever.web.http;

import org.clever.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 用于创建“Cache-Control”HTTP 响应标头的构建器。
 * 将缓存控制指令添加到 HTTP 响应可以显着改善与 Web 应用程序交互时的客户端体验。
 * 此构建器仅使用响应指令创建固执己见的“Cache-Control”标头，并考虑了多个用例。
 * <pre>
 * 1.使用 CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS) 缓存 HTTP 响应将导致 Cache-Control: "max-age=3600"
 * 2.使用 CacheControl cc = CacheControl.noStore() 阻止缓存将导致 Cache-Control: "no-store"
 * 3.CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().cachePublic() 等高级情况将导致 Cache-Control: "max-age=3600, no-transform, public"
 * </pre>
 * 请注意，为了提高效率，Cache-Control 标头应与 HTTP 验证器一起编写，例如“Last-Modified”或“ETag”标头。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 13:17 <br/>
 */
public class CacheControl {
    private Duration maxAge;
    private boolean noCache = false;
    private boolean noStore = false;
    private boolean mustRevalidate = false;
    private boolean noTransform = false;
    private boolean cachePublic = false;
    private boolean cachePrivate = false;
    private boolean proxyRevalidate = false;
    private Duration staleWhileRevalidate;
    private Duration staleIfError;
    private Duration sMaxAge;

    /**
     * 创建一个空的 CacheControl 实例。
     *
     * @see #empty()
     */
    protected CacheControl() {
    }

    /**
     * 返回一个空指令。
     * <p>这非常适合使用其他没有“max-age”、“no-cache”或“no-store”的可选指令
     *
     * @return {@code this}，促进方法链接
     */
    public static CacheControl empty() {
        return new CacheControl();
    }

    /**
     * 添加“max-age=”指令。
     * <p>该指令非常适合公开缓存资源，知道它们不会在配置的时间内更改。
     * 还可以使用其他指令，以防共享缓存不应缓存资源({@link #cachePrivate()})或转换({@link #noTransform()})。
     * <p>为了防止缓存重用缓存的响应，即使它已经过时(即通过“max-age”延迟)，应该设置“must-revalidate”指令({@link #mustRevalidate()}
     *
     * @param maxAge 应缓存响应的最长时间
     * @param unit   {@code maxAge} 参数的时间单位
     * @return {@code this}，促进方法链接
     * @see #maxAge(Duration)
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
     */
    public static CacheControl maxAge(long maxAge, TimeUnit unit) {
        return maxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
    }

    /**
     * 添加“max-age=”指令。
     * <p>该指令非常适合公开缓存资源，知道它们不会在配置的时间内更改。
     * 还可以使用其他指令，以防共享缓存不应缓存({@link #cachePrivate()})或转换({@link #noTransform()})资源。
     * <p>为了防止缓存重用缓存的响应，即使它已经过时（即通过“max-age”延迟），应该设置“must-revalidate”指令({@link #mustRevalidate()})
     *
     * @param maxAge the maximum time the response should be cached
     * @return {@code this}, to facilitate method chaining
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
     */
    public static CacheControl maxAge(Duration maxAge) {
        CacheControl cc = new CacheControl();
        cc.maxAge = maxAge;
        return cc;
    }

    /**
     * 添加“no-cache”指令。
     * <p>该指令非常适合告诉缓存，只有当客户端向服务器重新验证响应时，响应才能被重用。
     * 该指令不会完全禁用缓存，并且可能导致客户端发送条件请求（带有“ETag”、“If-Modified-Since”标头）并且服务器以“304 - Not Modified”状态响应。
     * <p>为了禁用缓存并最小化请求/响应交换，应该使用 {@link #noStore()} 指令而不是 {@code #noCache()}。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">rfc7234 section 5.2.2.2</a>
     */
    public static CacheControl noCache() {
        CacheControl cc = new CacheControl();
        cc.noCache = true;
        return cc;
    }

    /**
     * 添加“no-cache”指令。
     * <p>该指令非常适合防止缓存（浏览器和代理）缓存响应内容。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.3">rfc7234 section 5.2.2.3</a>
     */
    public static CacheControl noStore() {
        CacheControl cc = new CacheControl();
        cc.noStore = true;
        return cc;
    }

    /**
     * 添加“must-revalidate”指令。
     * <p>该指令指示一旦它变得陈旧，缓存不得使用响应来满足后续请求，而无需在源服务器上成功验证。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.1">rfc7234 section 5.2.2.1</a>
     */
    public CacheControl mustRevalidate() {
        this.mustRevalidate = true;
        return this;
    }

    /**
     * 添加“no-transform”指令。
     * <p>该指令指示中介（缓存和其他）不应转换响应内容。这对于强制缓存和 CDN 不自动 gzip 或优化响应内容很有用。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.4">rfc7234 section 5.2.2.4</a>
     */
    public CacheControl noTransform() {
        this.noTransform = true;
        return this;
    }

    /**
     * 添加“public”指令。
     * <p>该指令表示任何缓存都可以存储响应，即使响应通常是不可缓存的或只能在私有缓存中缓存。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">rfc7234 section 5.2.2.5</a>
     */
    public CacheControl cachePublic() {
        this.cachePublic = true;
        return this;
    }

    /**
     * 添加“private”指令。
     * <p>该指令指示响应消息是针对单个用户的，并且不得由共享缓存存储。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.6">rfc7234 section 5.2.2.6</a>
     */
    public CacheControl cachePrivate() {
        this.cachePrivate = true;
        return this;
    }

    /**
     * 添加“proxy-revalidate”指令。
     * <p>该指令与“must-revalidate”指令具有相同的含义，只是它不适用于私有缓存（即浏览器、HTTP 客户端）。
     *
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.7">rfc7234 section 5.2.2.7</a>
     */
    public CacheControl proxyRevalidate() {
        this.proxyRevalidate = true;
        return this;
    }

    /**
     * 添加“s-maxage”指令。
     * <p>该指令表明，在共享缓存中，该指令指定的最大期限覆盖其他指令指定的最大期限。
     *
     * @param sMaxAge 应缓存响应的最长时间
     * @param unit    {@code sMaxAge} 参数的时间单位
     * @return {@code this}，促进方法链接
     * @see #sMaxAge(Duration)
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
     */
    public CacheControl sMaxAge(long sMaxAge, TimeUnit unit) {
        return sMaxAge(Duration.ofSeconds(unit.toSeconds(sMaxAge)));
    }

    /**
     * 添加“s-maxage”指令。
     * <p>该指令表明，在共享缓存中，该指令指定的最大期限覆盖其他指令指定的最大期限。
     *
     * @param sMaxAge 应缓存响应的最长时间
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
     */
    public CacheControl sMaxAge(Duration sMaxAge) {
        this.sMaxAge = sMaxAge;
        return this;
    }

    /**
     * 添加“stale-while-revalidate”指令。
     * <p>该指令指示缓存可以在响应变得陈旧后提供它出现的响应，直到指定的秒数。
     * 如果缓存的响应由于此扩展的存在而变得陈旧，缓存应该尝试重新验证它，同时仍然提供陈旧的响应（即不阻塞）。
     *
     * @param staleWhileRevalidate 重新验证时应使用响应的最长时间
     * @param unit                 {@code staleWhileRevalidate} 参数的时间单位
     * @return {@code this}，促进方法链接
     * @see #staleWhileRevalidate(Duration)
     * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 section 3</a>
     */
    public CacheControl staleWhileRevalidate(long staleWhileRevalidate, TimeUnit unit) {
        return staleWhileRevalidate(Duration.ofSeconds(unit.toSeconds(staleWhileRevalidate)));
    }

    /**
     * 添加“stale-while-revalidate”指令。
     * <p>该指令指示缓存可以在响应变得陈旧后提供它出现的响应，直到指定的秒数。
     * 如果缓存的响应由于此扩展的存在而变得陈旧，缓存应该尝试重新验证它，同时仍然提供陈旧的响应（即不阻塞）。
     *
     * @param staleWhileRevalidate 重新验证时应使用响应的最长时间
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 section 3</a>
     */
    public CacheControl staleWhileRevalidate(Duration staleWhileRevalidate) {
        this.staleWhileRevalidate = staleWhileRevalidate;
        return this;
    }

    /**
     * 添加“stale-if-error”指令。
     * <p>该指令指示当遇到错误时，可以使用缓存的陈旧响应来满足请求，而不管其他新鲜度信息如何。
     *
     * @param staleIfError 遇到错误时应使用响应的最长时间
     * @param unit         {@code staleIfError} 参数的时间单位
     * @return {@code this}，促进方法链接
     * @see #staleIfError(Duration)
     * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 section 4</a>
     */
    public CacheControl staleIfError(long staleIfError, TimeUnit unit) {
        return staleIfError(Duration.ofSeconds(unit.toSeconds(staleIfError)));
    }

    /**
     * 添加“stale-if-error”指令。
     * <p>该指令指示当遇到错误时，可以使用缓存的陈旧响应来满足请求，而不管其他新鲜度信息如何。
     *
     * @param staleIfError 遇到错误时应使用响应的最长时间
     * @return {@code this}，促进方法链接
     * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 section 4</a>
     */
    public CacheControl staleIfError(Duration staleIfError) {
        this.staleIfError = staleIfError;
        return this;
    }

    /**
     * 返回“Cache-Control”标头值（如果有）
     *
     * @return 标头值，如果未添加指令，则为 {@code null}
     */
    public String getHeaderValue() {
        String headerValue = toHeaderValue();
        return (StringUtils.hasText(headerValue) ? headerValue : null);
    }

    /**
     * 返回“Cache-Control”标头值
     *
     * @return 标头值（可能为空）
     */
    private String toHeaderValue() {
        StringBuilder headerValue = new StringBuilder();
        if (this.maxAge != null) {
            appendDirective(headerValue, "max-age=" + this.maxAge.getSeconds());
        }
        if (this.noCache) {
            appendDirective(headerValue, "no-cache");
        }
        if (this.noStore) {
            appendDirective(headerValue, "no-store");
        }
        if (this.mustRevalidate) {
            appendDirective(headerValue, "must-revalidate");
        }
        if (this.noTransform) {
            appendDirective(headerValue, "no-transform");
        }
        if (this.cachePublic) {
            appendDirective(headerValue, "public");
        }
        if (this.cachePrivate) {
            appendDirective(headerValue, "private");
        }
        if (this.proxyRevalidate) {
            appendDirective(headerValue, "proxy-revalidate");
        }
        if (this.sMaxAge != null) {
            appendDirective(headerValue, "s-maxage=" + this.sMaxAge.getSeconds());
        }
        if (this.staleIfError != null) {
            appendDirective(headerValue, "stale-if-error=" + this.staleIfError.getSeconds());
        }
        if (this.staleWhileRevalidate != null) {
            appendDirective(headerValue, "stale-while-revalidate=" + this.staleWhileRevalidate.getSeconds());
        }
        return headerValue.toString();
    }

    private void appendDirective(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value);
    }

    @Override
    public String toString() {
        return "CacheControl [" + toHeaderValue() + "]";
    }
}
