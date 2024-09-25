package org.clever.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.config.Key;
import io.javalin.json.JsonMapper;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/15 22:38 <br/>
 */
public interface JavalinAppDataKey {
    /**
     * Javalin 使用的 JsonMapper 中的 ObjectMapper 对象
     */
    Key<ObjectMapper> OBJECT_MAPPER_KEY = new Key<>("__object_mapper_key");
    /**
     * Javalin 使用的 JsonMapper 对象
     */
    Key<JsonMapper> JSON_MAPPER_KEY = new Key<>("__json_mapper_key");
}
