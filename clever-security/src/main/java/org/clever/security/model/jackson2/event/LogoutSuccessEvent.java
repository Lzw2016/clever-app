package org.clever.security.model.jackson2.event;

import io.jsonwebtoken.Claims;
import lombok.Data;
import org.clever.security.model.SecurityContext;

import java.io.Serializable;

/**
 * 登出成功事件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:15 <br/>
 */
@Data
public class LogoutSuccessEvent implements Serializable {
    /**
     * 安全上下文(用户信息)
     */
    private final SecurityContext securityContext;
    /**
     * JWT-Token对象
     */
    private final Claims claims;

    public LogoutSuccessEvent(SecurityContext securityContext, Claims claims) {
        this.securityContext = securityContext;
        this.claims = claims;
    }
}
