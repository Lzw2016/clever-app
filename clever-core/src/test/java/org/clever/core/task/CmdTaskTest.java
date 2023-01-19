package org.clever.core.task;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 15:44 <br/>
 */
@Slf4j
public class CmdTaskTest {
    @Test
    public void t01() {
        StartupTaskConfig.CmdConfig cmd = new StartupTaskConfig.CmdConfig();
        // cmd.setCmd("gradlew.bat clever-boot:classes --watch-fs --build-cache --configuration-cache --configuration-cache-problems=warn --daemon");
        // cmd.setCmd("gradlew.bat clever-boot:classes --continuous");
        cmd.setCmd("dir");
        CmdTask cmdTask = new CmdTask(
                "测试",
                false,
                null,
                new File("D:\\SourceCode\\clever\\clever-app"),
                cmd
        );
        cmdTask.start();
    }

    @SneakyThrows
    @Test
    public void t02() {
        File workDir = new File("D:\\SourceCode\\clever\\clever-app");
        Process proc = Runtime.getRuntime().exec(new String[]{
                        "cmd",
                        "/q",
                        "/c",
                        // "gradlew.bat clever-boot:classes --watch-fs --build-cache --configuration-cache --configuration-cache-problems=warn --daemon",
                        "gradlew.bat clever-boot:classes --continuous --watch-fs --build-cache --configuration-cache --configuration-cache-problems=warn --daemon",
                        // "dir",
                },
                null,
                workDir
        );
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "GBK"));
        BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream(), "GBK"));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())), true);
        new Thread(() -> {
            try {
                String inLine, errLine;
                while ((inLine = StringUtils.trim(in.readLine())) != null | (errLine = StringUtils.trim(err.readLine())) != null) {
                    if (StringUtils.isNotBlank(inLine)) {
                        log.debug(inLine);
                    }
                    if (StringUtils.isNotBlank(errLine)) {
                        log.error(errLine);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        proc.waitFor();
        log.info("###-> {}", proc.exitValue());
    }
}
