package org.clever.security;

import io.jsonwebtoken.Claims;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/01/21 20:08 <br/>
 */
public class JwtTokenHolder {
    private static final ThreadLocal<Claims> JWT_CONTEXT = new ThreadLocal<>();

    public static void clear() {
        JWT_CONTEXT.remove();
    }

    public static Claims get() {
        return JWT_CONTEXT.get();
    }

    public static void set(Claims claims) {
        JWT_CONTEXT.set(claims);
    }
}
