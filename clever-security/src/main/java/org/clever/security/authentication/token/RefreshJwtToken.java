package org.clever.security.authentication.token;

import org.clever.security.model.NewJwtToken;
import org.clever.security.model.UseJwtRefreshToken;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/30 19:17 <br/>
 */
public interface RefreshJwtToken {
    /**
     * 使用JWT的刷新Token续期Token
     *
     * @param useJwtRefreshToken 使用JWT刷新Token的上下文信息
     * @return 新的JWT Token
     */
    NewJwtToken refresh(UseJwtRefreshToken useJwtRefreshToken);
}
