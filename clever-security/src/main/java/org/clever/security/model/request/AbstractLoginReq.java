package org.clever.security.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.BaseRequest;
import org.clever.security.model.LoginType;

/**
 * 登录请求数据抽象类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 14:32 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbstractLoginReq extends BaseRequest {
    /**
     * 登录类型参数名
     */
    public static final String LOGIN_TYPE_PARAM_NAME = "loginType";
    /**
     * 登录渠道参数名
     */
    public static final String LOGIN_CHANNEL_PARAM_NAME = "loginChannel";

    /**
     * 登录渠道
     */
    private String loginChannel;

    /**
     * 登录方式
     */
    public abstract LoginType getLoginType();
}
