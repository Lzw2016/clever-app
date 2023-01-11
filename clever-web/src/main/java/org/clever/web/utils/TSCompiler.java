//package org.clever.web.utils;
//
//import lombok.Getter;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//
//public class TSCompiler {
//    private static final Logger log = LoggerFactory.getLogger("TSCompiler");
//
//    @Getter
//    private final String folder;
//    private Process proc;
//    private BufferedReader in;
//    private PrintWriter out;
//
//    public TSCompiler(String folder) {
//        this.folder = folder;
//    }
//
//    public void start() {
//        new Thread(() -> {
//            File workingDir = new File(System.getProperty("user.dir"));
//            try {
//                // 启动进程
//                String os = System.getProperty("os.name");
//                if (os.toLowerCase().contains("window")) {
//                    proc = Runtime.getRuntime().exec("cmd /q /k cd /d " + folder, null, workingDir);
//                } else {
//                    proc = Runtime.getRuntime().exec("/bin/bash", null, workingDir);
//                }
//                Runtime.getRuntime().addShutdownHook(new Thread(() -> proc.destroy()));
//                // 启动 tsc -w
//                in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())), true);
//                out.println("cd " + folder);
//                out.println("node node_modules/typescript/bin/tsc -w");
//                String line;
//                while ((line = StringUtils.trim(in.readLine())) != null) {
//                    if (StringUtils.isBlank(line)) {
//                        continue;
//                    }
//                    log.debug("[TSC] \033[35m{}\033[0m", line);
//                }
//                proc.waitFor();
//                in.close();
//                out.close();
//                proc.exitValue();
//                proc.destroy();
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//            }
//        }).start();
//    }
//}
