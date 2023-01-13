package org.clever.web.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;

/**
 * 用于 URL 路径匹配的辅助类，包括并支持一致的 URL 解码。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/11 17:07 <br/>
 */
public class UrlPathHelper {
    private static final Logger logger = LoggerFactory.getLogger(UrlPathHelper.class);
    /**
     * 具有默认值的共享只读实例。以下适用：
     * <ul>
     * <li>{@code alwaysUseFullPath=false}
     * <li>{@code urlDecode=true}
     * <li>{@code removeSemicolon=true}
     * <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING}
     * </ul>
     */
    public static final UrlPathHelper defaultInstance = new UrlPathHelper();

    static {
        defaultInstance.setReadOnly();
    }

    private boolean urlDecode = true;
    private boolean readOnly = false;
    private String defaultEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

    /**
     * Return the default character encoding to use for URL decoding.
     */
    protected String getDefaultEncoding() {
        return this.defaultEncoding;
    }

    /**
     * 使用 URLDecoder 解码给定的源字符串。编码将从请求中获取，回退到默认的“ISO-8859-1”。
     * <p>默认实现使用 {@code URLDecoder.decode(input, enc)}。
     *
     * @param request 当前 HTTP 请求
     * @param source  要解码的字符串
     * @return 解码后的字符串
     * @see WebUtils#DEFAULT_CHARACTER_ENCODING
     * @see javax.servlet.ServletRequest#getCharacterEncoding
     * @see java.net.URLDecoder#decode(String, String)
     */
    public String decodeRequestString(HttpServletRequest request, String source) {
        if (this.urlDecode) {
            return decodeInternal(request, source);
        }
        return source;
    }

    /**
     * 确定给定请求的编码。可以在子类中被覆盖。
     * <p>默认实现检查请求编码，回退到为此解析器指定的默认编码。
     *
     * @param request 当前 HTTP 请求
     * @return 请求的编码（从不{@code null}）
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    protected String determineEncoding(HttpServletRequest request) {
        String enc = request.getCharacterEncoding();
        if (enc == null) {
            enc = getDefaultEncoding();
        }
        return enc;
    }

    @SuppressWarnings("deprecation")
    private String decodeInternal(HttpServletRequest request, String source) {
        String enc = determineEncoding(request);
        try {
            return UriUtils.decode(source, enc);
        } catch (UnsupportedCharsetException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not decode request string [" + source + "] with encoding '" + enc
                        + "': falling back to platform default encoding; exception message: " + ex.getMessage());
            }
            return URLDecoder.decode(source);
        }
    }

    /**
     * 切换到只读模式，不允许进一步更改配置。
     */
    private void setReadOnly() {
        this.readOnly = true;
    }
}
