package org.clever.web.support.mvc.interceptor;

import org.clever.core.MethodParameter;
import org.clever.core.OrderIncrement;
import org.clever.core.validator.BaseValidatorUtils;
import org.clever.web.support.mvc.HandlerContext;
import org.clever.web.support.mvc.annotation.Validated;

/**
 * HandlerMethod 参数 JSR-303 数据验证实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/20 22:44 <br/>
 */
public class ArgumentsValidated implements HandlerInterceptor {
    @Override
    public boolean beforeHandle(HandlerContext context) throws Exception {
        final MethodParameter[] parameters = context.getHandleMethod().getParameters();
        final Object[] args = context.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            Object arg = args[i];
            Validated validated = parameter.getParameterAnnotation(Validated.class);
            if (validated == null || arg == null) {
                continue;
            }
            Class<?>[] clazzArr = validated.value();
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
