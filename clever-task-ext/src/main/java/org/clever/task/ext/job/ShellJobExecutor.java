package org.clever.task.ext.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.DateUtils;
import org.clever.core.PlatformOS;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.job.JobExecutor;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.model.entity.TaskShellJob;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:56 <br/>
 */
@Slf4j
public class ShellJobExecutor implements JobExecutor {
    private static final int COMMAND_TIMEOUT = 60 * 10;
    private static final File WORKING_DIR;

    static {
        final String dir = "shell_job";
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
        WORKING_DIR = new File(workingDir);
        workingDir = FilenameUtils.normalize(WORKING_DIR.getAbsolutePath(), true);
        if (!WORKING_DIR.isDirectory()) {
            log.warn("创建ShellJob工作目录失败: {}", workingDir);
            System.exit(-1);
        }
        log.info("ShellJob工作目录: {}", workingDir);
    }

    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_4);
    }

    @Override
    public void exec(Date dbNow, TaskJob job, TaskScheduler scheduler, TaskStore taskStore) throws Exception {
        final TaskShellJob shellJob = taskStore.beginReadOnlyTX(status -> taskStore.getShellJob(scheduler.getNamespace(), job.getId()));
        if (shellJob == null) {
            throw new JobExecutorException(String.format("ShellJob数据不存在，JobId=%s", job.getId()));
        }
        final String dir = FilenameUtils.concat(WORKING_DIR.getAbsolutePath(), String.format("%s_%s", job.getId(), job.getName()));
        final String timeLine = String.format(
                "\n### %s ###---------------------------------------------------------------------------------------------------------------------------\n",
                DateUtils.formatToString(dbNow)
        );
        final List<String> command = EnumConstant.SHELL_TYPE_COMMAND_MAPPING.getOrDefault(shellJob.getShellType(), Arrays.asList("sh", "-c"));
        final String scriptFile = FilenameUtils.concat(
                dir,
                String.format(
                        "%s_%s%s",
                        job.getName(),
                        job.getId(),
                        EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.getOrDefault(shellJob.getShellType(), ".txt")
                )
        );
        final String outLogFile = FilenameUtils.concat(dir, "out.log");
        final String errLogFile = FilenameUtils.concat(dir, "err.log");
        // 生成脚本文件
        FileUtils.forceMkdir(new File(dir));
        FileUtils.writeStringToFile(new File(scriptFile), shellJob.getContent(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outLogFile), timeLine, StandardCharsets.UTF_8, true);
        FileUtils.writeStringToFile(new File(errLogFile), timeLine, StandardCharsets.UTF_8, true);
        String charset = shellJob.getShellCharset();
        if (StringUtils.isBlank(charset)) {
            charset = getCharset();
        }
        Integer timeout = shellJob.getShellTimeout();
        if (timeout == null || timeout <= 0) {
            timeout = COMMAND_TIMEOUT;
        }
        Integer exitValue = execShell(command, scriptFile, outLogFile, errLogFile, charset, timeout);
        log.info("ShellJob执行完成，进程退出状态码:{}， shellType={} | jobId={}", exitValue, shellJob.getShellType(), job.getId());
    }

    protected Integer execShell(List<String> command, String scriptFile, String outLogFile, String errLogFile, String charset, long timeout, String... params) throws Exception {
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
                commands.addAll(Arrays.stream(params).collect(Collectors.toList()));
            }
            proc = Runtime.getRuntime().exec(StringUtils.join(commands, " "), null, WORKING_DIR);
            copyStreamThread = asyncCopyStream(charset, proc.getInputStream(), out);
            copyStreamThread.start();
            copyErrStreamThread = asyncCopyStream(charset, proc.getErrorStream(), err);
            copyErrStreamThread.start();
            exited = proc.waitFor(timeout, TimeUnit.SECONDS);
        } finally {
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
            if (copyStreamThread != null) {
                copyStreamThread.interrupt();
            }
            if (copyErrStreamThread != null) {
                copyErrStreamThread.interrupt();
            }
            if (proc != null) {
                exitValue = proc.exitValue();
            }
        }
        return exitValue;
    }

    protected String getCharset() {
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

    private Thread asyncCopyStream(String charset, InputStream in, OutputStream out) {
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
                    // 响应线程中断
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
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
