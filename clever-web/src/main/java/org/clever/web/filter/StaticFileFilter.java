package org.clever.web.filter;

import org.clever.web.FilterRegistrar;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 22:00 <br/>
 */
public class StaticFileFilter implements FilterRegistrar.FilterFuc {
    public static final StaticFileFilter INSTANCE = new StaticFileFilter();

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws Exception {
        ctx.next();
    }
}
