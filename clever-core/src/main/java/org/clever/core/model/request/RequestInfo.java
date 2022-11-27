package org.clever.core.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 记录请求信息<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-5-8 21:32 <br/>
 */
@Data
public class RequestInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 登录名
     */
    private String loginName;
    /**
     * 请求时间
     */
    private Date requestTime;
    /**
     * 请求URI
     */
    private String requestUri;
    /**
     * 操作方式,POST、GET...
     */
    private String method;
    /**
     * 请求参数数据
     */
    private String params;
    /**
     * 请求处理时间
     */
    private Long processTime;
    /**
     * 客户端的IP地址
     */
    private String remoteAddr;
    /**
     * 用户代理
     */
    private String userAgent;
    /**
     * 是否有异常（0：否；1：是）
     */
    private Integer hasException;
    /**
     * 异常信息
     */
    private String exceptionInfo;

    @Override
    public String toString() {
        return "RequestInfo{" +
                "loginName='" + loginName + '\'' +
                ", requestTime=" + requestTime +
                ", requestUri='" + requestUri + '\'' +
                ", method='" + method + '\'' +
                ", params='" + params + '\'' +
                ", processTime=" + processTime +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", hasException='" + hasException + '\'' +
                ", exceptionInfo='" + (exceptionInfo == null ? "0" : exceptionInfo.length()) + '\'' +
                '}';
    }
}
