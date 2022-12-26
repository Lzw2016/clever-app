package org.clever.web.cors;

import org.clever.util.ObjectUtils;
import org.clever.web.http.HttpHeaders;
import org.clever.web.http.HttpMethod;
import org.clever.web.utils.UriComponents;
import org.clever.web.utils.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * 基于<a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>，用于 CORS 请求处理的实用程序类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 10:05 <br/>
 */
public abstract class CorsUtils {
    /**
     * 如果请求是有效的 CORS 请求，则返回 {@code true}，方法是检查 {@code Origin} 标头是否存在并确保来源不同。
     */
    public static boolean isCorsRequest(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return false;
        }
        UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        return !(ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme())
                && ObjectUtils.nullSafeEquals(host, originUrl.getHost())
                && getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));
    }

    private static int getPort(String scheme, int port) {
        if (port == -1) {
            if ("http".equals(scheme) || "ws".equals(scheme)) {
                port = 80;
            } else if ("https".equals(scheme) || "wss".equals(scheme)) {
                port = 443;
            }
        }
        return port;
    }

    /**
     * 如果请求是有效的 CORS 飞行前请求，
     * 则通过使用 {@code Origin} 和 {@code Access-Control-Request-Method} 标头检查 {code OPTIONS} 方法，
     * 返回 {@code true}。
     */
    public static boolean isPreFlightRequest(HttpServletRequest request) {
        return (HttpMethod.OPTIONS.matches(request.getMethod())
                && request.getHeader(HttpHeaders.ORIGIN) != null
                && request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null);
    }
}
