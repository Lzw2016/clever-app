package org.clever.app;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.HotReloadClassLoader;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/23 13:12 <br/>
 */
@Slf4j
public class KotlinTest {
    @Test
    public void t01() throws Exception {
        Class<?> clazz = Class.forName("org.clever.app.TestKt");
        clazz.getMethod("t01").invoke(null);
    }

    @Test
    public void t02() throws Exception {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/production/classes"
        );
        Class<?> clazz = classLoader.loadClass("org.clever.app.TestKt");
        clazz.getMethod("t01").invoke(null);
        // 修改 ClassLoaderTestA 代码
        Thread.yield();
        classLoader.unloadClass("org.clever.app.TestKt");
        clazz = classLoader.loadClass("org.clever.app.TestKt");
        clazz.getMethod("t01").invoke(null);
    }

    @Test
    public void t03() throws Exception {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/production/classes"
        );
        Class<?> clazz = classLoader.loadClass("org.clever.app.TestKt");
        KClass<?> kClass = JvmClassMappingKt.getKotlinClass(clazz);
        log.info("--> {}", kClass);
    }

    @Test
    public void t04() throws Exception {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/production/classes"
        );
        Class<?> clazz = classLoader.loadClass("org.clever.app.Test2");
        Object companion = clazz.getField("Companion").get(null);
        companion.getClass().getMethod("t01").invoke(companion);
    }

    @Test
    public void t05() throws Exception {
        HotReloadClassLoader classLoader = new HotReloadClassLoader(
                Thread.currentThread().getContextClassLoader(),
                "out/production/classes"
        );
        Class<?> clazz = classLoader.loadClass("org.clever.app.Test3");
        Object instance = clazz.getField("INSTANCE").get(null);
        instance.getClass().getMethod("t01").invoke(instance);
    }
}
