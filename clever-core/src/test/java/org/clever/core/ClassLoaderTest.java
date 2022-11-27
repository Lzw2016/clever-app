package org.clever.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import sun.misc.Launcher;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/18 10:59 <br/>
 */
@Slf4j
public class ClassLoaderTest {
    @Test
    public void t01() throws Exception {
        Launcher launcherA = new Launcher();
        log.info("--> {}", launcherA.getClassLoader());
        Launcher launcherB = new Launcher();
        log.info("--> {}", launcherB.getClassLoader());

        Class<?> classA = launcherA.getClassLoader().loadClass("org.clever.core.ClassLoaderTestA");
        // 修改 ClassLoaderTestA 代码
        Thread.yield();
        Class<?> classB = launcherB.getClassLoader().loadClass("org.clever.core.ClassLoaderTestA");
        log.info("# classA -------------------------------------");
        classA.getMethod("t01").invoke(null);
        log.info("# classB -------------------------------------");
        classB.getMethod("t01").invoke(null);
        log.info("");
        log.info("");
        log.info("# classA -------------------------------------");
        classA.getMethod("t02").invoke(classA.newInstance());
        log.info("# classB -------------------------------------");
        classB.getMethod("t02").invoke(classB.newInstance());
    }

    @Test
    public void t02() throws Exception {
        Launcher launcher = new Launcher();
        log.info("--> {}", launcher.getClassLoader());
        File root = new File("out/production/classes");
        Collection<File> files = FileUtils.listFiles(root, new String[]{"class"}, true);
        Set<String> classNames = new HashSet<>();
        for (File file : files) {
            String filePath = FilenameUtils.getFullPath(file.getAbsolutePath());
            String rootPath = root.getAbsolutePath();
            String className = filePath.substring(rootPath.length() + 1) + FilenameUtils.getBaseName(file.getAbsolutePath());
            className = className.replace('\\', '.');
            if (className.contains("$") | className.contains("[")) {
                continue;
            }
            classNames.add(className);
            log.info("className --> {}", className);
        }
        log.info("size --> {}", classNames.size());
        log.info("");
        log.info("");
        final int count = 5;
        for (int i = 0; i < count; i++) {
            long startTime = System.currentTimeMillis();
            for (String className : classNames) {
                launcher.getClassLoader().loadClass(className);
            }
            long endTime = System.currentTimeMillis();
            log.info("#{} | size:{} | 耗时：{}", (i + 1), classNames.size(), (endTime - startTime));
        }
    }

//    @Test
//    public void t03() {
//        File root = new File("../bundle/src");
//        Collection<File> files = FileUtils.listFiles(root, new String[]{"groovy"}, true);
//        Set<String> groovyNames = new HashSet<>();
//        for (File file : files) {
//            String groovyName = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
//            groovyName = groovyName.replace('\\', '.');
//            groovyNames.add(groovyName);
//            log.info("groovyName --> {}", groovyName);
//        }
//        log.info("size --> {}", groovyNames.size());
//        log.info("");
//        log.info("");
//        final HotReloadEngine engine = new HotReloadEngine(new String[]{
//                root.getAbsolutePath(),
//                new File("../../wms-core/src/main/groovy").getAbsolutePath(),
//                new File("../../framework/studio/src/main/groovy").getAbsolutePath(),
//        });
//        long startTime = System.currentTimeMillis();
//        for (String className : groovyNames) {
//            engine.loadClass(className);
//        }
//        long endTime = System.currentTimeMillis();
//        log.info("size:{} | 耗时：{}", groovyNames.size(), (endTime - startTime));
//    }

    @Test
    public void t04() {
        try {
            Class.forName("A.B");
        } catch (ClassNotFoundException e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Test
    public void t05() throws ClassNotFoundException {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/production/classes"
        );
        File root = new File("out/production/classes");
        Collection<File> files = FileUtils.listFiles(root, new String[]{"class"}, true);
        Set<String> classNames = new HashSet<>();
        for (File file : files) {
            String filePath = FilenameUtils.getFullPath(file.getAbsolutePath());
            String rootPath = root.getAbsolutePath();
            String className = filePath.substring(rootPath.length() + 1) + FilenameUtils.getBaseName(file.getAbsolutePath());
            className = className.replace('\\', '.');
            if (className.contains("$") | className.contains("[")) {
                continue;
            }
            classNames.add(className);
            // log.info("className --> {}", className);
        }
        log.info("size --> {}", classNames.size());
        log.info("");
        log.info("");
        final int count = 9;
        for (int i = 0; i < count; i++) {
            long startTime = System.currentTimeMillis();
            for (String className : classNames) {
                classLoader.loadClass(className);
            }
            long endTime = System.currentTimeMillis();
            log.info("unloadAllClass --> {}", classLoader.unloadAllClass());
            log.info("#{} | size:{} | 耗时：{}", (i + 1), classNames.size(), (endTime - startTime));
            System.gc();
            System.runFinalization();
        }
    }

    @Test
    public void t06() throws Exception {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/test/classes"
        );
        Class<?> classA = classLoader.loadClass("org.clever.core.ClassLoaderTestA");
        // 修改 ClassLoaderTestA 代码
        Thread.yield();
        classLoader.unloadClass("org.clever.core.ClassLoaderTestA");
        Class<?> classB = classLoader.loadClass("org.clever.core.ClassLoaderTestA");
        log.info("# classA -------------------------------------");
        classA.getMethod("t01").invoke(null);
        log.info("# classB -------------------------------------");
        classB.getMethod("t01").invoke(null);
    }

    @Test
    public void t07() throws Exception {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/test/classes"
        );
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> classA = Class.forName("org.clever.core.ClassLoaderTestA");
        // 修改 ClassLoaderTestA 代码
        Thread.yield();
        classLoader.unloadClass("org.clever.core.ClassLoaderTestA");
        Class<?> classB = Class.forName("org.clever.core.ClassLoaderTestA");
        log.info("# classA -------------------------------------");
        classA.getMethod("t01").invoke(null);
        log.info("# classB -------------------------------------");
        classB.getMethod("t01").invoke(null);
    }
}
