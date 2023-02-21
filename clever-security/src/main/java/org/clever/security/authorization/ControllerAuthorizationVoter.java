//package org.clever.security.authorization;
//
//import org.clever.security.authorization.voter.VoterResult;
//import org.clever.security.exception.AuthorizationInnerException;
//import org.clever.security.utils.CheckPermissionUtils;
//import org.clever.web.utils.SpringMvcHandlerUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.clever.security.authorization.voter.AuthorizationVoter;
//import org.clever.security.config.SecurityConfig;
//import org.clever.security.model.SecurityContext;
//import org.clever.util.Assert;
//import org.clever.web.support.mvc.HandlerMethod;
//import org.springframework.util.Assert;
//import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
//
//import javax.servlet.http.HttpServletRequest;
//
///**
// * Spring MVC Controller访问权限投票
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2020/12/06 20:27 <br/>
// */
//@Slf4j
//public class ControllerAuthorizationVoter implements AuthorizationVoter {
//    /**
//     * Spring内置的根据request获取Controller Method的工具对象
//     */
//    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
//
//    public ControllerAuthorizationVoter(RequestMappingHandlerMapping requestMappingHandlerMapping) {
//        Assert.notNull(requestMappingHandlerMapping, "参数 requestMappingHandlerMapping 不能为 null");
//        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
//    }
//
//    @Override
//    public double getOrder() {
//        return 100;
//    }
//
//    @Override
//    public VoterResult vote(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext) {
//        HandlerMethod handlerMethod;
//        try {
//            handlerMethod = SpringMvcHandlerUtils.getHandlerMethod(requestMappingHandlerMapping, request);
//        } catch (Exception e) {
//            throw new AuthorizationInnerException("获取HandlerExecutionChain异常");
//        }
//        if (handlerMethod == null) {
//            // 弃权
//            return VoterResult.ABSTAIN;
//        }
//        boolean hasPermission = CheckPermissionUtils.hasPermission(handlerMethod.getMethod(), securityContext);
//        // log.debug("### ControllerHandle授权结果:{} | {}", hasPermission, request.getRequestURI());
//        return hasPermission ? VoterResult.PASS : VoterResult.REJECT;
//    }
//}
