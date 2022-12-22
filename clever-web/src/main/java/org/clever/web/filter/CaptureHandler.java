package org.clever.web.filter;

import org.clever.web.FilterRegistrar;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 22:10 <br/>
 */
public class CaptureHandler implements FilterRegistrar.FilterFuc {
    public static final CaptureHandler INSTANCE = new CaptureHandler();

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws Exception {
        ctx.next();
    }
}
