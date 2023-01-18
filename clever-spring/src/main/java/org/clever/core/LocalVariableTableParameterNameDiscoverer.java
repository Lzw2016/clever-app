package org.clever.core;

import org.clever.asm.*;
import org.clever.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ParameterNameDiscoverer} 的实现，它使用方法属性中的 LocalVariableTable 信息来发现参数名称。
 * 如果类文件是在没有调试信息的情况下编译的，则返回 null。
 *
 * <p>
 * 使用 ObjectWeb 的 ASM 库来分析类文件。
 * 每个发现者实例以线程安全的方式缓存每个自省类的 ASM 发现信息。
 * 建议尽可能重用 ParameterNameDiscoverer 实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 13:00 <br/>
 */
public class LocalVariableTableParameterNameDiscoverer implements ParameterNameDiscoverer {
    private static final Logger logger = LoggerFactory.getLogger(LocalVariableTableParameterNameDiscoverer.class);
    // marker object for classes that do not have any debug info
    private static final Map<Executable, String[]> NO_DEBUG_INFO_MAP = Collections.emptyMap();
    // 是否使用 parameterNamesCache
    private final boolean useCache;
    // the cache uses a nested index (value is a map) to keep the top level cache relatively small(in size)
    private final Map<Class<?>, Map<Executable, String[]>> parameterNamesCache = new ConcurrentHashMap<>(32);

    public LocalVariableTableParameterNameDiscoverer(boolean useCache) {
        this.useCache = useCache;
    }

    public LocalVariableTableParameterNameDiscoverer() {
        this(true);
    }

    @Override
    public String[] getParameterNames(Method method) {
        Method originalMethod = BridgeMethodResolver.findBridgedMethod(method);
        return doGetParameterNames(originalMethod);
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        return doGetParameterNames(ctor);
    }

    private String[] doGetParameterNames(Executable executable) {
        Class<?> declaringClass = executable.getDeclaringClass();
        Map<Executable, String[]> map;
        if (useCache) {
            map = this.parameterNamesCache.computeIfAbsent(declaringClass, this::inspectClass);
        } else {
            map = inspectClass(declaringClass);
        }
        return (map != NO_DEBUG_INFO_MAP ? map.get(executable) : null);
    }

    /**
     * 检查目标类。
     * <p>将记录异常，并返回一个标记图以指示缺少调试信息。
     */
    private Map<Executable, String[]> inspectClass(Class<?> clazz) {
        InputStream is = clazz.getResourceAsStream(ClassUtils.getClassFileName(clazz));
        if (is == null) {
            // We couldn't load the class file, which is not fatal as it
            // simply means this method of discovering parameter names won't work.
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot find '.class' file for class [" + clazz +
                        "] - unable to determine constructor/method parameter names"
                );
            }
            return NO_DEBUG_INFO_MAP;
        }
        // We cannot use try-with-resources here for the InputStream, since we have
        // custom handling of the close() method in a finally-block.
        try {
            ClassReader classReader = new ClassReader(is);
            Map<Executable, String[]> map = new ConcurrentHashMap<>(32);
            classReader.accept(new ParameterNameDiscoveringVisitor(clazz, map), 0);
            return map;
        } catch (IOException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception thrown while reading '.class' file for class [" + clazz +
                        "] - unable to determine constructor/method parameter names", ex);
            }
        } catch (IllegalArgumentException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("ASM ClassReader failed to parse class file [" + clazz +
                        "], probably due to a new Java class file version that isn't supported yet " +
                        "- unable to determine constructor/method parameter names", ex);
            }
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
        return NO_DEBUG_INFO_MAP;
    }

    /**
     * 检查所有方法和构造函数然后尝试查找给定 {@link Executable} 文件的参数名称的助手类。
     */
    private static class ParameterNameDiscoveringVisitor extends ClassVisitor {
        private static final String STATIC_CLASS_INIT = "<clinit>";

        private final Class<?> clazz;
        private final Map<Executable, String[]> executableMap;

        public ParameterNameDiscoveringVisitor(Class<?> clazz, Map<Executable, String[]> executableMap) {
            super(SpringAsmInfo.ASM_VERSION);
            this.clazz = clazz;
            this.executableMap = executableMap;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // exclude synthetic + bridged && static class initialization
            if (!isSyntheticOrBridged(access) && !STATIC_CLASS_INIT.equals(name)) {
                return new LocalVariableTableVisitor(this.clazz, this.executableMap, name, desc, isStatic(access));
            }
            return null;
        }

        private static boolean isSyntheticOrBridged(int access) {
            return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
        }

        private static boolean isStatic(int access) {
            return ((access & Opcodes.ACC_STATIC) > 0);
        }
    }

    private static class LocalVariableTableVisitor extends MethodVisitor {
        private static final String CONSTRUCTOR = "<init>";

        private final Class<?> clazz;
        private final Map<Executable, String[]> executableMap;
        private final String name;
        private final Type[] args;
        private final String[] parameterNames;
        private final boolean isStatic;
        private boolean hasLvtInfo = false;
        /*
         * The nth entry contains the slot index of the LVT table entry holding the
         * argument name for the nth parameter.
         */
        private final int[] lvtSlotIndex;

        public LocalVariableTableVisitor(Class<?> clazz, Map<Executable, String[]> map, String name, String desc, boolean isStatic) {
            super(SpringAsmInfo.ASM_VERSION);
            this.clazz = clazz;
            this.executableMap = map;
            this.name = name;
            this.args = Type.getArgumentTypes(desc);
            this.parameterNames = new String[this.args.length];
            this.isStatic = isStatic;
            this.lvtSlotIndex = computeLvtSlotIndices(isStatic, this.args);
        }

        @Override
        public void visitLocalVariable(String name, String description, String signature, Label start, Label end, int index) {
            this.hasLvtInfo = true;
            for (int i = 0; i < this.lvtSlotIndex.length; i++) {
                if (this.lvtSlotIndex[i] == index) {
                    this.parameterNames[i] = name;
                }
            }
        }

        @Override
        public void visitEnd() {
            if (this.hasLvtInfo || (this.isStatic && this.parameterNames.length == 0)) {
                // visitLocalVariable will never be called for static no args methods
                // which doesn't use any local variables.
                // This means that hasLvtInfo could be false for that kind of methods
                // even if the class has local variable info.
                this.executableMap.put(resolveExecutable(), this.parameterNames);
            }
        }

        private Executable resolveExecutable() {
            ClassLoader loader = this.clazz.getClassLoader();
            Class<?>[] argTypes = new Class<?>[this.args.length];
            for (int i = 0; i < this.args.length; i++) {
                argTypes[i] = ClassUtils.resolveClassName(this.args[i].getClassName(), loader);
            }
            try {
                if (CONSTRUCTOR.equals(this.name)) {
                    return this.clazz.getDeclaredConstructor(argTypes);
                }
                return this.clazz.getDeclaredMethod(this.name, argTypes);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Method [" + this.name +
                        "] was discovered in the .class file but cannot be resolved in the class object", ex
                );
            }
        }

        private static int[] computeLvtSlotIndices(boolean isStatic, Type[] paramTypes) {
            int[] lvtIndex = new int[paramTypes.length];
            int nextIndex = (isStatic ? 0 : 1);
            for (int i = 0; i < paramTypes.length; i++) {
                lvtIndex[i] = nextIndex;
                if (isWideType(paramTypes[i])) {
                    nextIndex += 2;
                } else {
                    nextIndex++;
                }
            }
            return lvtIndex;
        }

        private static boolean isWideType(Type aType) {
            // float is not a wide type
            return (aType == Type.LONG_TYPE || aType == Type.DOUBLE_TYPE);
        }
    }
}
