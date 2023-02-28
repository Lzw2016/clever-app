package org.clever.security.utils;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/31 14:50 <br/>
 */
public class SecurityRedisKey {
    /**
     * SecurityContext Redis key
     */
    public static String getSecurityContextKey(String redisNamespace, String userId) {
        return String.format("%s:context:uid_%s", redisNamespace, userId);
    }

    /**
     * Token Redis key
     */
    public static String getTokenKey(String redisNamespace, String userId, String tokenId) {
        return String.format("%s:token:uid_%s:tk_%s", redisNamespace, userId, tokenId);
    }

    /**
     * User Redis key
     */
    public static String getUserKey(String redisNamespace, String userId) {
        return String.format("%s:user:uid_%s", redisNamespace, userId);
    }
}
