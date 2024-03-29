package org.clever.security.model;

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;

/**
 * 登录渠道
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 15:11 <br/>
 */
@ToString
@Getter
public enum LoginChannel {
    Unknown(-1, "未知"),
    /**
     * PC 管理后台
     */
    PCAdmin(0, "PC-Admin"),
    /**
     * PC 业务端
     */
    PCWeb(1, "PC-Web"),
    /**
     * H5
     */
    H5(2, "H5"),
    /**
     * IOS-APP
     */
    IOS_APP(3, "IOS-APP"),
    /**
     * Android-APP
     */
    Android_APP(4, "Android-APP"),
    /**
     * 微信小程序
     */
    WechatSmallProgram(5, "WechatSmallProgram"),
    ;

    /**
     * 登录渠道 id
     */
    private final int id;
    /**
     * 登录渠道 name
     */
    private final String name;

    LoginChannel(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static LoginChannel lookup(Object obj) {
        String name = String.valueOf(obj);
        LoginChannel loginChannel = lookup(name);
        if (loginChannel == null) {
            loginChannel = lookup(NumberUtils.toInt(name, -1));
        }
        return loginChannel;
    }

    public static LoginChannel lookup(String name) {
        for (LoginChannel loginChannel : LoginChannel.values()) {
            if (loginChannel.getName().equalsIgnoreCase(name)) {
                return loginChannel;
            }
        }
        return null;
    }

    public static LoginChannel lookup(int id) {
        for (LoginChannel loginChannel : LoginChannel.values()) {
            if (Objects.equals(loginChannel.getId(), id)) {
                return loginChannel;
            }
        }
        return null;
    }
}
