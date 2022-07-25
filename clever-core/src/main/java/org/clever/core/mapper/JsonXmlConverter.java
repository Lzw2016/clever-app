package org.clever.core.mapper;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.json.JsonTypeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

/**
 * Json数据与XML数据相互转换的工具类，使用org.json实现<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-5-1 13:03 <br/>
 */
public class JsonXmlConverter {
    /**
     * XML字符串转换成Json字符串<br/>
     *
     * @param xml XML字符串
     * @return Json字符串
     */
    public static String xmlToJson(String xml) {
        if (StringUtils.isBlank(xml)) {
            return null;
        }
        JSONObject jsonObject = XML.toJSONObject(xml);
        return jsonObject.toString();
    }

    /**
     * Json字符串转换成XML字符串<br/>
     *
     * @param json Json字符串
     * @return XML字符串
     */
    public static String jsonToXml(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        Object object;
        String jsonType = JsonTypeUtils.getJSONType(json);
        if (JsonTypeUtils.JSON_OBJECT.equals(jsonType)) {
            object = new JSONObject(json);
        } else if (JsonTypeUtils.JSON_ARRAY.equals(jsonType)) {
            object = new JSONArray(json);
        } else {
            throw new RuntimeException("Json字符串格式不正确");
        }
        return XML.toString(object);
    }
}
