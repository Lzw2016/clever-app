package org.clever.core.http;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * HttpServletRequest工具类<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-6-19 23:12 <br/>
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class HttpServletRequestUtils {
    /**
     * 获取请求的URL地址
     * <pre>{@code
     * 示例：
     * 当前url：http://localhost:8080/context_path/test_api?param_1=value_1
     * request.getRequestURL()   http://localhost:8080/context_path/test_api?param_1=value_1
     * request.getContextPath()  /context_path
     * request.getRequestURI()   /context_path/test_api
     * request.getPathInfo()     /test_api
     * request.getQueryString()  param_1=value_1
     * }</pre>
     *
     * @param request 请求对象
     * @return 请求的URL地址
     */
    public static String getRequestURL(HttpServletRequest request) {
        if (request == null) {
            return "";
        } else {
            return request.getRequestURL().toString();
        }
    }

    /**
     * 获取请求的URI地址
     * <pre>
     * 示例：
     * 当前url：http://localhost:8080/CarsiLogCenter_new/idpstat.jsp?action=idp.sptopn
     * request.getRequestURI() /CarsiLogCenter_new/idpstat.jsp
     * </pre>
     *
     * @param request 请求对象
     * @return 请求的URL地址
     */
    public static String getRequestURI(HttpServletRequest request) {
        if (request == null) {
            return "";
        } else {
            return request.getRequestURI();
        }
    }

    /**
     * 获取请求的URI地址(不包含后缀,如:.json、.xml、.html、.jsp等)
     * <pre>
     * 示例：
     * 当前url：http://localhost:8080/CarsiLogCenter_new/idpstat.jsp?action=idp.sptopn
     * request.getRequestURI() /CarsiLogCenter_new/idpstat
     * </pre>
     *
     * @param request 请求对象
     * @return 请求的URL地址
     */
    public static String getRequestURINotSuffix(HttpServletRequest request) {
        if (request == null) {
            return "";
        } else {
            String requestUrl = request.getRequestURI();
            int positionBySeparator = requestUrl.lastIndexOf("/");
            int positionPoint = requestUrl.lastIndexOf(".");
            if (positionPoint >= 0 && positionPoint > positionBySeparator) {
                requestUrl = requestUrl.substring(0, positionPoint);
            }
            return requestUrl;
        }
    }

    /**
     * 字符串数组输出
     */
    private static String arrayToString(String[] array) {
        if (array == null || array.length == 0) {
            return "";
        }
        if (array.length == 1) {
            return array[0];
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : array) {
            if (stringBuilder.length() <= 0) {
                stringBuilder.append(str);
            } else {
                stringBuilder.append(",").append(str);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 获取请求的所有参数
     *
     * @param request 请求对象
     * @return 请求数据值(已解码)
     */
    @SneakyThrows
    public static String getRequestParams(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        // 用户上传文件请求[系统判断]
        if (isMultipartRequest(request)) {
            return "";
        }
        StringBuilder paramStr = new StringBuilder();
        Set<Map.Entry<String, String[]>> paramMap = request.getParameterMap().entrySet();
        for (Map.Entry<String, String[]> entry : paramMap) {
            if (paramStr.length() <= 0) {
                paramStr.append(entry.getKey()).append("=").append(arrayToString(entry.getValue()));
            } else {
                paramStr.append("&").append(entry.getKey()).append("=").append(arrayToString(entry.getValue()));
            }
        }
        String result = paramStr.toString();
        result = URLDecoder.decode(result, StandardCharsets.UTF_8);
        return result;
    }

    /**
     * 判断当前请求是否是上传文件请求
     */
    public static boolean isMultipartRequest(HttpServletRequest request) {
        final String contentType = request.getContentType();
        if ((contentType != null && contentType.toLowerCase().startsWith("multipart/"))) {
            return true;
        }
        return request.getClass().getName().contains("MultipartHttpServletRequest");
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ip = null;
        String xForwardedFor = request.getHeader("x-forwarded-for");
        if (StringUtils.isNotBlank(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            if (xForwardedFor.contains(",")) {
                ip = StringUtils.split(xForwardedFor, ",")[0];
            } else {
                ip = xForwardedFor;
            }
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
            ip = StringUtils.split(ip, ",")[0];
        }
        return ip;
    }
}
