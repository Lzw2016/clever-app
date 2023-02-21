//package org.clever.security.authorization;
//
//import lombok.extern.slf4j.Slf4j;
//import org.clever.security.authorization.voter.AuthorizationVoter;
//import org.clever.security.authorization.voter.VoterResult;
//import org.clever.security.config.SecurityConfig;
//import org.clever.security.model.SecurityContext;
//
//import javax.servlet.http.HttpServletRequest;
//import java.lang.reflect.Method;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/17 14:42 <br/>
// */
//@Slf4j
//public class GroovyAuthorizationVoter implements AuthorizationVoter {
//    @Override
//    public boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext) {
//        Object matcherPath = request.getAttribute(HandlerMapping.GROOVY_MATCHER_PATH);
//        return matcherPath != null;
//    }
//
//    @Override
//    public VoterResult vote(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext) {
//        Object handleMethod = request.getAttribute(HandlerMapping.GROOVY_HANDLE_METHOD);
//        if (!(handleMethod instanceof Method)) {
//            return VoterResult.ABSTAIN;
//        }
//        boolean hasPermission = CheckPermissionUtils.hasPermission((Method) handleMethod, securityContext);
//        // log.debug("### GroovyHandle授权结果:{} | {}", hasPermission, request.getRequestURI());
//        return hasPermission ? VoterResult.PASS : VoterResult.REJECT;
//    }
//
//    @Override
//    public double getOrder() {
//        return AuthorizationVoter.super.getOrder();
//    }
//}
