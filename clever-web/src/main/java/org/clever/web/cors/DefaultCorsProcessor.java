package org.clever.web.cors;

import lombok.extern.slf4j.Slf4j;
import org.clever.util.CollectionUtils;
import org.clever.util.StringUtils;
import org.clever.web.config.CorsConfig;
import org.clever.web.http.HttpHeaders;
import org.clever.web.http.HttpMethod;
import org.clever.web.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * {@link CorsProcessor} 的默认实现，由 <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a> <br/>
 * 请注意，当输入 {@link CorsProcessor} 为 {@code null} 时，
 * 此实现不会完全拒绝简单或实际请求，而只是避免将 CORS 标头添加到响应中。
 * 如果响应已包含 CORS 标头，也会跳过 CORS 处理。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 10:02 <br/>
 */
@Slf4j
public class DefaultCorsProcessor implements CorsProcessor {
    @Override
    public boolean processRequest(CorsConfig config, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Collection<String> varyHeaders = response.getHeaders(HttpHeaders.VARY);
        if (!varyHeaders.contains(HttpHeaders.ORIGIN)) {
            response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        }
        if (!varyHeaders.contains(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)) {
            response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        }
        if (!varyHeaders.contains(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS)) {
            response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        }
        if (!CorsUtils.isCorsRequest(request)) {
            return true;
        }
        if (response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            log.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
            return true;
        }
        boolean preFlightRequest = CorsUtils.isPreFlightRequest(request);
        if (config == null) {
            if (preFlightRequest) {
                rejectRequest(response);
                return false;
            } else {
                return true;
            }
        }
        return handleInternal(request, response, config, preFlightRequest);
    }

    /**
     * 当其中一项 CORS 检查失败时调用。
     * 默认实现将响应状态设置为 403 并将“无效的 CORS 请求”写入响应。
     */
    protected void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().print("Invalid CORS request");
        response.getWriter().flush();
    }

    /**
     * 处理给定的请求
     */
    protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response, CorsConfig config, boolean preFlightRequest) throws IOException {
        String requestOrigin = request.getHeader(HttpHeaders.ORIGIN);
        String allowOrigin = checkOrigin(config, requestOrigin);
        if (allowOrigin == null) {
            log.debug("Reject: '" + requestOrigin + "' origin is not allowed");
            rejectRequest(response);
            return false;
        }
        HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
        List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
        if (allowMethods == null) {
            log.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
            rejectRequest(response);
            return false;
        }
        List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
        List<String> allowHeaders = checkHeaders(config, requestHeaders);
        if (preFlightRequest && allowHeaders == null) {
            log.debug("Reject: headers '" + requestHeaders + "' are not allowed");
            rejectRequest(response);
            return false;
        }
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
        if (preFlightRequest) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowMethods));
        }
        if (preFlightRequest && !allowHeaders.isEmpty()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, StringUtils.collectionToCommaDelimitedString(allowHeaders));
        }
        if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, StringUtils.collectionToCommaDelimitedString(config.getExposedHeaders()));
        }
        if (Boolean.TRUE.equals(config.getAllowCredentials())) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
        if (preFlightRequest && config.getMaxAge() != null) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, config.getMaxAge().toString());
        }
        // response.flushBuffer();
        return true;
    }

    /**
     * 检查来源并确定响应的来源
     * 默认实现只是委托给 {@link org.clever.web.config.CorsConfig#checkOrigin(String)}
     */
    protected String checkOrigin(CorsConfig config, String requestOrigin) {
        return config.checkOrigin(requestOrigin);
    }

    /**
     * 检查 HTTP 方法并确定pre-flight前请求的响应方法。
     * 默认实现只是委托给 {@link org.clever.web.config.CorsConfig#checkHttpMethod(HttpMethod)}.
     */
    protected List<HttpMethod> checkMethods(CorsConfig config, HttpMethod requestMethod) {
        return config.checkHttpMethod(requestMethod);
    }

    private HttpMethod getMethodToUse(HttpServletRequest request, boolean isPreFlight) {
        if (isPreFlight) {
            return HttpMethod.resolve(request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD));
        }
        return HttpMethod.resolve(request.getMethod());
    }

    /**
     * 检查标头并确定pre-flight前请求响应的标头。
     * 默认实现只是委托给 {@link org.clever.web.config.CorsConfig#checkHeaders(List)}
     */
    protected List<String> checkHeaders(CorsConfig config, List<String> requestHeaders) {
        return config.checkHeaders(requestHeaders);
    }

    private List<String> getHeadersToUse(HttpServletRequest request, boolean isPreFlight) {
        if (isPreFlight) {
            Enumeration<String> headerValues = request.getHeaders(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (headerValues == null) {
                return Collections.emptyList();
            }
            List<String> values = new ArrayList<>();
            while (headerValues.hasMoreElements()) {
                values.add(headerValues.nextElement());
            }
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (value != null) {
                    Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
                }
            }
            return result;
        }
        Enumeration<String> names = request.getHeaderNames();
        List<String> result = new ArrayList<>();
        while (names.hasMoreElements()) {
            result.add(names.nextElement());
        }
        return result;
    }
}

