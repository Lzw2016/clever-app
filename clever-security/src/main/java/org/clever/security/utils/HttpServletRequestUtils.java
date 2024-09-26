package org.clever.security.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.JacksonMapper;

/**
 * 作者：ymx <br/>
 * 创建时间：2020/12/04 20:42 <br/>
 */
@Slf4j
public class HttpServletRequestUtils {
    public static final String READ_COMPLETE_BODY_DATA_ATTRIBUTE = HttpServletRequestUtils.class.getName() + "_Read_Complete_Body_Data_Attribute";
    public static final String BODY_DATA_ATTRIBUTE = HttpServletRequestUtils.class.getName() + "_Body_Data_Attribute";

    /**
     * 获取请求body数据
     *
     * @param request 请求对象
     */
    public static String getBodyData(HttpServletRequest request) {
        // 已经读取过一次了,直接返回
        Object readComplete = request.getAttribute(READ_COMPLETE_BODY_DATA_ATTRIBUTE);
        if (readComplete instanceof Boolean && (Boolean) readComplete) {
            Object bodyObject = request.getAttribute(BODY_DATA_ATTRIBUTE);
            if (bodyObject instanceof CharSequence) {
                return String.valueOf(bodyObject);
            }
            return null;
        }
        // 读取请求body数据
        String bodyData = null;
        try {
            bodyData = IOUtils.toString(request.getReader());
        } catch (Exception e) {
            log.debug("读取请求body数据失败", e);
        } finally {
            request.setAttribute(READ_COMPLETE_BODY_DATA_ATTRIBUTE, true);
        }
        request.setAttribute(BODY_DATA_ATTRIBUTE, bodyData);
        if (StringUtils.isNotBlank(bodyData)) {
            return bodyData;
        }
        return null;
    }

    /**
     * 解析请求body数据成实体对象(Json反序列化)
     *
     * @param request 请求对象
     * @param clazz   实体类类型
     * @return 解析失败返回null
     */
    public static <T> T parseBodyToEntity(HttpServletRequest request, Class<T> clazz) {
        String jsonBody = getBodyData(request);
        if (StringUtils.isBlank(jsonBody)) {
            return null;
        }
        jsonBody = StringUtils.trim(jsonBody);
        if (!jsonBody.startsWith("{") || !jsonBody.endsWith("}")) {
            return null;
        }
        try {
            return JacksonMapper.getInstance().fromJson(jsonBody, clazz);
        } catch (Exception e) {
            log.debug("解析请求body数据失败", e);
        }
        return null;
    }
}
