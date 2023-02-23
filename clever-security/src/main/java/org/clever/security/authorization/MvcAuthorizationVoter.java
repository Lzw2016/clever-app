package org.clever.security.authorization;

import lombok.extern.slf4j.Slf4j;
import org.clever.security.annotation.CheckPermission;
import org.clever.security.authorization.voter.AuthorizationVoter;
import org.clever.security.authorization.voter.VoterResult;
import org.clever.security.config.SecurityConfig;
import org.clever.security.model.SecurityContext;
import org.clever.security.utils.CheckPermissionUtils;
import org.clever.web.filter.MvcHandlerMethodFilter;
import org.clever.web.support.mvc.HandlerMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * 为 MVC 对应的 HandlerMethod 检查授权, MVC Method{@link CheckPermission} 注解的支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/17 14:42 <br/>
 */
@Slf4j
public class MvcAuthorizationVoter implements AuthorizationVoter {
    @Override
    public boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext) {
        return MvcHandlerMethodFilter.getHandleMethod(request) != null;
    }

    @Override
    public VoterResult vote(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext) {
        HandlerMethod handleMethod = MvcHandlerMethodFilter.getHandleMethod(request);
        if (handleMethod == null || handleMethod.getMethod() == null) {
            return VoterResult.ABSTAIN;
        }
        boolean hasPermission = CheckPermissionUtils.hasPermission(handleMethod.getMethod(), securityContext);
        log.debug("### GroovyHandle授权结果:{} | {}", hasPermission, request.getRequestURI());
        return hasPermission ? VoterResult.PASS : VoterResult.REJECT;
    }

    @Override
    public double getOrder() {
        return AuthorizationVoter.super.getOrder();
    }
}
