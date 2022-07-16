package org.clever.beans;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.clever.core.*;
import org.clever.util.*;

import java.beans.ConstructorProperties;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * JavaBeans的静态便捷方法：用于实例化bean、检查bean属性类型、复制bean属性等
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 13:51 <br/>
 */
public abstract class BeanUtils {
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    /**
     * 缓存没有编辑器的类型集合
     */
    private static final Set<Class<?>> unknownEditorTypes = Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));
    /**
     * 类型默认值
     */
    private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

    static {
        Map<Class<?>, Object> values = new HashMap<>();
        values.put(boolean.class, false);
        values.put(byte.class, (byte) 0);
        values.put(short.class, (short) 0);
        values.put(int.class, 0);
        values.put(long.class, 0L);
        values.put(float.class, 0F);
        values.put(double.class, 0D);
        values.put(char.class, '\0');
        DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
    }

    /**
     * 使用其无参数构造函数实例化类
     *
     * @param clazz 要实例化的类
     * @throws BeanInstantiationException 如果bean无法实例化
     * @see Class#newInstance()
     */
    public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
        }
    }

    /**
     * 使用其“主”构造函数(对于Kotlin类，可能声明了默认参数)或其默认构造函数(对于常规Java类，需要标准的无参数设置)实例化一个类<br/>
     * 请注意，如果给定了不可访问(即非公共)构造函数，此方法将尝试将构造函数设置为可访问
     *
     * @param clazz 要实例化的类
     * @throws BeanInstantiationException 如果bean无法实例化
     * @see Constructor#newInstance
     */
    public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            Constructor<T> ctor = findPrimaryConstructor(clazz);
            if (ctor != null) {
                return instantiateClass(ctor);
            }
            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
        } catch (LinkageError err) {
            throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
        }
    }

    /**
     * 使用其无参数构造函数实例化一个类，并将新实例作为指定的可赋值类型返回<br/>
     * 在要实例化的类的类型(clazz)不可用，但所需的类型(assignableTo)已知的情况下很有用<br/>
     * 请注意，如果给定了不可访问(即非公共)构造函数，此方法将尝试将构造函数设置为可访问
     *
     * @param clazz        要实例化的类
     * @param assignableTo clazz必须可分配给的类型
     * @throws BeanInstantiationException 如果bean无法实例化
     * @see Constructor#newInstance
     */
    @SuppressWarnings("unchecked")
    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
        Assert.isAssignable(assignableTo, clazz);
        return (T) instantiateClass(clazz);
    }

    /**
     * 使用给定构造函数实例化类<br/>
     * 请注意，如果给定了不可访问(即非公共)构造函数，此方法会尝试将构造函数设置为可访问，并支持具有可选参数和默认值的Kotlin类
     *
     * @param ctor 要实例化的构造函数
     * @param args 要应用的构造函数参数(对未指定的参数使用null，支持Kotlin可选参数和Java基元类型)
     * @throws BeanInstantiationException 如果bean无法实例化
     * @see Constructor#newInstance
     */
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            ReflectionUtils.makeAccessible(ctor);
            if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
                return KotlinDelegate.instantiateClass(ctor, args);
            } else {
                Class<?>[] parameterTypes = ctor.getParameterTypes();
                Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
                Object[] argsWithDefaultValues = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null) {
                        Class<?> parameterType = parameterTypes[i];
                        argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                    } else {
                        argsWithDefaultValues[i] = args[i];
                    }
                }
                return ctor.newInstance(argsWithDefaultValues);
            }
        } catch (InstantiationException ex) {
            throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
        } catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
        } catch (InvocationTargetException ex) {
            throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
        }
    }

    /**
     * 返回所提供类的可解析构造函数，可以是带参数的主构造函数或单个公共构造函数，也可以是带参数的单个非公共构造函数，也可以是默认构造函数。
     * 调用者必须准备好解析返回的构造函数参数的参数（如果有）
     *
     * @param clazz 要检查的类
     * @throws IllegalStateException 如果没有找到唯一的构造函数
     * @see #findPrimaryConstructor
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getResolvableConstructor(Class<T> clazz) {
        Constructor<T> ctor = findPrimaryConstructor(clazz);
        if (ctor != null) {
            return ctor;
        }

        Constructor<?>[] ctors = clazz.getConstructors();
        if (ctors.length == 1) {
            // A single public constructor
            return (Constructor<T>) ctors[0];
        } else if (ctors.length == 0) {
            ctors = clazz.getDeclaredConstructors();
            if (ctors.length == 1) {
                // A single non-public constructor, e.g. from a non-public record type
                return (Constructor<T>) ctors[0];
            }
        }

        // Several constructors -> let's try to take the default constructor
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            // Giving up...
        }

        // No unique constructor at all
        throw new IllegalStateException("No primary or single unique constructor found for " + clazz);
    }

    /**
     * 返回所提供类的主构造函数。
     * 对于Kotlin类，这将返回与Kotlin主构造函数(如Kotlin规范中所定义)相对应的Java构造函数。
     * 否则，特别是对于非Kotlin类，这只会返回null
     *
     * @param clazz 要检查的类
     * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
     */
    public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
            return KotlinDelegate.findPrimaryConstructor(clazz);
        }
        return null;
    }

    /**
     * 查找具有给定方法名称和给定参数类型的方法，该方法在给定类或其超类之一上声明。
     * 首选公共方法，但也将返回受保护的、包访问或私有方法。
     * 首先是{@code Class.getMethod}，然后{@code findDeclaredMethod}。
     * 这样，即使在Java安全设置受限的环境中，也可以找到没有问题的公共方法。
     *
     * @param clazz      要检查的类
     * @param methodName 要查找的方法的名称
     * @param paramTypes 要查找的方法的参数类型
     * @see Class#getMethod
     * @see #findDeclaredMethod
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return findDeclaredMethod(clazz, methodName, paramTypes);
        }
    }

    /**
     * 查找具有给定方法名称和给定参数类型的方法，该方法在给定类或其超类之一上声明。
     * 将返回公共、受保护、包访问或私有方法。检查{@code Class.getDeclaredMethod}，向上级联到所有超类
     *
     * @param clazz      要检查的类
     * @param methodName 要查找的方法的名称
     * @param paramTypes 要查找的方法的参数类型
     * @see Class#getDeclaredMethod
     */
    public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
            }
            return null;
        }
    }

    /**
     * 查找具有给定方法名称和最小参数(最佳情况：none)的方法，该方法在给定类或其一个超类上声明。
     * 首选公共方法，但也将返回受保护的、包访问或私有方法。检查类别。
     * 首先是{@code Class.getMethods}，然后是{@code findDeclaredMethodWithMinimalParameters}。
     * 这使得即使在具有受限Java安全设置的环境中也可以找到没有问题的公共方法
     *
     * @param clazz      要检查的类
     * @param methodName 要查找的方法的名称
     * @throws IllegalArgumentException 如果找到了具有给定名称的方法，但无法解析为具有最小参数的唯一方法
     * @see Class#getMethods
     * @see #findDeclaredMethodWithMinimalParameters
     */
    public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName) throws IllegalArgumentException {
        Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
        if (targetMethod == null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
        }
        return targetMethod;
    }

    /**
     * 查找具有给定方法名称和最小参数(最佳情况：none)的方法，该方法在给定类或其一个超类上声明。
     * 将返回公共、受保护、包访问或私有方法。检查{@code Class.getDeclaredMethods}，向上级联到所有超类
     *
     * @param clazz      要检查的类
     * @param methodName 要查找的方法的名称
     * @throws IllegalArgumentException 如果找到了具有给定名称的方法，但无法解析为具有最小参数的唯一方法
     * @see Class#getDeclaredMethods
     */
    public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName) throws IllegalArgumentException {
        Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
        if (targetMethod == null && clazz.getSuperclass() != null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
        }
        return targetMethod;
    }

    /**
     * 在给定的方法列表中查找具有给定方法名称和最小参数(最佳情况：none)的方法
     *
     * @param methods    检查方法
     * @param methodName 要查找的方法的名称
     * @throws IllegalArgumentException 如果找到了具有给定名称的方法，但无法解析为具有最小参数的唯一方法
     */
    public static Method findMethodWithMinimalParameters(Method[] methods, String methodName) throws IllegalArgumentException {
        Method targetMethod = null;
        int numMethodsFoundWithCurrentMinimumArgs = 0;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                int numParams = method.getParameterCount();
                if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
                    targetMethod = method;
                    numMethodsFoundWithCurrentMinimumArgs = 1;
                } else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
                    if (targetMethod.isBridge()) {
                        // Prefer regular method over bridge...
                        targetMethod = method;
                    } else {
                        // Additional candidate with same length
                        numMethodsFoundWithCurrentMinimumArgs++;
                    }
                }
            }
        }
        if (numMethodsFoundWithCurrentMinimumArgs > 1) {
            throw new IllegalArgumentException("Cannot resolve method '" + methodName +
                    "' to a unique method. Attempted to resolve to overloaded method with " +
                    "the least number of parameters but there were " +
                    numMethodsFoundWithCurrentMinimumArgs + " candidates."
            );
        }
        return targetMethod;
    }

    /**
     * 分析{@code methodName[([arg_list])]}形式的方法签名，
     * 其中{@code arg_list}是一个可选的、以逗号分隔的完全限定类型名称列表，并尝试根据提供的类解析该签名。
     * 当不提供参数列表{@code methodName}时，将返回名称匹配且参数数最少的方法。
     * 提供参数类型列表时，只返回名称和参数类型匹配的方法。
     * 然后请注意，{@code methodName}和{@code methodName()}的解析方式不同。
     * 签名{@code methodName}表示具有最少参数的{@code methodName}方法，而{@code methodName()}表示具有0个参数的methodName方法。
     * 如果找不到方法，则返回null
     *
     * @param signature 作为字符串表示的方法签名
     * @param clazz     要根据其解析方法签名的类
     * @see #findMethod
     * @see #findMethodWithMinimalParameters
     */
    public static Method resolveSignature(String signature, Class<?> clazz) {
        Assert.hasText(signature, "'signature' must not be empty");
        Assert.notNull(clazz, "Class must not be null");
        int startParen = signature.indexOf('(');
        int endParen = signature.indexOf(')');
        if (startParen > -1 && endParen == -1) {
            throw new IllegalArgumentException("Invalid method signature '" + signature + "': expected closing ')' for args list");
        } else if (startParen == -1 && endParen > -1) {
            throw new IllegalArgumentException("Invalid method signature '" + signature + "': expected opening '(' for args list");
        } else if (startParen == -1) {
            return findMethodWithMinimalParameters(clazz, signature);
        } else {
            String methodName = signature.substring(0, startParen);
            String[] parameterTypeNames = StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
            Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                String parameterTypeName = parameterTypeNames[i].trim();
                try {
                    parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
                } catch (Throwable ex) {
                    throw new IllegalArgumentException(
                            "Invalid method signature: unable to resolve type [" + parameterTypeName + "] for argument " + i + ". Root cause: " + ex
                    );
                }
            }
            return findMethod(clazz, methodName, parameterTypes);
        }
    }

    /**
     * 检索给定类的JavaBeans {@code PropertyDescriptor}s
     *
     * @param clazz 要为其检索PropertyDescriptors的类
     * @return 给定类的PropertyDescriptor数组
     * @throws BeansException 如果PropertyDescriptor查找失败
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
        return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptors();
    }

    /**
     * 检索给定属性的JavaBeans PropertyDescriptors
     *
     * @param clazz        要为其检索PropertyDescriptor的类
     * @param propertyName 属性的名称
     * @return 对应的PropertyDescriptor，如果没有，则为null
     * @throws BeansException 如果PropertyDescriptor查找失败
     */
    public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {
        return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptor(propertyName);
    }

    /**
     * 找到给定方法的JavaBeans PropertyDescriptor，该方法可以是该bean属性的读方法，也可以是写方法
     *
     * @param method 用于查找的相应PropertyDescriptor的方法，内省其声明类
     * @return 对应的PropertyDescriptor，如果没有，则为null
     * @throws BeansException 如果PropertyDescriptor查找失败
     */
    public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
        return findPropertyForMethod(method, method.getDeclaringClass());
    }

    /**
     * 找到给定方法的JavaBeans PropertyDescriptor，该方法可以是该bean属性的读方法，也可以是写方法
     *
     * @param method 查找的相应PropertyDescriptor的方法
     * @param clazz  对描述符进行内省的(最具体的)类
     * @return 对应的PropertyDescriptor，如果没有，则为null
     * @throws BeansException 如果PropertyDescriptor查找失败
     */
    public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
        Assert.notNull(method, "Method must not be null");
        PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
        for (PropertyDescriptor pd : pds) {
            if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
                return pd;
            }
        }
        return null;
    }

    /**
     * 按照“Editor”后缀约定查找JavaBeans属性编辑器（例如“mypackage.MyDomainClass”）→ “mypackage.MyDomainClassEditor”）。
     * 与java实现的标准JavaBeans约定兼容{@link java.beans.PropertyEditorManager}，但与后者为基元类型注册的默认编辑器隔离
     *
     * @param targetType 要查找编辑器的类型
     * @return 相应的编辑器，如果未找到，则为null
     */
    public static PropertyEditor findEditorByConvention(Class<?> targetType) {
        if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
            return null;
        }
        ClassLoader cl = targetType.getClassLoader();
        if (cl == null) {
            try {
                cl = ClassLoader.getSystemClassLoader();
                if (cl == null) {
                    return null;
                }
            } catch (Throwable ex) {
                // e.g. AccessControlException on Google App Engine
                return null;
            }
        }
        String targetTypeName = targetType.getName();
        String editorName = targetTypeName + "Editor";
        try {
            Class<?> editorClass = cl.loadClass(editorName);
            if (editorClass != null) {
                if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
                    unknownEditorTypes.add(targetType);
                    return null;
                }
                return (PropertyEditor) instantiateClass(editorClass);
            }
            // Misbehaving ClassLoader returned null instead of ClassNotFoundException
            // - fall back to unknown editor type registration below
        } catch (ClassNotFoundException ex) {
            // Ignore - fall back to unknown editor type registration below
        }
        unknownEditorTypes.add(targetType);
        return null;
    }

    /**
     * 如果可能的话，从给定的classes/interfaces中确定给定属性的bean属性类型
     *
     * @param propertyName bean属性的名称
     * @param beanClasses  要检查的类
     * @return 属性类型或{@code Object.class}作为回退
     */
    public static Class<?> findPropertyType(String propertyName, Class<?>... beanClasses) {
        if (beanClasses != null) {
            for (Class<?> beanClass : beanClasses) {
                PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
                if (pd != null) {
                    return pd.getPropertyType();
                }
            }
        }
        return Object.class;
    }

    /**
     * 为指定属性的write方法获取新的MethodParameter对象
     *
     * @param pd 属性的PropertyDescriptor
     * @return 对应的MethodParameter对象
     */
    public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
        if (pd instanceof GenericTypeAwarePropertyDescriptor) {
            return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
        } else {
            Method writeMethod = pd.getWriteMethod();
            Assert.state(writeMethod != null, "No write method available");
            return new MethodParameter(writeMethod, 0);
        }
    }

    /**
     * 考虑JavaBeans {@link ConstructorProperties}注释以及{@link DefaultParameterNameDiscoverer}，确定给定构造函数所需的参数名称
     *
     * @param ctor 要为其查找参数名称的构造函数
     * @return 参数名称(与构造函数的参数计数匹配)
     * @throws IllegalStateException 如果参数名称不可解析
     * @see ConstructorProperties
     * @see DefaultParameterNameDiscoverer
     */
    public static String[] getParameterNames(Constructor<?> ctor) {
        ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
        String[] paramNames = (cp != null ? cp.value() : parameterNameDiscoverer.getParameterNames(ctor));
        Assert.state(paramNames != null, () -> "Cannot resolve parameter names for constructor " + ctor);
        Assert.state(
                paramNames.length == ctor.getParameterCount(),
                () -> "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor
        );
        return paramNames;
    }

    /**
     * 检查给定类型是否表示“simple”属性：简单值类型或简单值类型数组。
     * 有关简单值类型的定义，请参见{@link #isSimpleValueType(Class)}。
     * 用于确定要检查“simple”依赖项检查的属性
     *
     * @param type 要检查的类型
     * @return 给定类型是否表示“simple”属性
     * @see #isSimpleValueType(Class)
     */
    public static boolean isSimpleProperty(Class<?> type) {
        Assert.notNull(type, "'type' must not be null");
        return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
    }

    /**
     * 检查给定类型是否表示“simple”值类型：基元或基元wrapper、enum、String或其他CharSequence、Number、Date、Temporal、URI、URL、Locale或Class。
     * {@code Void}和{@code void}不是简单的值类型
     *
     * @param type 要检查的类型
     * @return 给定类型是否表示“simple”值类型
     * @see #isSimpleProperty(Class)
     */
    public static boolean isSimpleValueType(Class<?> type) {
        return Void.class != type
                && void.class != type
                && (
                ClassUtils.isPrimitiveOrWrapper(type)
                        || Enum.class.isAssignableFrom(type)
                        || CharSequence.class.isAssignableFrom(type)
                        || Number.class.isAssignableFrom(type)
                        || Date.class.isAssignableFrom(type)
                        || Temporal.class.isAssignableFrom(type)
                        || URI.class == type
                        || URL.class == type
                        || Locale.class == type
                        || Class.class == type
        );
    }

    /**
     * 将给定源bean的属性值复制到目标bean中。
     * 注意：只要属性匹配，源类和目标类就不必相互匹配，甚至不必相互派生。源bean公开但目标bean没有公开的任何bean属性都将被忽略。
     * 这只是一种方便的方法。对于更复杂的传输需求，请考虑使用完整的BeanWrapper。
     * 当匹配源对象和目标对象中的属性时，此方法将接受泛型类型信息。
     * 下表提供了一组可以复制的源和目标属性类型以及不能复制的源和目标属性类型的非详尽示例。
     * <table border="1">
     * <tr><th>源属性类型</th><th>目标属性类型</th><th>支持复制</th></tr>
     * <tr><td>{@code Integer}</td><td>{@code Integer}</td><td>yes</td></tr>
     * <tr><td>{@code Integer}</td><td>{@code Number}</td><td>yes</td></tr>
     * <tr><td>{@code List<Integer>}</td><td>{@code List<Integer>}</td><td>yes</td></tr>
     * <tr><td>{@code List<?>}</td><td>{@code List<?>}</td><td>yes</td></tr>
     * <tr><td>{@code List<Integer>}</td><td>{@code List<?>}</td><td>yes</td></tr>
     * <tr><td>{@code List<Integer>}</td><td>{@code List<? extends Number>}</td><td>yes</td></tr>
     * <tr><td>{@code String}</td><td>{@code Integer}</td><td>no</td></tr>
     * <tr><td>{@code Number}</td><td>{@code Integer}</td><td>no</td></tr>
     * <tr><td>{@code List<Integer>}</td><td>{@code List<Long>}</td><td>no</td></tr>
     * <tr><td>{@code List<Integer>}</td><td>{@code List<Number>}</td><td>no</td></tr>
     * </table>
     *
     * @param source 源bean
     * @param target 目标bean
     * @throws BeansException 如果复制失败
     * @see BeanWrapper
     */
    public static void copyProperties(Object source, Object target) throws BeansException {
        copyProperties(source, target, null, (String[]) null);
    }

    /**
     * 将给定源bean的属性值复制到给定的目标bean中，只设置在给定的“editable”类(或接口)中定义的属性。
     * 注意：只要属性匹配，源类和目标类就不必相互匹配，甚至不必相互派生。
     * 源bean公开但目标bean没有公开的任何bean属性都将被忽略。
     * 这只是一种方便的方法。对于更复杂的传输需求，请考虑使用完整的{@link BeanWrapper}。
     * 当匹配源对象和目标对象中的属性时，此方法将接受泛型类型信息。有关详细信息，请参阅{@link #copyProperties(Object, Object)}的文档。
     *
     * @param source   源bean
     * @param target   目标bean
     * @param editable 要将属性设置限制为的类(或接口)
     * @throws BeansException 如果复制失败
     * @see BeanWrapper
     */
    public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyProperties(source, target, editable, (String[]) null);
    }

    /**
     * 将给定源bean的属性值复制到给定目标bean中，忽略给定的“ignoreProperties”。
     * 注意：只要属性匹配，源类和目标类就不必相互匹配，甚至不必相互派生。
     * 源bean公开但目标bean没有公开的任何bean属性都将被忽略。
     * 这只是一种方便的方法。对于更复杂的传输需求，请考虑使用完整的{@link BeanWrapper}。
     * 当匹配源对象和目标对象中的属性时，此方法将接受泛型类型信息。
     * 有关详细信息，请参阅{@link #copyProperties(Object, Object)}的文档。
     *
     * @param source           源bean
     * @param target           目标bean
     * @param ignoreProperties 要忽略的属性名称数组
     * @throws BeansException 如果复制失败
     * @see BeanWrapper
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
        copyProperties(source, target, null, ignoreProperties);
    }

    /**
     * 将给定源bean的属性值复制到给定目标bean中。
     * 注意：只要属性匹配，源类和目标类就不必相互匹配，甚至不必相互派生。
     * 源bean公开但目标bean没有公开的任何bean属性都将被忽略。
     * 当匹配源对象和目标对象中的属性时，此方法将接受泛型类型信息。
     * 有关详细信息，请参阅{@link #copyProperties(Object, Object)}的文档。
     *
     * @param source           源bean
     * @param target           目标bean
     * @param editable         要将属性设置限制为的类(或接口)
     * @param ignoreProperties 要忽略的属性名称数组
     * @throws BeansException 如果复制失败
     * @see BeanWrapper
     */
    private static void copyProperties(Object source, Object target, Class<?> editable, String... ignoreProperties) throws BeansException {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");
        Class<?> actualEditable = target.getClass();
        if (editable != null) {
            if (!editable.isInstance(target)) {
                throw new IllegalArgumentException(
                        "Target class [" + target.getClass().getName() + "] not assignable to Editable class [" + editable.getName() + "]"
                );
            }
            actualEditable = editable;
        }
        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);
        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null) {
                        ResolvableType sourceResolvableType = ResolvableType.forMethodReturnType(readMethod);
                        ResolvableType targetResolvableType = ResolvableType.forMethodParameter(writeMethod, 0);
                        // Ignore generic types in assignable check if either ResolvableType has unresolvable generics.
                        boolean isAssignable = (
                                sourceResolvableType.hasUnresolvableGenerics() || targetResolvableType.hasUnresolvableGenerics() ?
                                        ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType()) :
                                        targetResolvableType.isAssignableFrom(sourceResolvableType)
                        );
                        if (isAssignable) {
                            try {
                                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                    readMethod.setAccessible(true);
                                }
                                Object value = readMethod.invoke(source);
                                if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                    writeMethod.setAccessible(true);
                                }
                                writeMethod.invoke(target, value);
                            } catch (Throwable ex) {
                                throw new FatalBeanException(
                                        "Could not copy property '" + targetPd.getName() + "' from source to target",
                                        ex
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 内部类，以避免在运行时对Kotlin产生硬依赖
     */
    private static class KotlinDelegate {
        /**
         * 检索与Kotlin主构造函数相对应的Java构造函数(如果有)
         *
         * @param clazz Kotlin类
         * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">https://kotlinlang.org/docs/reference/classes.html#constructors</a>
         */
        public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
            try {
                KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
                if (primaryCtor == null) {
                    return null;
                }
                Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
                if (constructor == null) {
                    throw new IllegalStateException("Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
                }
                return constructor;
            } catch (UnsupportedOperationException ex) {
                return null;
            }
        }

        /**
         * 使用提供的构造函数实例化Kotlin类
         *
         * @param ctor 要实例化的Kotlin类的构造函数
         * @param args 要应用的构造函数参数(如果需要，请对未指定的参数使用null)
         */
        public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
            if (kotlinConstructor == null) {
                return ctor.newInstance(args);
            }
            if ((!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))) {
                KCallablesJvm.setAccessible(kotlinConstructor, true);
            }
            List<KParameter> parameters = kotlinConstructor.getParameters();
            Map<KParameter, Object> argParameters = CollectionUtils.newHashMap(parameters.size());
            Assert.isTrue(
                    args.length <= parameters.size(),
                    "Number of provided arguments should be less of equals than number of constructor parameters"
            );
            for (int i = 0; i < args.length; i++) {
                if (!(parameters.get(i).isOptional() && args[i] == null)) {
                    argParameters.put(parameters.get(i), args[i]);
                }
            }
            return kotlinConstructor.callBy(argParameters);
        }
    }
}
