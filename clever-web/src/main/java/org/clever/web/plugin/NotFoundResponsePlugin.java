package org.clever.web.plugin;

import io.javalin.config.JavalinConfig;
import io.javalin.plugin.Plugin;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.DateUtils;
import org.clever.core.model.response.ErrorResponse;
import org.jetbrains.annotations.NotNull;

/**
 * 404异常处理插件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/23 19:06 <br/>
 */
public class NotFoundResponsePlugin extends Plugin<Void> {
    public static final NotFoundResponsePlugin INSTANCE = new NotFoundResponsePlugin();

    private NotFoundResponsePlugin() {
    }

    @Override
    public void onStart(@NotNull JavalinConfig config) {
        config.router.mount(javalinDefaultRouting -> javalinDefaultRouting.error(404, ctx -> {
                final String contentType = StringUtils.trimToEmpty(ctx.contentType()).toLowerCase();
                ctx.res().setStatus(404);
                if (contentType.contains("json")) {
                    ErrorResponse res = new ErrorResponse();
                    res.setError("Not Found");
                    res.setException("NotFoundResponse");
                    res.setPath(ctx.req().getRequestURI());
                    res.setStatus(404);
                    res.setMessage("Not Found");
                    ctx.json(res);
                    return;
                }
                ctx.html("<html>" +
                    "  <body>" +
                    "    <h1>Whitelabel Error Page</h1>" +
                    "    <p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>" +
                    "    <div id='created'>" + DateUtils.getCurrentDate(DateUtils.yyyy_MM_dd_HH_mm_ss) + "</div>" +
                    "    <div>There was an unexpected error (type=Not Found, status=404).</div>" +
                    "  </body>" +
                    "</html>"
                );
            })
        );
    }
}
