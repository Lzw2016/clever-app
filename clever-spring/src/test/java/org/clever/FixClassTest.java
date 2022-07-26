package org.clever;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 12:00 <br/>
 */
@Slf4j
public class FixClassTest {
    @Test
    public void t01() {
        File root = new File("src/main/java/org/clever");
        Collection<File> files = FileUtils.listFiles(root, new String[]{"java"}, true);
        Set<String> ignoreClassNames = new HashSet<>(Arrays.asList(
                ".core.io.VfsUtils",
                ".core.io.support.VfsPatternUtils",
                ".core.Initializer",
                ".core.InitializerException",
                ".beans.factory.config.BeanHolder",
                ".beans.factory.NoSuchBeanException",
                ".beans.factory.NoUniqueBeanException",
                ".beans.factory.support.BeanOverrideException",
                ".boot.context.config.ConfigDataBootstrap",
                ".boot.context.logging.LoggingBootstrap",
                ".context.AppContext",
                "",
                ""
        ));
        List<String> classNames = new ArrayList<>(files.size());
        for (File file : files) {
            // log.info("--> {}", file.getAbsolutePath());
            String name = file.getAbsolutePath()
                    .substring(root.getAbsolutePath().length())
                    .replace('\\', '.')
                    .replace(".java", "");
            if (!ignoreClassNames.contains(name)) {
                classNames.add("org.springframework" + name);
            }
        }
        List<String> errorClassNames = new ArrayList<>();
        for (String className : classNames) {
            // log.info("--> {}", className);
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                errorClassNames.add(className);
            } catch (Throwable e) {
                log.info("{} 加载失败: {}", className, e.getMessage());
            }
        }
        log.info("-- 错误 ---------------------------------------------------");
        for (String errorClassName : errorClassNames) {
            log.info("--> {}", errorClassName);
        }
        log.info("-- 完成 ---------------------------------------------------");
    }
}
