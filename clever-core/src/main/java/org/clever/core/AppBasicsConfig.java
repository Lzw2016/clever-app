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
    public static AppBasicsConfig init(Environment environment) {
        AppBasicsConfig config = Binder.get(environment).bind(AppBasicsConfig.PREFIX, AppBasicsConfig.class).orElseGet(AppBasicsConfig::new);
        AnsiOutput.setEnabled(config.ansi);
        AnsiOutput.setConsoleAvailable(config.consoleAvailable);
        AppContextHolder.registerBean("rootPath", config.rootPath, true);
        AppContextHolder.registerBean("appBasicsConfig", config, true);
        List<String> logs = new ArrayList<>();
        logs.add("rootPath        : " + ResourcePathUtils.getAbsolutePath(config.rootPath, ""));
        logs.add("ansi            : " + config.ansi);
        logs.add("consoleAvailable: " + config.consoleAvailable);
        BannerUtils.printConfig(log, "应用配置", logs.toArray(new String[0]));
        return config;
    }
}
