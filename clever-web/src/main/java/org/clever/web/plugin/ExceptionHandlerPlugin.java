package org.clever.web.plugin;

import io.javalin.config.JavalinConfig;
import io.javalin.plugin.Plugin;
import org.clever.web.utils.GlobalExceptionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/27 22:11 <br/>
 */
public class ExceptionHandlerPlugin extends Plugin<Void> {
    public static final ExceptionHandlerPlugin INSTANCE = new ExceptionHandlerPlugin();

    private ExceptionHandlerPlugin() {
    }

    @Override
    public void onStart(@NotNull JavalinConfig config) {
        config.router.mount(javalinDefaultRouting -> javalinDefaultRouting.exception(Exception.class, (exception, ctx) -> GlobalExceptionHandler.handle(exception, ctx.req(), ctx.res())));
    }
}
