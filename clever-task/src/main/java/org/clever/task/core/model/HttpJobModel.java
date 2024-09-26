package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.Assert;
import org.clever.core.mapper.JacksonMapper;
import org.clever.task.core.model.entity.TaskHttpJob;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:06 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HttpJobModel extends AbstractJob {
    /**
     * http请求method，ALL GET HEAD POST PUT DELETE CONNECT OPTIONS TRACE PATCH
     */
    private String requestMethod;
    /**
     * Http请求地址
     */
    private String requestUrl;
    /**
     * Http请求数据json格式，包含：params、headers、body
     */
    private HttpRequestData requestData;

    public HttpJobModel(String name, String requestMethod, String requestUrl) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(requestMethod, "参数requestMethod不能为空");
        Assert.hasText(requestUrl, "参数requestUrl不能为空");
        this.name = name;
        this.requestMethod = requestMethod;
        this.requestUrl = requestUrl;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_1;
    }

    public TaskHttpJob toJobEntity() {
        TaskHttpJob httpJob = new TaskHttpJob();
        httpJob.setRequestMethod(getRequestMethod());
        httpJob.setRequestUrl(getRequestUrl());
        if (getRequestData() != null) {
            httpJob.setRequestData(getRequestData().toString());
        }
        return httpJob;
    }

    @Data
    public static final class HttpRequestData implements Serializable {
        private final LinkedHashMap<String, String> params = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        private final LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        // private LinkedHashMap<String, HttpCookie> cookies;

        @Override
        public String toString() {
            return JacksonMapper.getInstance().toJson(this);
        }

        public HttpRequestData addParam(String name, String value) {
            params.put(name, value);
            return this;
        }

        public HttpRequestData removeParam(String name) {
            params.remove(name);
            return this;
        }

        public String getParam(String name) {
            params.get(name);
            return null;
        }

        public HttpRequestData addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public HttpRequestData removeHeader(String name) {
            headers.remove(name);
            return this;
        }

        public String getHeader(String name) {
            headers.get(name);
            return null;
        }

        public HttpRequestData addBody(String name, Object value) {
            body.put(name, value);
            return this;
        }

        public HttpRequestData removeBody(String name) {
            body.remove(name);
            return this;
        }

        public Object getBody(String name) {
            body.get(name);
            return null;
        }
    }
}
