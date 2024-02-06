package org.clever.js.nashorn.internal;

import lombok.SneakyThrows;
import org.clever.js.api.internal.Logger;
import org.clever.js.api.internal.LoggerFactory;
import org.clever.js.nashorn.support.NashornObjectToString;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 13:06 <br/>
 */
public class NashornLoggerFactory extends LoggerFactory {
    public static final NashornLoggerFactory INSTANCE = new NashornLoggerFactory();

    protected NashornLoggerFactory() {
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
            logger.setObjectToString(NashornObjectToString.INSTANCE);
            return logger;
        });
    }
}
