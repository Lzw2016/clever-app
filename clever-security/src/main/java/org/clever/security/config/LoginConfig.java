package org.clever.security.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录配置
 * 作者： lzw<br/>
 * 创建时间：2019-04-25 19:01 <br/>
 */
@Data
public class LoginConfig implements Serializable {
    /**
     * 登录请求Path
     */
    private String path = "/login";
    /**
     * 登录只支持POST请求
     */
    private boolean postOnly = true;
    /**
     * 隐藏登录用户不存在的异常
     */
    private boolean hideUserNotFoundException = true;
    /**
     * 登录成功 - 是否需要重定向到指定页面
     */
    private boolean successNeedRedirect = false;
    /**
     * 登录成功重定向的地址
     */
    private String successRedirectPage = "/home.html";
    /**
     * 登录失败 - 是否需要重定向
     */
    private boolean failureNeedRedirect = false;
    /**
     * 登录失败跳转地址
     */
    private String failureRedirectPage = "/index.html";
    /**
     * 是否允许重复登录(在登录状态下登录其他账号)
     */
    private boolean allowRepeatLogin = true;
    /**
     * 同一个用户并发登录次数限制(小于等于0表示不限制)
     */
    private int concurrentLoginCount = -1;
    /**
     * 同一个用户并发登录次数达到最大值之后,是否后登陆的挤下前登录的(false表示登录次数达到最大值后,之后的用户无法登录)
     */
    private boolean allowAfterLogin = true;
}
