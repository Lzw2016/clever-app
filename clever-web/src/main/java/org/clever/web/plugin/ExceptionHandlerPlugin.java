package org.clever.web.plugin;

import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import org.clever.web.utils.GlobalExceptionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/27 22:11 <br/>
 */
public class ExceptionHandlerPlugin implements Plugin {
    public static final ExceptionHandlerPlugin INSTANCE = new ExceptionHandlerPlugin();

    private ExceptionHandlerPlugin() {
    }

    @Override
    public void apply(@NotNull Javalin app) {
        app.exception(Exception.class, (exception, ctx) -> GlobalExceptionHandler.handle(exception, ctx.req, ctx.res));
    }
}
