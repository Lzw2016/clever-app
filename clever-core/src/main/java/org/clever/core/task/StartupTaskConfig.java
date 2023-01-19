package org.clever.core.task;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统启动后的命令行任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 09:14 <br/>
 */
@Data
public class StartupTaskConfig {
    public static final String PREFIX = "startup-task";
    /**
     * 定时任务
     */
    private List<TimedTaskConfig> timedTask = new ArrayList<>();
    /**
     * 命令行任务
     */
    private List<CmdTaskConfig> cmdTask = new ArrayList<>();

    @Data
    public static class TimedTaskConfig {
        /**
         * 任务名称
         */
        private String name = "定时任务";
        /**
         * 是否启用当前任务,默认: true
         */
        private boolean enable = true;
        /**
         * 时执行此任务的时间间隔,默认60秒
         */
        private Duration interval = Duration.ofSeconds(60);
        /**
         * class名称
         */
        private String clazz;
        /**
         * method名称
         */
        private String method;
    }

    @Data
    public static class CmdTaskConfig {
        /**
         * 任务名称
         */
        private String name = "开机Command任务";
        /**
         * Command任务的工作目录
         */
        private String workDir = "./";
        /**
         * 执行的命令行
         */
        private CmdConfig cmd;
        /**
         * 是否启用当前任务,默认: true
         */
        private boolean enable = true;
        /**
         * 是否异步执行任务,默认: true
         */
        private boolean async = true;
        /**
         * 时执行此任务的时间间隔,默认不定时执行
         */
        private Duration interval = null;
    }

    @Data
    public static class CmdConfig {
        /**
         * windows 系统的命令行
         */
        private String windows;
        /**
         * linux 系统的命令行
         */
        private String linux;
        /**
         * macos 系统的命令行
         */
        private String macos;

        public void setCmd(String cmd) {
            this.windows = cmd;
            this.linux = cmd;
            this.macos = cmd;
        }
    }
}
