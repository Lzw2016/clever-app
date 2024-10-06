package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.PlatformOS;
import org.clever.core.exception.ExceptionUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/04 11:52 <br/>
 */
@Slf4j
public class ExecShellUtils {
    private static final long DEF_COMMAND_TIMEOUT = 60 * 10;
    private static final File DEF_WORKING_DIR;

    static {
        final String dir = "shell_job_log";
        String workingDir;
        try {
            workingDir = new File("./").getAbsolutePath();
            workingDir = FilenameUtils.concat(workingDir, dir);
            FileUtils.forceMkdir(new File(workingDir));
        } catch (Exception e) {
            log.warn("创建ShellJob工作目录失败", e);
            try {
                workingDir = FilenameUtils.concat(System.getProperty("user.home"), dir);
                FileUtils.forceMkdir(new File(workingDir));
            } catch (Exception ioException) {
                log.warn("创建ShellJob工作目录失败", e);
                workingDir = null;
                System.exit(-1);
            }
        }
        workingDir = FilenameUtils.normalize(workingDir);
        DEF_WORKING_DIR = new File(workingDir);
        workingDir = FilenameUtils.normalize(DEF_WORKING_DIR.getAbsolutePath(), true);
        if (!DEF_WORKING_DIR.isDirectory()) {
            log.warn("创建ShellJob默认工作目录失败: {}", workingDir);
            System.exit(-1);
        }
        log.info("ShellJob默认工作目录: {}", workingDir);
    }

    /**
     * 执行控制台命令
     *
     * @param command    需要支持的命令
     * @param scriptFile 需要执行的脚本文件
     * @param outLogFile 标准输出流日志文件
     * @param errLogFile 错误输出流日志文件
     * @param charset    字符集
     * @param timeout    超时时间
     * @param params     命令行参数
     * @return 控制台进程退出值
     */
    public static Integer execShell(List<String> command,
                                    String scriptFile,
                                    String outLogFile,
                                    String errLogFile,
                                    String charset,
                                    Long timeout,
                                    String... params) throws Exception {
        return execShell(null, command, scriptFile, outLogFile, errLogFile, charset, timeout, null, params);
    }

    /**
     * 执行控制台命令
     *
     * @param workingDir 命令执行目录
     * @param command    需要支持的命令
     * @param scriptFile 需要执行的脚本文件
     * @param outLogFile 标准输出流日志文件
     * @param errLogFile 错误输出流日志文件
     * @param charset    字符集
     * @param timeout    超时时间
     * @param callback   日志输出的回调
     * @param params     命令行参数
     * @return 控制台进程退出值
     */
    public static Integer execShell(String workingDir,
                                    List<String> command,
                                    String scriptFile,
                                    String outLogFile,
                                    String errLogFile,
                                    String charset,
                                    Long timeout,
                                    Consumer<String> callback,
                                    String... params) throws Exception {
        Assert.isTrue(new File(scriptFile).setExecutable(true), "文件 " + scriptFile + " 没有可执行权限");
        if (StringUtils.isBlank(charset)) {
            charset = getDefCharset();
        }
        if (timeout == null || timeout <= 0) {
            timeout = DEF_COMMAND_TIMEOUT;
        }
        FileOutputStream out = null;
        FileOutputStream err = null;
        Process proc = null;
        boolean exited = false;
        Thread copyStreamThread = null;
        Thread copyErrStreamThread = null;
        Integer exitValue = null;
        try {
            out = new FileOutputStream(outLogFile, true);
            err = new FileOutputStream(errLogFile, true);
            List<String> commands = new ArrayList<>(2);
            commands.addAll(command);
            commands.add(scriptFile);
            if (params != null) {
                commands.addAll(Arrays.stream(params).toList());
            }
            File dir = DEF_WORKING_DIR;
            if (StringUtils.isNotBlank(workingDir)) {
                dir = new File(workingDir);
            }
            proc = Runtime.getRuntime().exec(StringUtils.join(commands, " "), null, dir);
            copyStreamThread = asyncCopyStream(charset, proc.getInputStream(), out, callback);
            copyStreamThread.start();
            copyErrStreamThread = asyncCopyStream(charset, proc.getErrorStream(), err, callback);
            copyErrStreamThread.start();
            exited = proc.waitFor(timeout, TimeUnit.SECONDS);
        } finally {
            // 关闭流复制线程
            if (copyStreamThread != null) {
                try {
                    copyStreamThread.join(1000);
                } catch (Throwable ignored) {
                }
                copyStreamThread.interrupt();
            }
            if (copyErrStreamThread != null) {
                try {
                    copyErrStreamThread.join(1000);
                } catch (Throwable ignored) {
                }
                copyErrStreamThread.interrupt();
            }
            // 关闭数据流
            try {
                IOUtils.close(out);
            } catch (IOException ignored) {
            }
            try {
                IOUtils.close(err);
            } catch (IOException ignored) {
            }
            if (proc != null && !exited) {
                proc.destroy();
            }
            // 获取进程返回值
            if (proc != null) {
                exitValue = proc.exitValue();
            }
        }
        return exitValue;
    }

    protected static String getDefCharset() {
        String charset;
        if (PlatformOS.isMacOS()) {
            charset = "UTF-8";
        } else if (PlatformOS.isWindows()) {
            charset = "GBK";
        } else {
            charset = "UTF-8";
        }
        if (StringUtils.isBlank(charset)) {
            charset = "UTF-8";
        }
        return charset;
    }

    private static Thread asyncCopyStream(String charset, InputStream in, OutputStream out, Consumer<String> logCallback) {
        Thread thread = new Thread(() -> {
            BufferedReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new BufferedReader(new InputStreamReader(in, charset));
                writer = new BufferedWriter(new OutputStreamWriter(out));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.write("\r");
                    writer.flush();
                    if (logCallback != null) {
                        logCallback.accept(line);
                    }
                    // 响应线程中断
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (logCallback != null) {
                    logCallback.accept(e.getMessage() + "\n" + ExceptionUtils.getStackTraceAsString(e));
                }
            } finally {
                try {
                    IOUtils.close(reader);
                } catch (IOException ignored) {
                }
                try {
                    IOUtils.close(writer);
                } catch (IOException ignored) {
                }
            }
        });
        thread.setName("shell-job-copy-stream");
        thread.setDaemon(true);
        return thread;
    }
}
