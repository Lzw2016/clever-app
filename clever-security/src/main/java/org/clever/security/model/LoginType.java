package org.clever.security.model;

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;

/**
 * 登录方式
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 15:24 <br/>
 */
@ToString
@Getter
public enum LoginType {
    /**
     * 用户名密码
     */
    LoginNamePassword(1, "LoginNamePassword"),
    /**
     * 手机号验证码
     */
    SmsValidateCode(2, "SmsValidateCode"),
    /**
     * 邮箱验证码
     */
    EmailValidateCode(3, "EmailValidateCode"),
    /**
     * 刷新令牌
     */
    RefreshToken(4, "RefreshToken"),
    /**
     * 微信小程序
     */
    WechatSmallProgram(5, "WechatSmallProgram"),
    /**
     * 扫码登录
     */
    ScanCode(6, "ScanCode"),
    ;

    /**
     * 登录方式 id
     */
    private final int id;
    /**
     * 登录方式 name
     */
    private final String name;

    LoginType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static LoginType lookup(Object obj) {
        String name = String.valueOf(obj);
        LoginType loginType = lookup(name);
        if (loginType == null) {
            loginType = lookup(NumberUtils.toInt(name, -1));
        }
        return loginType;
    }

    private static LoginType lookup(String name) {
        for (LoginType loginType : LoginType.values()) {
            if (loginType.getName().equalsIgnoreCase(name)) {
                return loginType;
            }
        }
        return null;
    }

    private static LoginType lookup(int id) {
        for (LoginType loginType : LoginType.values()) {
            if (Objects.equals(loginType.getId(), id)) {
                return loginType;
            }
        }
        return null;
    }
}
