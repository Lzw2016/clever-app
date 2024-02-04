package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.clever.core.Conv;
import org.clever.core.DateUtils;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.model.entity.TaskShellJob;
import org.clever.task.core.support.ExecShellUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:56 <br/>
 */
@Slf4j
public class ShellJobExecutor implements JobExecutor {

    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_4);
    }

    @Override
    public void exec(final JobContext context) throws Exception {
        final TaskJob job = context.getJob();
        final TaskStore taskStore = context.getTaskStore();
        final TaskScheduler scheduler = context.getScheduler();
        final TaskShellJob shellJob = taskStore.beginReadOnlyTX(status -> taskStore.getShellJob(scheduler.getNamespace(), job.getId()));
        if (shellJob == null) {
            throw new JobExecutorException(String.format("ShellJob数据不存在，JobId=%s", job.getId()));
        }
        context.setInnerData(JobContext.INNER_SHELL_JOB_KEY, shellJob);
        final String shellJobWorkingDir = context.getTaskContext().getShellJobWorkingDir();
        final String dir = FilenameUtils.concat(shellJobWorkingDir, String.format("%s_%s", job.getId(), job.getName()));
        final String timeLine = String.format(
            "\n### %s ###---------------------------------------------------------------------------------------------------------------------------\n",
            DateUtils.formatToString(context.getDbNow())
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
        Integer exitValue = ExecShellUtils.execShell(
            shellJobWorkingDir,
            command,
            scriptFile,
            outLogFile,
            errLogFile,
            shellJob.getShellCharset(),
            Conv.asLong(shellJob.getShellTimeout()),
            context::debug
        );
        log.info("ShellJob执行完成，进程退出状态码:{}， shellType={} | jobId={}", exitValue, shellJob.getShellType(), job.getId());
    }
}
