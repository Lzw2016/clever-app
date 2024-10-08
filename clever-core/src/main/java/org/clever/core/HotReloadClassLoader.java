package org.clever.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/18 14:12 <br/>
 */
@Slf4j
public class HotReloadClassLoader extends ClassLoader {
    /**
     * 内部ClassLoader的引用队列，用于观察内部的ClassLoader对象是否被垃圾回收
     */
    private static final ReferenceQueue<ClassLoader> REFERENCE_QUEUE = new ReferenceQueue<>();
    private static final ConcurrentMap<Reference<ClassLoader>, Long> PHANTOM_REFERENCE_MAP = new ConcurrentHashMap<>(8);
    /**
     * 当前进程中存在的 InnerClassLoader 数量计数器
     */
    private static final AtomicInteger INNER_CLASS_LOADER_COUNT = new AtomicInteger(0);
    /**
     * 当前进程创建 InnerClassLoader 的总数
     */
    private static final AtomicLong SERIAL_NUMBER = new AtomicLong(0);

    static {
        Thread thread = new Thread(() -> {
            while (true) {
                Reference<?> phantomReference = REFERENCE_QUEUE.poll();
                if (phantomReference != null) {
                    Long serialNumber = PHANTOM_REFERENCE_MAP.remove(phantomReference);
                    log.debug("[#{}]InnerClassLoader被GC回收，当前size={}", serialNumber, INNER_CLASS_LOADER_COUNT.decrementAndGet());
                }
                try {
                    // noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }, "WATCH-InnerClassLoader-GC");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 已加载的class文件缓存
     */
    private final ConcurrentMap<String, byte[]> classCache = new ConcurrentHashMap<>(64);
    /**
     * 加载class文件的路径，以"/"结尾
     */
    private final String[] loadPath;
    /**
     * 不支持热加载的class前缀
     */
    private final String[] excludeClassPrefixes;
    /**
     * 内部委托的 ClassLoader
     */
    private volatile ClassLoader innerClassLoader;

    /**
     * @param parent               父ClassLoader
     * @param excludeClassPrefixes 不支持热加载的class前缀
     * @param loadPath             加载class文件的路径
     */
    public HotReloadClassLoader(ClassLoader parent, String[] excludeClassPrefixes, String... loadPath) {
        super(parent);
        Assert.notNull(excludeClassPrefixes, "excludeClassPrefixes 不能为null");
        Assert.notEmpty(loadPath, "loadPath 不能为空");
        this.excludeClassPrefixes = excludeClassPrefixes;
        this.loadPath = cleanPath(loadPath);
        this.innerClassLoader = new InnerClassLoader(this.getParent());
    }

    /**
     * @param parent   父ClassLoader
     * @param loadPath 加载class文件的路径
     */
    public HotReloadClassLoader(ClassLoader parent, String... loadPath) {
        this(parent, new String[0], loadPath);
    }

    /**
     * 卸载 class
     *
     * @param names class全路径名
     * @return 卸载的Class数量
     */
    public int unloadClass(Collection<String> names) {
        if (names == null) {
            return 0;
        }
        int count = 0;
        for (String name : names) {
            if (classCache.remove(name) != null) {
                count++;
            }
        }
        if (count > 0) {
            innerClassLoader = new InnerClassLoader(this.getParent());
        }
        return count;
    }

    /**
     * 卸载 class
     *
     * @param names class全路径名
     * @return 卸载的Class数量
     */
    public int unloadClass(String... names) {
        if (names == null || names.length == 0) {
            return 0;
        }
        return unloadClass(Arrays.asList(names));
    }

    /**
     * 卸载所有的 class
     */
    public int unloadAllClass() {
        int count = classCache.size();
        classCache.clear();
        if (count > 0) {
            innerClassLoader = new InnerClassLoader(this.getParent());
        }
        return count;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return innerClassLoader.loadClass(name);
    }

    private static String[] cleanPath(String[] paths) {
        List<String> result = new ArrayList<>(paths.length);
        for (String path : paths) {
            path = StringUtils.trimToEmpty(path);
            path = path.replace('\\', '/');
            if (!path.replace('.', ' ').replace('/', ' ').trim().isEmpty() && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            path = path + "/";
            result.add(path);
        }
        return result.toArray(new String[0]);
    }

    /**
     * 是否需要热加载class
     *
     * @param name class名
     */
    private boolean basePackageFilter(String name) {
        if (excludeClassPrefixes == null) {
            return true;
        }
        for (String prefix : excludeClassPrefixes) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 内部真实的 ClassLoader
     */
    private class InnerClassLoader extends ClassLoader {
        private final Method findLoadedClassMethod;

        public InnerClassLoader(ClassLoader parent) {
            super(parent);
            Method method = null;
            if (parent != null) {
                try {
                    method = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                    method.setAccessible(true);
                } catch (Exception ignored) {
                }
            }
            findLoadedClassMethod = method;
            // 创建序列号
            final Long serialNumber = SERIAL_NUMBER.incrementAndGet();
            PHANTOM_REFERENCE_MAP.put(new PhantomReference<>(this, REFERENCE_QUEUE), serialNumber);
            // InnerClassLoader对象计数
            final Integer count = INNER_CLASS_LOADER_COUNT.incrementAndGet();
            log.debug("[#{}]InnerClassLoader被创建 size={},", serialNumber, count);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            ClassLoader parentClassLoader = getParent();
            synchronized (getClassLoadingLock(name)) {
                Class<?> clazz = findLoadedClass(name);
                if (clazz != null) {
                    return clazz;
                }
                if (parentClassLoader != null && findLoadedClassMethod != null) {
                    try {
                        // 调用 getParent().findLoadedClass(name)
                        clazz = (Class<?>) findLoadedClassMethod.invoke(parentClassLoader, name);
                    } catch (Exception ignored) {
                    }
                    if (clazz != null) {
                        return clazz;
                    }
                }
                ClassNotFoundException exception = null;
                if (basePackageFilter(name)) {
                    try {
                        clazz = findClass(name);
                    } catch (ClassNotFoundException e) {
                        exception = e;
                        if (parentClassLoader != null) {
                            clazz = parentClassLoader.loadClass(name);
                        }
                    }
                } else {
                    if (parentClassLoader != null) {
                        clazz = parentClassLoader.loadClass(name);
                    }
                }
                if (clazz == null) {
                    if (exception == null) {
                        exception = new ClassNotFoundException(String.format("找不到class: %s", name));
                    }
                    throw exception;
                }
                return clazz;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // log.debug("findClass -> {}", name);
            byte[] bytes = classCache.get(name);
            if (bytes == null) {
                File clazzFile = null;
                List<String> clazzPath = new ArrayList<>(loadPath.length);
                for (String path : loadPath) {
                    try {
                        path = path + name.replace('.', '/') + ".class";
                        clazzPath.add(path);
                        File file = ResourceUtils.getFile(path);
                        if (file.exists()) {
                            clazzFile = file;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (clazzFile == null) {
                    throw new ClassNotFoundException(String.format("%s 不存在,paths=[%s]", name, StringUtils.join(clazzPath.toArray(), ", ")));
                }
                try {
                    // log.debug("findClass: {}", clazzFile.getAbsolutePath());
                    bytes = FileUtils.readFileToByteArray(clazzFile);
                    classCache.put(name, bytes);
                } catch (Exception e) {
                    throw new ClassNotFoundException(String.format("读取文件失败，path=%s", clazzFile.getAbsolutePath()), e);
                }
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
