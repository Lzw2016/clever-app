package org.clever.web.mvc.interceptor;

import org.clever.core.OrderIncrement;
import org.clever.core.validator.BaseValidatorUtils;
import org.clever.web.mvc.HandlerContext;
import org.clever.web.mvc.annotation.Validated;
import org.springframework.core.MethodParameter;

/**
 * HandlerMethod 参数 JSR-303 数据验证实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/20 22:44 <br/>
 */
public class ArgumentsValidated implements HandlerInterceptor {
    private static final Class<org.springframework.validation.annotation.Validated> SPRING_ANNOTATION = org.springframework.validation.annotation.Validated.class;

    @Override
    public boolean beforeHandle(HandlerContext context) {
        final MethodParameter[] parameters = context.getHandleMethod().getParameters();
        final Object[] args = context.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            Class<?>[] clazzArr;
            Validated validated = parameter.getParameterAnnotation(Validated.class);
            if (validated == null) {
                org.springframework.validation.annotation.Validated springValidated = parameter.getParameterAnnotation(SPRING_ANNOTATION);
                if (springValidated == null) {
                    continue;
                } else {
                    clazzArr = springValidated.value();
                }
            } else {
                clazzArr = validated.value();
            }
            // 读取 Validated 注解验证参数
            BaseValidatorUtils.validateThrowException(arg, clazzArr);
        }
        return true;
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MIN;
    }
}
