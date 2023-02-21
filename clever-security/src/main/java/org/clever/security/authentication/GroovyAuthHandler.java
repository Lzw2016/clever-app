//package org.clever.security.authentication;
//
//import org.clever.groovy.intercept.GroovyHandlerContext;
//import org.clever.groovy.intercept.GroovyHandlerInterceptor;
//import org.clever.security.SecurityContextHolder;
//import org.clever.security.model.SecurityContext;
//
//import java.lang.reflect.Method;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/21 15:21 <br/>
// */
//public class GroovyAuthHandler implements GroovyHandlerInterceptor {
//    @Override
//    public boolean preHandle(GroovyHandlerContext context) {
//        final SecurityContext securityContext = SecurityContextHolder.getContext();
//        final Method handleMethod = context.getHandleMethod();
//        final Object[] args = context.getArgs();
//        final Class<?>[] parameterTypes = handleMethod.getParameterTypes();
//        for (int idx = 0; idx < parameterTypes.length; idx++) {
//            if (args[idx] != null) {
//                continue;
//            }
//            final Class<?> paramType = parameterTypes[idx];
//            if (SecurityContext.class.isAssignableFrom(paramType)) {
//                if (paramType.isInstance(securityContext)) {
//                    args[idx] = securityContext;
//                }
//            }
//        }
//        return true;
//    }
//}
