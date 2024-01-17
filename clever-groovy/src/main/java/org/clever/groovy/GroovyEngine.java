package org.clever.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.groovy.utils.FilePathUtils;
import org.clever.util.Assert;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/17 12:12 <br/>
 */
@Slf4j
public class GroovyEngine {
    private final GroovyScriptEngine engine;

    @SneakyThrows
    public GroovyEngine(String[] urls) {
        Assert.notNull(urls, "参数urls不能为null");
        Assert.notEmpty(urls, "参数urls不能为空");
        engine = new GroovyScriptEngine(urls);
    }

    public GroovyEngine(String url) {
        this(new String[]{url});
    }

    /**
     * 动态加载Class
     *
     * @param classFullName class全路径(包点类名称)
     */
    @SneakyThrows
    public Class<?> loadClass(String classFullName) {
        Assert.hasText(classFullName, "参数classFullName不能为空");
        Class<?> clazz;
        String classPath = FilePathUtils.getClassPath(classFullName);
        clazz = engine.loadScriptByName(classPath);
        // GroovyClassLoader classLoader = new GroovyClassLoader();
        // classLoader.parseClass("SourceCode");
        return clazz;
    }

    public List<Method> getMethods(String classFullName, String methodName) {
        Assert.hasText(classFullName, "参数classFullName不能为空");
        Assert.hasText(methodName, "参数method不能为空");
        Class<?> clazz = loadClass(classFullName);
        Method[] methods = clazz.getDeclaredMethods();
        return Arrays.stream(methods).filter(m -> Objects.equals(methodName, m.getName())).collect(Collectors.toList());
    }

    public Method getMethod(String classFullName, String methodName) {
        Method method = null;
        List<Method> methods = getMethods(classFullName, methodName);
        if (!methods.isEmpty()) {
            method = methods.get(0);
        }
        if (methods.size() > 1) {
            log.error("class={} 包含{}个 method={}", classFullName, methods.size(), methodName);
        }
        return method;
    }

    @SneakyThrows
    public Object invokeMethod(String classFullName, String methodName, Object... args) {
        Method method = getMethod(classFullName, methodName);
        if (method == null) {
            throw new IllegalArgumentException(String.format("class=%s中不存在static method=%s", classFullName, methodName));
        }
        ReflectionsUtils.makeAccessible(method);
        return method.invoke(null, args);
    }

    public Method getStaticMethod(String classFullName, String methodName) {
        List<Method> methods = getMethods(classFullName, methodName).stream()
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .collect(Collectors.toList());
        Method method = null;
        if (!methods.isEmpty()) {
            method = methods.get(0);
        }
        if (methods.size() > 1) {
            log.warn("class={} 包含{}个 method={}", classFullName, methods.size(), methodName);
        }
        return method;
    }

    @SneakyThrows
    public Object invokeStaticMethod(String classFullName, String methodName, Object... args) {
        Method method = getStaticMethod(classFullName, methodName);
        if (method == null) {
            throw new IllegalArgumentException(String.format("class=%s中不存在static method=%s", classFullName, methodName));
        }
        ReflectionsUtils.makeAccessible(method);
        return method.invoke(null, args);
    }

    public Class<?> parseClass(String classCode, String fileName) {
        return engine.getGroovyClassLoader().parseClass(classCode, fileName);
    }

    public long groovyLastModifiedMax() {
        long max = 0;
        URL[] roots = ReflectionsUtils.getFieldValue(engine, "roots");
        if (roots != null) {
            max = Arrays.stream(roots).map(url -> {
                    File file = null;
                    try {
                        file = new File(url.getFile());
                        if (!file.exists() || !file.isDirectory()) {
                            file = null;
                        }
                    } catch (Exception ignored) {
                    }
                    return file;
                }).filter(Objects::nonNull)
                .map(file -> FileUtils.listFiles(file, new String[]{"groovy"}, true).stream()
                    .map(File::lastModified)
                    .max(Long::compare)
                    .orElse(0L)
                ).max(Long::compare).orElse(0L);
        }
        return max;
    }

    public GroovyClassLoader getGroovyClassLoader() {
        return engine.getGroovyClassLoader();
    }
}
