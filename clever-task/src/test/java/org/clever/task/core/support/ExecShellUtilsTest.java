package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/04 13:57 <br/>
 */
@Slf4j
public class ExecShellUtilsTest {
    @Test
    public void t01() throws Exception {
        // TODO 待多平台测试
        String scriptFile = "./shell_job_log/t01.bat";
        scriptFile = new File(scriptFile).getAbsolutePath();
        String outLogFile = "./shell_job_log/out.txt";
        String errLogFile = "./shell_job_log/err.txt";
        Integer exitValue = ExecShellUtils.execShell(
            Arrays.asList("cmd", "/q", "/c"),
            scriptFile,
            outLogFile,
            errLogFile,
            "GBK",
            10L
        );
        log.info("exitValue-> {}", exitValue);
    }
}
