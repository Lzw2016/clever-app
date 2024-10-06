package org.clever.security.login;

import jakarta.servlet.http.HttpServletRequest;
import org.clever.core.OrderIncrement;
import org.clever.core.Ordered;
import org.clever.security.config.TokenConfig;
import org.clever.security.model.UserInfo;

import java.util.Map;

/**
 * 创建JWT-Token时加入扩展数据
 * 作者：lizw <br/>
 * 创建时间：2020/12/02 22:54 <br/>
 */
public interface AddJwtTokenExtData extends Ordered {
    /**
     * 向JWT-Token中加入自定义扩展数据
     *
     * @param request     请求对象
     * @param tokenConfig Token配置
     * @param userInfo    用户信息
     * @param extData     扩展数据
     * @return 扩展数据
     */
    Map<String, Object> addExtData(HttpServletRequest request, TokenConfig tokenConfig, UserInfo userInfo, Map<String, Object> extData);

    @Override
    default double getOrder() {
        return OrderIncrement.NORMAL;
    }
}
