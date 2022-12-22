package org.clever.web.filter;

import org.clever.web.FilterRegistrar;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 21:58 <br/>
 */
public class CorsFilter implements FilterRegistrar.FilterFuc {
    public static final CorsFilter INSTANCE = new CorsFilter();

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws Exception {
        ctx.next();
    }
}
