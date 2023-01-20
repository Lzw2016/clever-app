package org.clever.core.task;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.SystemClock;
import org.clever.core.job.DaemonExecutor;
import org.clever.util.Assert;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统命令行任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 09:13 <br/>
 */
@Slf4j
public class CmdTask {
    public static String[] MACOS_CMD = new String[]{"/bin/sh", "-c"};
    public static String[] WINDOWS_CMD = new String[]{"cmd", "/q", "/c"};
    public static String[] LINUX_CMD = new String[]{"/bin/sh", "-c"};

    public static String MACOS_CHARSET = "UTF-8";
    public static String WINDOWS_CHARSET = "GBK";
    public static String LINUX_CHARSET = "UTF-8";

    private final File workDir;
    @Getter
    private final String name;
    private final StartupTaskConfig.CmdConfig cmd;
    @Getter
    private final boolean async;
    @Getter
    private final Duration interval;
    private Process proc;
    private BufferedReader in;
    private BufferedReader err;
    private PrintWriter out;
    private volatile boolean started = false;
    private final DaemonExecutor daemonExecutor;

    public CmdTask(String name, boolean async, Duration interval, File workDir, StartupTaskConfig.CmdConfig cmd) {
        Assert.notNull(workDir, "参数 workDir 不能为null");
        Assert.notNull(cmd, "参数 cmd 不能为null");
        this.name = StringUtils.trimToEmpty(name);
        this.async = async;
        this.interval = interval;
        this.workDir = workDir;
        this.cmd = cmd;
        if (interval != null && interval.toMillis() > 0) {
            daemonExecutor = new DaemonExecutor(StringUtils.isNotBlank(this.name) ? this.name : "CmdTask");
        } else {
            daemonExecutor = null;
        }
    }

    public void start() {
        Assert.isFalse(started, "任务已经启动，不可重复启动");
        started = true;
        Runnable runnable = () -> {
            long startTime = SystemClock.now();
            try {
                doStart();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            } finally {
                proc.destroyForcibly();
                int exitValue = proc.exitValue();
                if (exitValue != 0) {
                    log.error("命令行任务: [{}], 退出码: {}", this.name, exitValue);
                }
                IOUtils.closeQuietly(proc.getInputStream());
                IOUtils.closeQuietly(proc.getErrorStream());
                IOUtils.closeQuietly(proc.getOutputStream());
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(err);
                IOUtils.closeQuietly(out);
            }
            long cost = SystemClock.now() - startTime;
            if (daemonExecutor != null && cost > 8_000) {
                log.info("任务: [{}]执行完成,耗时: {}ms", name, cost);
            }
        };
        if (daemonExecutor == null) {
            if (async) {
                Thread thread = new Thread(runnable);
                thread.setName(StringUtils.isNotBlank(name) ? name : "CmdTask");
                thread.setDaemon(true);
                thread.start();
            } else {
                runnable.run();
            }
        } else {
            daemonExecutor.scheduleAtFixedRate(runnable, interval.toMillis());
            Runtime.getRuntime().addShutdownHook(new Thread(daemonExecutor::stop));
        }
    }

    private void doStart() throws IOException, InterruptedException {
        List<String> params = new ArrayList<>(4);
        params.addAll(getCmdArray());
        params.add(getCmd());
        proc = Runtime.getRuntime().exec(params.toArray(new String[0]), null, workDir);
        in = new BufferedReader(new InputStreamReader(proc.getInputStream(), getCharset()));
        err = new BufferedReader(new InputStreamReader(proc.getErrorStream(), getCharset()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())), true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            proc.destroyForcibly();
            int exitValue = proc.exitValue();
            if (exitValue != 0) {
                log.error("命令行任务: [{}], 退出码: {}", this.name, exitValue);
            }
        }));
        String inLine, errLine;
        while ((inLine = StringUtils.trim(in.readLine())) != null | (errLine = StringUtils.trim(err.readLine())) != null) {
            if (Thread.interrupted()) {
                break;
            }
            if (StringUtils.isNotBlank(inLine)) {
                log.debug(inLine);
            }
            if (StringUtils.isNotBlank(errLine)) {
                log.error(errLine);
            }
        }
        proc.waitFor();
    }

    public String getWorkDir() {
        return workDir.getAbsolutePath();
    }

    public String getCmd() {
        return getCmd(cmd);
    }

    protected List<String> getCmdArray() {
        String[] cmdArray;
        String osInfo = System.getProperty("os.name").toLowerCase();
        if (osInfo.contains("mac os")) {
            cmdArray = MACOS_CMD;
        } else if (osInfo.contains("windows")) {
            cmdArray = WINDOWS_CMD;
        } else {
            cmdArray = LINUX_CMD;
        }
        return Arrays.stream(cmdArray).collect(Collectors.toList());
    }

    protected String getCharset() {
        String charset;
        String osInfo = System.getProperty("os.name").toLowerCase();
        if (osInfo.contains("mac os")) {
            charset = MACOS_CHARSET;
        } else if (osInfo.contains("windows")) {
            charset = WINDOWS_CHARSET;
        } else {
            charset = LINUX_CHARSET;
        }
        if (StringUtils.isBlank(charset)) {
            charset = "UTF-8";
        }
        return charset;
    }

    public static String getCmd(StartupTaskConfig.CmdConfig cmdConfig) {
        String cmd;
        String osInfo = System.getProperty("os.name").toLowerCase();
        if (osInfo.contains("mac os")) {
            cmd = cmdConfig.getMacos();
        } else if (osInfo.contains("windows")) {
            cmd = cmdConfig.getWindows();
        } else {
            cmd = cmdConfig.getLinux();
        }
        return StringUtils.trimToEmpty(cmd);
    }
}
