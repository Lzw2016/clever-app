package org.clever.util;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 10:16 <br/>
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ClassUtils {
    /**
     * 数组类型名称后缀: {@code "[]"}.
     */
    public static final String ARRAY_SUFFIX = "[]";
    /**
     * 数组类型名称前缀: {@code "["}.
     */
    private static final String INTERNAL_ARRAY_PREFIX = "[";
    /**
     * 非基本类型的数组类型名称前缀: {@code "[L"}.
     */
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";
    /**
     * 一个可重用的空类数组常量
     */
    private static final Class<?>[] EMPTY_CLASS_ARRAY = {};
    /**
     * package分隔符字符: {@code '.'}.
     */
    private static final char PACKAGE_SEPARATOR = '.';
    /**
     * 路径分隔符字符: {@code '/'}.
     */
    private static final char PATH_SEPARATOR = '/';
    /**
     * 内部类的分隔符字符: {@code '$'}.
     */
    private static final char NESTED_CLASS_SEPARATOR = '$';
    /**
     * CGLIB的类型分隔符: {@code "$$"}.
     */
    public static final String CGLIB_CLASS_SEPARATOR = "$$";
    /**
     * “.class”文件后缀
     */
    public static final String CLASS_FILE_SUFFIX = ".class";
    /**
     * 基本数据类型与其对应的包装类型的映射关系<br/>
     * <pre>{@code Map<包装类型, 基本类型>}</pre>
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);
    /**
     * 基本数据类型与其对应的包装类型的映射关系<br/>
     * <pre>{@code Map<基本类型, 包装类型>}</pre>
     */
    private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);
    /**
     * 基本数据类型名称与其Class的映射关系<br/>
     * <pre>{@code Map<基本类型名称, 基本类型的Class>}</pre>
     */
    private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);
    /**
     * Java常用类型名与其Class的映射关系<br/>
     * <pre>{@code Map<常用类型名称, 基本类型的Class>}</pre>
     */
    private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);
    /**
     * 常见的Java语言接口
     */
    private static final Set<Class<?>> javaLanguageInterfaces;

    static {
        // 初始化基本数据类型与其对应的包装类型的映射关系
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);
        primitiveWrapperTypeMap.put(Void.class, void.class);
        // Map entry iteration is less expensive to initialize than forEach with lambdas
        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
            primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
            registerCommonClasses(entry.getKey());
        }
        // 初始化基本数据类型名称与其Class的映射关系
        Set<Class<?>> primitiveTypes = new HashSet<>(32);
        primitiveTypes.addAll(primitiveWrapperTypeMap.values());
        Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class, double[].class, float[].class, int[].class, long[].class, short[].class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }
        // 初始化Java常用类型名与其Class的映射关系
        registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class, Float[].class, Integer[].class, Long[].class, Short[].class);
        registerCommonClasses(Number.class, Number[].class, String.class, String[].class, Class.class, Class[].class, Object.class, Object[].class);
        registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class, Error.class, StackTraceElement.class, StackTraceElement[].class);
        registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class, Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);
        Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class, Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
        registerCommonClasses(javaLanguageInterfaceArray);
        javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
    }

    /**
     * 缓存Java常用类型
     */
    private static void registerCommonClasses(Class<?>... commonClasses) {
        for (Class<?> clazz : commonClasses) {
            commonClassCache.put(clazz.getName(), clazz);
        }
    }

    /**
     * 检查crhsType是不是lhsType的子类或者子接口(支持java基本类型的判断)
     */
    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        Assert.notNull(lhsType, "Left-hand side type must not be null");
        Assert.notNull(rhsType, "Right-hand side type must not be null");
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }
        if (lhsType.isPrimitive()) {
            Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
            return (lhsType == resolvedPrimitive);
        } else {
            Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
            return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
        }
    }

    /**
     * 判断clazz是否是内部类
     */
    public static boolean isInnerClass(Class<?> clazz) {
        return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
    }

    /**
     * 代替{@code Class.forName()}，如：
     * <pre>{@code
     * Class.forName("int")
     * Class.forName("String[]")
     * }</pre>
     * 支持解析Java源代码样式的嵌套类名，如：
     * <pre>{@code
     * Class.forName("java.lang.Thread.State")
     * 而不是
     * Class.forName("java.lang.Thread$State")
     * }</pre>
     */
    public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
        Assert.notNull(name, "Name must not be null");
        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }
        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        // "[Ljava.lang.String;" style arrays
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = getDefaultClassLoader();
        }
        try {
            return Class.forName(name, false, clToUse);
        } catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String nestedClassName = name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                try {
                    return Class.forName(nestedClassName, false, clToUse);
                } catch (ClassNotFoundException ex2) {
                    // Swallow - let original exception get through
                }
            }
            throw ex;
        }
    }

    /**
     * 根据类名获取对应的Class对象，仅支持基础类型<br/>
     * 提供给{@link #forName(String, ClassLoader)}函数调用
     */
    public static Class<?> resolvePrimitiveClassName(String name) {
        Class<?> result = null;
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if (name != null && name.length() <= 7) {
            // Could be a primitive - likely.
            result = primitiveTypeNameMap.get(name);
        }
        return result;
    }

    /**
     * 返回要使用的默认类加载器
     *
     * @see Thread#getContextClassLoader()
     * @see ClassLoader#getSystemClassLoader()
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }

    /**
     * 尝试加载Class，加载成功返回true
     */
    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            forName(className, classLoader);
            return true;
        } catch (IllegalAccessError err) {
            throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" + className + "]: " + err.getMessage(), err);
        } catch (Throwable ex) {
            // Typically ClassNotFoundException or NoClassDefFoundError...
            return false;
        }
    }

    /**
     * 获取Class的对象类型，如果是基本类型就返回其包装类型，否则返回其自身
     */
    public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
    }

    /**
     * 获取Class的类名(含包名)
     */
    public static String getQualifiedName(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return clazz.getTypeName();
    }

    /**
     * 获取该类所有实现的接口
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
        return getAllInterfacesForClass(clazz, null);
    }

    /**
     * 获取该类所有实现的接口
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
        return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
    }

    /**
     * 获取该类所有实现的接口
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
        return getAllInterfacesForClassAsSet(clazz, null);
    }

    /**
     * 获取该类所有实现的接口
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface() && isVisible(clazz, classLoader)) {
            return Collections.singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null) {
            Class<?>[] ifcs = current.getInterfaces();
            for (Class<?> ifc : ifcs) {
                if (isVisible(ifc, classLoader)) {
                    interfaces.add(ifc);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

    /**
     * 讲集合转换为数组，元素是Class类型
     */
    public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
        return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
    }

    /**
     * 检查ClassLoader中是否存在Class
     */
    public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        try {
            if (clazz.getClassLoader() == classLoader) {
                return true;
            }
        } catch (SecurityException ex) {
            // Fall through to loadable check below
        }
        // Visible if same Class can be loaded from given ClassLoader
        return isLoadable(clazz, classLoader);
    }

    /**
     * 返回给定对象类型的描述性名称：通常只是类名，但类型类名+“[]”表示数组，以及JDK代理的已实现接口的附加列表
     */
    public static String getDescriptiveType(Object value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (Proxy.isProxyClass(clazz)) {
            String prefix = clazz.getName() + " implementing ";
            StringJoiner result = new StringJoiner(",", prefix, "");
            for (Class<?> ifc : clazz.getInterfaces()) {
                result.add(ifc.getName());
            }
            return result.toString();
        } else {
            return clazz.getTypeName();
        }
    }

    /**
     * 检查Class是否在ClassLoader中加载的
     */
    private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
        try {
            return (clazz == classLoader.loadClass(clazz.getName()));
            // Else: different class with same name found
        } catch (ClassNotFoundException ex) {
            // No corresponding class found at all
            return false;
        }
    }

    /**
     * 代替{@code Class.forName()}，如：
     * <pre>{@code
     * Class.forName("int")
     * Class.forName("String[]")
     * }</pre>
     * 支持解析Java源代码样式的嵌套类名，如：
     * <pre>{@code
     * Class.forName("java.lang.Thread.State")
     * 而不是
     * Class.forName("java.lang.Thread$State")
     * }</pre>
     */
    public static Class<?> resolveClassName(String className, ClassLoader classLoader) throws IllegalArgumentException {
        try {
            return forName(className, classLoader);
        } catch (IllegalAccessError err) {
            throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" + className + "]: " + err.getMessage(), err);
        } catch (LinkageError err) {
            throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
        }
    }

    /**
     * 获取Class的类名(不含包名)
     */
    public static String getShortName(Class<?> clazz) {
        return getShortName(getQualifiedName(clazz));
    }

    /**
     * 获取Class的类名(不含包名)
     */
    public static String getShortName(String className) {
        Assert.hasLength(className, "Class name must not be empty");
        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
        if (nameEndIndex == -1) {
            nameEndIndex = className.length();
        }
        String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
        shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
        return shortName;
    }

    /**
     * 获取class的public method，如果不存在返回null
     *
     * @param clazz      指定的class
     * @param methodName 函数名
     * @param paramTypes 参数签名
     * @see Class#getMethod
     */
    public static Method getMethodIfAvailable(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(methodName, "Method name must not be null");
        if (paramTypes != null) {
            return getMethodOrNull(clazz, methodName, paramTypes);
        } else {
            Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            }
            return null;
        }
    }

    /**
     * 获取class的public static method，如果不存在返回null
     *
     * @param clazz      指定的class
     * @param methodName 函数名
     * @param args       参数签名
     */
    public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(methodName, "Method name must not be null");
        try {
            Method method = clazz.getMethod(methodName, args);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * 获取class的构造函数，如果不存在返回null
     *
     * @param clazz      指定的class
     * @param paramTypes 参数签名
     */
    public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
        Assert.notNull(clazz, "Class must not be null");
        try {
            return clazz.getConstructor(paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * 假设通过反射进行设置，确定给定类型是否可以从给定值赋值。
     *
     * @param type  目标类型
     * @param value 应分配给类型的值
     */
    public static boolean isAssignableValue(Class<?> type, Object value) {
        Assert.notNull(type, "Type must not be null");
        return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
    }

    /**
     * 检查给定类是否表示基础类型，即: boolean, byte, char, short, int, long, float, double, void <br/>
     * 或这些类型的包装类型，即: Boolean, Byte, Character, Short, Integer, Long, Float, Double, Void
     */
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
    }

    /**
     * 检查给定类是否表示基本类型的包装类，即: Boolean, Byte, Character, Short, Integer, Long, Float, Double, Void
     */
    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return primitiveWrapperTypeMap.containsKey(clazz);
    }

    /**
     * 检查给定的类在给定的上下文中是否是缓存安全的，即它是由给定的类加载器加载还是由其父类加载
     *
     * @param clazz       要分析的类
     * @param classLoader 类加载器(可能为null，表示系统类加载器)
     */
    public static boolean isCacheSafe(Class<?> clazz, ClassLoader classLoader) {
        Assert.notNull(clazz, "Class must not be null");
        try {
            ClassLoader target = clazz.getClassLoader();
            // Common cases
            if (target == classLoader || target == null) {
                return true;
            }
            if (classLoader == null) {
                return false;
            }
            // Check for match in ancestors -> positive
            ClassLoader current = classLoader;
            while (current != null) {
                current = current.getParent();
                if (current == target) {
                    return true;
                }
            }
            // Check for match in children -> negative
            while (target != null) {
                target = target.getParent();
                if (target == classLoader) {
                    return false;
                }
            }
        } catch (SecurityException ex) {
            // Fall through to loadable check below
        }
        // Fallback for ClassLoaders without parent/child relationship:
        // safe if same Class can be loaded from given ClassLoader
        return (classLoader != null && isLoadable(clazz, classLoader));
    }

    /**
     * 确定给定的接口是否是公共Java语言接口：
     * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
     * {@link Cloneable}, {@link Comparable}
     */
    public static boolean isJavaLanguageInterface(Class<?> ifc) {
        return javaLanguageInterfaces.contains(ifc);
    }

    /**
     * 确定给定类是否具有具有给定签名的公共构造函数
     *
     * @see Class#getConstructor
     */
    public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
        return (getConstructorIfAvailable(clazz, paramTypes) != null);
    }

    /**
     * 给定一个输入类对象，返回一个由类的包名组成的字符串作为路径名，
     * 即所有点('.')都替换为斜杠('/')。既不添加前导斜杠，也不添加尾随斜杠
     */
    public static String classPackageAsResourcePath(Class<?> clazz) {
        if (clazz == null) {
            return "";
        }
        String className = clazz.getName();
        int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        if (packageEndIndex == -1) {
            return "";
        }
        String packageName = className.substring(0, packageEndIndex);
        return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    /**
     * 确定类文件的名称，相对于包含的包：例如“String.class”
     *
     * @param clazz class
     * @return “.class”文件的文件名
     */
    public static String getClassFileName(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        String className = clazz.getName();
        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
    }

    /**
     * 确定给定全限定类名的包名，例如 "java.lang" 为 {@code java.lang.String} 类名。
     *
     * @param clazz class
     * @return 包名，如果类是在默认包中定义的，则为空字符串
     */
    public static String getPackageName(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return getPackageName(clazz.getName());
    }

    /**
     * 确定给定全限定类名的包名，例如 "java.lang" 为 {@code java.lang.String} 类名。
     *
     * @param fqClassName 完全限定的类名
     * @return 包名，如果类是在默认包中定义的，则为空字符串
     */
    public static String getPackageName(String fqClassName) {
        Assert.notNull(fqClassName, "Class name must not be null");
        int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
        return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
    }

    /**
     * 为给定接口创建一个复合接口类，在一个类中实现给定接口。
     * <p>这个实现为给定的接口构建一个JDK代理类。
     *
     * @param interfaces  要合并的接口
     * @param classLoader ClassLoader在中创建复合类
     * @return 合并的接口为Class
     * @throws IllegalArgumentException 如果指定的接口暴露了冲突的方法签名（或违反了类似的约束）
     * @see java.lang.reflect.Proxy#getProxyClass
     */
    public static Class<?> createCompositeInterface(Class<?>[] interfaces, ClassLoader classLoader) {
        Assert.notEmpty(interfaces, "Interface array must not be empty");
        return Proxy.getProxyClass(classLoader, interfaces);
    }

    private static Method getMethodOrNull(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static Set<Method> findMethodCandidatesByName(Class<?> clazz, String methodName) {
        Set<Method> candidates = new HashSet<>(1);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                candidates.add(method);
            }
        }
        return candidates;
    }
}
