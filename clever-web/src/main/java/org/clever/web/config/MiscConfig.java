package org.clever.web.config;

import io.javalin.core.JavalinConfig;
import lombok.Data;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 13:34 <br/>
 */
@Data
public class MiscConfig {
    /**
     * 是否输出 Javalin Banner 日志
     */
    private boolean showJavalinBanner = false;

    /**
     * 应用当前配置到 JavalinConfig
     */
    public void apply(JavalinConfig config) {
        Assert.notNull(config, "参数 config 不能为空");
        MiscConfig misc = this;
        config.showJavalinBanner = misc.isShowJavalinBanner();
    }
}
