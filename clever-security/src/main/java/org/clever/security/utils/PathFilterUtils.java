package org.clever.security.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.http.HttpServletRequestUtils;
import org.clever.security.config.LoginConfig;
import org.clever.security.config.LogoutConfig;
import org.clever.security.config.SecurityConfig;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/06 12:14 <br/>
 */
public class PathFilterUtils {
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static String getPath(HttpServletRequest request) {
        return HttpServletRequestUtils.getPathWithoutContextPath(request);
    }

    /**
     * 当前请求是否是获取图片验证码请求
     */
    public static boolean isLoginCaptchaPath(HttpServletRequest request, SecurityConfig securityConfig) {
        final String path = getPath(request);
        return isLoginCaptchaPath(path, securityConfig);
    }

    public static boolean isLoginCaptchaPath(String path, SecurityConfig securityConfig) {
        return false;
        // LoginConfig login = securityConfig.getLogin();
        // if (login == null) {
        //     return false;
        // }
        // LoginCaptchaConfig loginCaptcha = login.getLoginCaptcha();
        // if (loginCaptcha == null || !loginCaptcha.isNeedCaptcha()) {
        //     return false;
        // }
        // return Objects.equals(loginCaptcha.getLoginCaptchaPath(), path);
    }

    /**
     * 当前请求是否是登录请求
     */
    public static boolean isLoginRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        final String path = getPath(request);
        return isLoginRequest(path, request.getMethod(), securityConfig);
    }

    public static boolean isLoginRequest(String path, String method, SecurityConfig securityConfig) {
        LoginConfig login = securityConfig.getLogin();
        if (login == null) {
            return false;
        }
        // 支持以"/"结尾的请求
        if (StringUtils.endsWith(path, "/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (login.getPaths() == null || !login.getPaths().contains(path)) {
            return false;
        }
        boolean postRequest = StringUtils.isBlank(method) || HttpMethod.POST.matches(method);
        return !login.isPostOnly() || postRequest;
    }

    /**
     * 当前请求是否是登录请求
     */
    public static boolean isLogoutRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        final String path = getPath(request);
        return isLogoutRequest(path, securityConfig);
    }

    public static boolean isLogoutRequest(String path, SecurityConfig securityConfig) {
        LogoutConfig logout = securityConfig.getLogout();
        if (logout == null) {
            return false;
        }
        // 支持以"/"结尾的请求
        if (StringUtils.endsWith(path, "/")) {
            path = path.substring(0, path.length() - 1);
        }
        return Objects.equals(logout.getPath(), path);
    }

    /**
     * 当前请求是否是注册请求
     */
    public static boolean isRegisterRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        final String path = getPath(request);
        return isRegisterRequest(path, securityConfig);
    }

    public static boolean isRegisterRequest(String path, SecurityConfig securityConfig) {
        return false;
        // UserRegisterConfig register = securityConfig.getRegister();
        // if (register == null) {
        //     return false;
        // }
        // return Objects.equals(register.getRegisterPath(), path);
    }

    /**
     * 当前请求是否是获取当前登录用户信息
     */
    public static boolean isGetCurrentUserRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        String path = getPath(request);
        // 支持以"/"结尾的请求
        if (StringUtils.endsWith(path, "/")) {
            path = path.substring(0, path.length() - 1);
        }
        return isGetCurrentUserRequest(path, securityConfig);
    }

    public static boolean isGetCurrentUserRequest(String path, SecurityConfig securityConfig) {
        return Objects.equals(securityConfig.getCurrentUserPath(), path);
    }

    /**
     * 当前请求是否需要身份认证
     */
    public static boolean isAuthenticationRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        // 不需要认证的Path
        final String path = getPath(request);
        return isAuthenticationRequest(path, request.getMethod(), securityConfig);
    }

    public static boolean isAuthenticationRequest(String path, String method, SecurityConfig securityConfig) {
        // 当前请求是“登录请求”或“验证码请求”或“注册请求”或“密码找回”
        if (isLoginRequest(path, method, securityConfig)
                || isLoginCaptchaPath(path, securityConfig)
                || isRegisterRequest(path, securityConfig)) {
            return false;
        }
        List<String> ignorePaths = securityConfig.getIgnorePaths();
        if (ignorePaths == null || ignorePaths.isEmpty()) {
            return true;
        }
        for (String ignorePath : ignorePaths) {
            if (ANT_PATH_MATCHER.match(ignorePath, path)) {
                // 忽略当前路径
                return false;
            }
        }
        return true;
    }

    /**
     * 当前请求是否需要授权
     */
    public static boolean isAuthorizationRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        // 不需要授权的Path
        final String path = getPath(request);
        return isAuthorizationRequest(path, request.getMethod(), securityConfig);
    }

    public static boolean isAuthorizationRequest(String path, String method, SecurityConfig securityConfig) {
        // 当前请求不需要身份认证 - 那就更不需要授权了
        if (!isAuthenticationRequest(path, method, securityConfig)) {
            return false;
        }
        // 获取当前登录用户信息
        if (isGetCurrentUserRequest(path, securityConfig)) {
            return false;
        }
        List<String> ignoreAuthPaths = securityConfig.getIgnoreAuthPaths();
        if (ignoreAuthPaths == null || ignoreAuthPaths.isEmpty()) {
            return true;
        }
        for (String ignorePath : ignoreAuthPaths) {
            if (ANT_PATH_MATCHER.match(ignorePath, path)) {
                // 忽略当前路径
                return false;
            }
        }
        return true;
    }

    /**
     * 认证或授权失败时不拦截的请求
     */
    public static boolean isIgnoreAuthFailedRequest(HttpServletRequest request, SecurityConfig securityConfig) {
        // 不需要授权的Path
        final String path = getPath(request);
        return isIgnoreAuthFailedRequest(path, request.getMethod(), securityConfig);
    }

    public static boolean isIgnoreAuthFailedRequest(String path, String method, SecurityConfig securityConfig) {
        for (String ignorePath : securityConfig.getIgnoreAuthFailedPaths()) {
            if (ANT_PATH_MATCHER.match(ignorePath, path)) {
                return true;
            }
        }
        return false;
    }
}
