package org.clever.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用基础配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/20 22:06 <br/>
 */
@ConfigurationProperties(prefix = AppBasicsConfig.PREFIX)
@Data
@Slf4j
public class AppBasicsConfig {
    public static AppBasicsConfig create(Environment environment) {
        AppBasicsConfig appBasicsConfig = Binder.get(environment).bind(AppBasicsConfig.PREFIX, AppBasicsConfig.class).orElseGet(AppBasicsConfig::new);
        AppContextHolder.registerBean("rootPath", appBasicsConfig.rootPath, true);
        AppContextHolder.registerBean("appBasicsConfig", appBasicsConfig, true);
        return appBasicsConfig;
    }

    public static final String PREFIX = "app";
    /**
     * 应用根路径配置
     */
    private String rootPath = "./";
    /**
     * 日志输出是否高亮
     */
    private AnsiOutput.Enabled ansi = AnsiOutput.Enabled.ALWAYS;
    /**
     * 控制台是否可用
     */
    private Boolean consoleAvailable = true;

    /**
     * 初始化应用基础配置
     */
    public void init() {
        AnsiOutput.setEnabled(this.ansi);
        AnsiOutput.setConsoleAvailable(this.consoleAvailable);
        List<String> logs = new ArrayList<>();
        logs.add("rootPath        : " + ResourcePathUtils.getAbsolutePath(this.rootPath, ""));
        logs.add("ansi            : " + this.ansi);
        logs.add("consoleAvailable: " + this.consoleAvailable);
        BannerUtils.printConfig(log, "应用配置", logs.toArray(new String[0]));
    }
}
