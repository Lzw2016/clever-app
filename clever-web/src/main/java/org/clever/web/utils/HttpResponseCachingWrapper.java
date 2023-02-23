package org.clever.web.utils;

import javax.servlet.http.HttpServletResponse;

/**
 * 缓存响应数据
 * 参考 org.springframework.web.util.ContentCachingResponseWrapper
 * 作者：lizw <br/>
 * 创建时间：2022/04/02 11:16 <br/>
 */
public class HttpResponseCachingWrapper extends ContentCachingResponseWrapper {
    public static HttpResponseCachingWrapper wrapper(HttpServletResponse response) {
        if (response instanceof HttpResponseCachingWrapper) {
            return (HttpResponseCachingWrapper) response;
        }
        return new HttpResponseCachingWrapper(response);
    }

    public HttpResponseCachingWrapper(HttpServletResponse response) {
        super(response);
    }
}
