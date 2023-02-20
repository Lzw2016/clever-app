package org.clever.security.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.BaseRequest;
import org.clever.security.model.LoginType;

/**
 * 用户登录请求数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 14:32 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbstractUserLoginReq extends BaseRequest {
    public static final String LOGIN_TYPE_PARAM_NAME = "loginType";
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
