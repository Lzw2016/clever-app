package org.clever.security.utils;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/31 14:50 <br/>
 */
public class SecurityRedisKey {
    public static String getSecurityContextKey(String redisNamespace, String userId) {
        return String.format("%s:context:uid_%s", redisNamespace, userId);
    }

    public static String getTokenKey(String redisNamespace, String userId, String tokenId) {
        return String.format("%s:token:uid_%s:tk_%s", redisNamespace, userId, tokenId);
    }

//    public static String getLoginAgentKey(String redisNamespace, Long loginId) {
//        return String.format("%s:login-agent:login_id_%s", redisNamespace, loginId);
//    }

    public static String getUserKey(String redisNamespace, String userId) {
        return String.format("%s:user:uid_%s", redisNamespace, userId);
    }
}
