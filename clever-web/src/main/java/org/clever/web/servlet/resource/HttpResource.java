package org.clever.web.servlet.resource;

import org.clever.core.io.Resource;
import org.clever.web.http.HttpHeaders;

/**
 * 要写入 HTTP 响应的 {@link Resource} 的扩展接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 14:54 <br/>
 */
public interface HttpResource extends Resource {
    /**
     * 用于为当前资源提供服务的 HTTP 响应的 HTTP 标头
     *
     * @return HTTP 响应标头
     */
    HttpHeaders getResponseHeaders();
}
