package org.clever.beans;

import org.clever.core.convert.TypeDescriptor;
import org.clever.util.ClassUtils;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 缓存Java类的JavaBeans {@link java.beans.PropertyDescriptor}信息的内部类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:27 <br/>
 *
 * @see #acceptClassLoader(ClassLoader)
 * @see #clearClassLoader(ClassLoader)
 * @see #forClass(Class)
 */
public final class CachedIntrospectionResults {
    private static final Logger logger = LoggerFactory.getLogger(CachedIntrospectionResults.class);
    private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = {};
    /**
     * 内省者是否忽略BeanInfo类
     */
    private static final boolean shouldIntrospectorIgnoreBeaninfoClasses = false;
    /**
     * 存储BeanInfoFactory实例。
     */
    private static final List<BeanInfoFactory> beanInfoFactories = Collections.emptyList();
    /**
     * 这个CachedIntrospectionResults类将始终接受来自的类的类加载器集，即使这些类不符合缓存安全的条件
     */
    static final Set<ClassLoader> acceptedClassLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
    /**
     * 由包含CachedIntrospectionResults的类设置关键字的映射。用于缓存安全bean类
     */
    static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache = new ConcurrentHashMap<>(64);
    /**
     * 由包含CachedIntrospectionResults的类设置关键帧的映射。用于非缓存安全bean类
     */
    static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache = new ConcurrentReferenceHashMap<>(64);

    /**
     * 接受给定的类加载器作为缓存安全的，即使其类在此CachedIntrospectionResults类中不符合缓存安全的条件。
     *
     * @param classLoader 要接受的类加载器
     */
    public static void acceptClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            acceptedClassLoaders.add(classLoader);
        }
    }

    /**
     * 清除给定类加载器的内省缓存，删除该类加载器下所有类的内省结果，并从接受列表中删除类加载器(及其子类)
     *
     * @param classLoader 清除缓存的类加载器
     */
    public static void clearClassLoader(ClassLoader classLoader) {
        acceptedClassLoaders.removeIf(registeredLoader -> isUnderneathClassLoader(registeredLoader, classLoader));
        strongClassCache.keySet().removeIf(beanClass -> isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
        softClassCache.keySet().removeIf(beanClass -> isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
    }

    /**
     * 为给定bean类创建CachedIntrospectionResults
     *
     * @param beanClass 要分析的bean类
     * @throws BeansException 如果内省失败
     */
    static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
        CachedIntrospectionResults results = strongClassCache.get(beanClass);
        if (results != null) {
            return results;
        }
        results = softClassCache.get(beanClass);
        if (results != null) {
            return results;
        }
        results = new CachedIntrospectionResults(beanClass);
        ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse;

        if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader()) || isClassLoaderAccepted(beanClass.getClassLoader())) {
            classCacheToUse = strongClassCache;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
            }
            classCacheToUse = softClassCache;
        }
        CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
        return (existing != null ? existing : results);
    }

    /**
     * 检查此CachedIntrospectionResults类是否配置为接受给定的类加载器
     *
     * @param classLoader 要检查的类加载器
     * @see #acceptClassLoader
     */
    private static boolean isClassLoaderAccepted(ClassLoader classLoader) {
        for (ClassLoader acceptedLoader : acceptedClassLoaders) {
            if (isUnderneathClassLoader(classLoader, acceptedLoader)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查给定的类加载器是否在给定的父级之下，即父级是否在候选的层次结构中
     *
     * @param candidate 要检查的候选类加载器
     * @param parent    要检查的父类加载器
     */
    private static boolean isUnderneathClassLoader(ClassLoader candidate, ClassLoader parent) {
        if (candidate == parent) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        ClassLoader classLoaderToCheck = candidate;
        while (classLoaderToCheck != null) {
            classLoaderToCheck = classLoaderToCheck.getParent();
            if (classLoaderToCheck == parent) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检索给定目标类的{@link BeanInfo}描述符
     *
     * @param beanClass 要内省的目标类
     * @throws IntrospectionException 来自潜在的内省者
     */
    private static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        for (BeanInfoFactory beanInfoFactory : beanInfoFactories) {
            BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanClass);
            if (beanInfo != null) {
                return beanInfo;
            }
        }
        return shouldIntrospectorIgnoreBeaninfoClasses ?
                Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO) :
                Introspector.getBeanInfo(beanClass);
    }

    /**
     * 内省bean类的BeanInfo对象
     */
    private final BeanInfo beanInfo;
    /**
     * {@code Map<属性名称字符串, PropertyDescriptor对象>}
     */
    private final Map<String, PropertyDescriptor> propertyDescriptors;
    /**
     * {@code Map<PropertyDescriptor对象, TypeDescriptor对象>}
     */
    private final ConcurrentMap<PropertyDescriptor, TypeDescriptor> typeDescriptorCache;

    /**
     * 为给定类创建一个新的CachedIntrospectionResults实例
     *
     * @param beanClass 要分析的bean类
     * @throws BeansException 如果内省失败
     */
    private CachedIntrospectionResults(Class<?> beanClass) throws BeansException {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
            }
            this.beanInfo = getBeanInfo(beanClass);
            if (logger.isTraceEnabled()) {
                logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
            }
            this.propertyDescriptors = new LinkedHashMap<>();
            Set<String> readMethodNames = new HashSet<>();
            // This call is slow so we do it once.
            PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (Class.class == beanClass && (!"name".equals(pd.getName()) && !pd.getName().endsWith("Name"))) {
                    // Only allow all name variants of Class properties
                    continue;
                }
                if (pd.getPropertyType() != null
                        && (ClassLoader.class.isAssignableFrom(pd.getPropertyType()) || ProtectionDomain.class.isAssignableFrom(pd.getPropertyType()))) {
                    // Ignore ClassLoader and ProtectionDomain types - nobody needs to bind to those
                    continue;
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Found bean property '" + pd.getName() + "'" +
                            (pd.getPropertyType() != null ? " of type [" + pd.getPropertyType().getName() + "]" : "") +
                            (pd.getPropertyEditorClass() != null ? "; editor [" + pd.getPropertyEditorClass().getName() + "]" : "")
                    );
                }
                pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
                this.propertyDescriptors.put(pd.getName(), pd);
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    readMethodNames.add(readMethod.getName());
                }
            }
            // Explicitly check implemented interfaces for setter/getter methods as well,
            // in particular for Java 8 default methods...
            Class<?> currClass = beanClass;
            while (currClass != null && currClass != Object.class) {
                introspectInterfaces(beanClass, currClass, readMethodNames);
                currClass = currClass.getSuperclass();
            }
            // Check for record-style accessors without prefix: e.g. "lastName()"
            // - accessor method directly referring to instance field of same name
            // - same convention for component accessors of Java 15 record classes
            assert beanClass != null;
            introspectPlainAccessors(beanClass, readMethodNames);
            this.typeDescriptorCache = new ConcurrentReferenceHashMap<>();
        } catch (IntrospectionException ex) {
            throw new FatalBeanException("Failed to obtain BeanInfo for class [" + beanClass.getName() + "]", ex);
        }
    }

    private void introspectInterfaces(Class<?> beanClass, Class<?> currClass, Set<String> readMethodNames) throws IntrospectionException {
        for (Class<?> ifc : currClass.getInterfaces()) {
            if (!ClassUtils.isJavaLanguageInterface(ifc)) {
                for (PropertyDescriptor pd : getBeanInfo(ifc).getPropertyDescriptors()) {
                    PropertyDescriptor existingPd = this.propertyDescriptors.get(pd.getName());
                    if (existingPd == null || (existingPd.getReadMethod() == null && pd.getReadMethod() != null)) {
                        // GenericTypeAwarePropertyDescriptor leniently resolves a set* write method
                        // against a declared read method, so we prefer read method descriptors here.
                        pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
                        if (pd.getPropertyType() != null &&
                                (ClassLoader.class.isAssignableFrom(pd.getPropertyType()) || ProtectionDomain.class.isAssignableFrom(pd.getPropertyType()))) {
                            // Ignore ClassLoader and ProtectionDomain types - nobody needs to bind to those
                            continue;
                        }
                        this.propertyDescriptors.put(pd.getName(), pd);
                        Method readMethod = pd.getReadMethod();
                        if (readMethod != null) {
                            readMethodNames.add(readMethod.getName());
                        }
                    }
                }
                introspectInterfaces(ifc, ifc, readMethodNames);
            }
        }
    }

    private void introspectPlainAccessors(Class<?> beanClass, Set<String> readMethodNames) throws IntrospectionException {
        for (Method method : beanClass.getMethods()) {
            if (!this.propertyDescriptors.containsKey(method.getName()) &&
                    !readMethodNames.contains((method.getName())) && isPlainAccessor(method)) {
                this.propertyDescriptors.put(method.getName(),
                        new GenericTypeAwarePropertyDescriptor(beanClass, method.getName(), method, null, null));
                readMethodNames.add(method.getName());
            }
        }
    }

    private boolean isPlainAccessor(Method method) {
        if (method.getParameterCount() > 0
                || method.getReturnType() == void.class
                || method.getDeclaringClass() == Object.class
                || Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        try {
            // Accessor method referring to instance field of same name?
            method.getDeclaringClass().getDeclaredField(method.getName());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    BeanInfo getBeanInfo() {
        return this.beanInfo;
    }

    Class<?> getBeanClass() {
        return this.beanInfo.getBeanDescriptor().getBeanClass();
    }

    PropertyDescriptor getPropertyDescriptor(String name) {
        PropertyDescriptor pd = this.propertyDescriptors.get(name);
        if (pd == null && StringUtils.hasLength(name)) {
            // Same lenient fallback checking as in Property...
            pd = this.propertyDescriptors.get(StringUtils.uncapitalize(name));
            if (pd == null) {
                pd = this.propertyDescriptors.get(StringUtils.capitalize(name));
            }
        }
        return pd;
    }

    PropertyDescriptor[] getPropertyDescriptors() {
        return this.propertyDescriptors.values().toArray(EMPTY_PROPERTY_DESCRIPTOR_ARRAY);
    }

    private PropertyDescriptor buildGenericTypeAwarePropertyDescriptor(Class<?> beanClass, PropertyDescriptor pd) {
        try {
            return new GenericTypeAwarePropertyDescriptor(
                    beanClass,
                    pd.getName(),
                    pd.getReadMethod(),
                    pd.getWriteMethod(),
                    pd.getPropertyEditorClass()
            );
        } catch (IntrospectionException ex) {
            throw new FatalBeanException("Failed to re-introspect class [" + beanClass.getName() + "]", ex);
        }
    }

    TypeDescriptor addTypeDescriptor(PropertyDescriptor pd, TypeDescriptor td) {
        TypeDescriptor existing = this.typeDescriptorCache.putIfAbsent(pd, td);
        return (existing != null ? existing : td);
    }

    TypeDescriptor getTypeDescriptor(PropertyDescriptor pd) {
        return this.typeDescriptorCache.get(pd);
    }
}
