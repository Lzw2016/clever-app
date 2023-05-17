package org.clever.task.ext.job;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/17 14:32 <br/>
 */
@Slf4j
public class ShellJobExecutorTest {
    @Test
    public void t01() throws Exception {
        // TODO 待多平台测试
        String scriptFile = "./shell_job/t01.bat";
        scriptFile = new File(scriptFile).getAbsolutePath();
        String outLogFile = "./shell_job/out.txt";
        String errLogFile = "./shell_job/err.txt";
        ShellJobExecutor executor = new ShellJobExecutor();
        Integer exitValue = executor.execShell(Arrays.asList("cmd", "/q", "/c"), scriptFile, outLogFile, errLogFile, "GBK", 10);
        log.info("exitValue-> {}", exitValue);
    }
}
