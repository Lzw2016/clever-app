package org.clever.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.clever.web.FilterRegistrar;
import org.clever.web.utils.GlobalExceptionHandler;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 21:31 <br/>
 */
@Slf4j
public class ExceptionHandlerFilter implements FilterRegistrar.FilterFuc {
    public static final ExceptionHandlerFilter INSTANCE = new ExceptionHandlerFilter();

    public ExceptionHandlerFilter() {
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) {
        try {
            ctx.next();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            GlobalExceptionHandler.handle(e, ctx.req, ctx.res);
        }
    }
}
