package org.clever.js.graaljs.internal;

import lombok.SneakyThrows;
import org.clever.js.api.internal.Logger;
import org.clever.js.api.internal.LoggerFactory;
import org.clever.js.graaljs.support.GraalObjectToString;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 15:20 <br/>
 */
public class GraalLoggerFactory extends LoggerFactory {
    public static final GraalLoggerFactory INSTANCE = new GraalLoggerFactory();

    protected GraalLoggerFactory() {
    }

    /**
     * 获取日志对象
     *
     * @param name 名称
     */
    @SneakyThrows
    public Logger getLogger(String name) {
        return LOGGER_CACHE.get(name, () -> {
            Logger logger = new Logger(name);
            logger.setObjectToString(GraalObjectToString.INSTANCE);
            return logger;
        });
    }
}
