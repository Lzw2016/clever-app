package org.clever.web.cors;

import org.clever.web.config.CorsConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 接受请求和 {@link CorsConfig} 并更新响应的策略 <br/>
 * 此组件不关心如何选择 {@code CorsConfig}，而是采取后续操作，例如应用 CORS 验证检查以及拒绝响应或将 CORS 标头添加到响应中。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 09:55 <br/>
 *
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 */
public interface CorsProcessor {
    /**
     * 处理给定 {@code CorsConfig} 的请求
     *
     * @param corsConfig 适用的 CORS 配置（可能是 {@code null}）
     * @param request    当前请求
     * @param response   当前响应
     * @return {@code false} 如果请求被拒绝，否则返回 {@code true}
     */
    boolean processRequest(CorsConfig corsConfig, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
