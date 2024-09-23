//package org.clever.web.http;
//
//import org.apache.commons.lang3.StringUtils;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2022/12/23 11:06 <br/>
// */
//public enum HttpMethod {
//    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;
//
//    private static final Map<String, HttpMethod> mappings = new HashMap<>(16);
//
//    static {
//        for (HttpMethod httpMethod : values()) {
//            mappings.put(httpMethod.name(), httpMethod);
//        }
//    }
//
//    /**
//     * 将给定的方法值解析为 {@code HttpMethod}
//     */
//    public static HttpMethod resolve(String method) {
//        return (method != null ? mappings.get(method) : null);
//    }
//
//    /**
//     * 确定此 {@code HttpMethod} 是否与给定的方法值匹配
//     */
//    public boolean matches(String method) {
//        return name().equals(StringUtils.upperCase(method));
//    }
//}
