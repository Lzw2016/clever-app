package org.clever.security.impl.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.security.model.LoginType;
import org.clever.security.model.request.AbstractLoginReq;

/**
 * 用户名/密码登录数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 14:34 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NamePasswordLoginReq extends AbstractLoginReq {
    /**
     * “登录名”参数名
     */
    public static final String LOGIN_NAME_PARAM_NAME = "loginName";
    /**
     * “登录密码”参数名
     */
    public static final String PASSWORD_PARAM_NAME = "password";
    /**
     * 用户登录名
     */
    @NotBlank(message = "登录名不能为空")
    private String loginName;
    /**
     * 密码
     */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    @Override
    public Integer getLoginType() {
        return LoginType.LoginNamePassword.getId();
    }
}
