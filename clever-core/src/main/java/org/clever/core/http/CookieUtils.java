package org.clever.core.http;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * 作者：LiZW <br/>
 * 创建时间：2016-5-8 16:31 <br/>
 */
@Slf4j
public class CookieUtils {
    /**
     * Cookie的默认编码格式
     */
    public static String DEFAULT_COOKIE_ENCODE = "UTF-8";

    /**
     * 设置Cookie
     *
     * @param response HTTP响应
     * @param path     Cookie的Path
     * @param name     名称
     * @param value    值
     * @param maxAge   Cookie生存时间，单位：秒。负数表示Cookie永不过期，0表示删除Cookie
     */
    @SneakyThrows
    public static void setCookie(HttpServletResponse response, String path, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        if (StringUtils.isNotBlank(path)) {
            cookie.setPath(path);
        }
        if (maxAge >= 0) {
            cookie.setMaxAge(maxAge);
        }
        cookie.setValue(encodeRequestString(response, value));
        response.addCookie(cookie);
    }

    /**
     * 设置Cookie
     *
     * @param response HTTP响应
     * @param path     Cookie的Path
     * @param name     名称
     * @param value    值
     */
    public static void setCookie(HttpServletResponse response, String path, String name, String value) {
        setCookie(response, path, name, value, Integer.MAX_VALUE);
    }

    /**
     * 设置Cookie(当前路径)
     *
     * @param name  名称
     * @param value 值
     */
    public static void setCookieForCurrentPath(HttpServletResponse response, String name, String value) {
        setCookie(response, null, name, value, Integer.MAX_VALUE);
    }

    /**
     * 设置Cookie(根路径)
     *
     * @param name  名称
     * @param value 值
     */
    public static void setCookieForRooPath(HttpServletResponse response, String name, String value) {
        setCookie(response, "/", name, value, Integer.MAX_VALUE);
    }

    /**
     * 获得指定Cookie的值
     *
     * @param request 请求对象
     * @param name    名字
     * @return Cookie值，不存在返回null
     */
    public static String getCookie(HttpServletRequest request, String name) {
        String value = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    try {
                        value = decodeRequestString(request, cookie.getValue());
                    } catch (Exception ignored) {
                        value = cookie.getValue();
                    }
                    break;
                }
            }
        }
        return value;
    }

    /**
     * 删除指定Cookie
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param name     名称
     * @param path     Cookie的Path
     */
    public static void delCookie(HttpServletRequest request, HttpServletResponse response, String name, String path) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setMaxAge(0);
                    cookie.setPath(path);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }

    /**
     * 删除指定Cookie(当前路径)
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param name     名称
     */
    public static void delCookieForCurrentPath(HttpServletRequest request, HttpServletResponse response, String name) {
        delCookie(request, response, name, null);
    }

    /**
     * 删除指定Cookie(根路径)
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param name     名称
     */
    public static void delCookieForRooPath(HttpServletRequest request, HttpServletResponse response, String name) {
        delCookie(request, response, name, "/");
    }

    /**
     * 使用 URLDecoder 解码给定的源字符串。编码将从请求中获取，回退到默认的“UTF-8”。
     * <p>默认实现使用 {@code URLDecoder.decode(input, enc)}。
     *
     * @param request 当前 HTTP 请求
     * @param source  要解码的字符串
     * @return 解码后的字符串
     * @see javax.servlet.ServletRequest#getCharacterEncoding
     * @see java.net.URLDecoder#decode(String, String)
     */
    @SuppressWarnings("deprecation")
    public static String decodeRequestString(HttpServletRequest request, String source) {
        String enc = request.getCharacterEncoding();
        if (enc == null) {
            enc = DEFAULT_COOKIE_ENCODE;
        }
        try {
            return URLDecoder.decode(source, enc);
        } catch (UnsupportedEncodingException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Could not decode request string [" + source + "] with encoding '" + enc
                        + "': falling back to platform default encoding; exception message: " + ex.getMessage());
            }
            return URLDecoder.decode(source);
        }
    }

    /**
     * 使用 URLEncoder 编码给定的源字符串。编码将从响应中获取，回退到默认的“UTF-8”。
     * <p>默认实现使用 {@code URLEncoder.encode(input, enc)}。
     *
     * @param response 当前 HTTP 响应
     * @param source   要解码的字符串
     * @return 解码后的字符串
     * @see javax.servlet.ServletResponse#getCharacterEncoding
     * @see java.net.URLEncoder#encode(String, String)
     */
    @SuppressWarnings("deprecation")
    public static String encodeRequestString(HttpServletResponse response, String source) {
        String enc = response.getCharacterEncoding();
        if (enc == null) {
            enc = DEFAULT_COOKIE_ENCODE;
        }
        try {
            return URLEncoder.encode(source, enc);
        } catch (UnsupportedEncodingException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Could not encode request string [" + source + "] with encoding '" + enc
                        + "': falling back to platform default encoding; exception message: " + ex.getMessage());
            }
            return URLEncoder.encode(source);
        }
    }
}
