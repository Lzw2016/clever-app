package org.clever.core;

import org.clever.util.ClassUtils;

import java.io.*;

/**
 * 针对特定ClassLoader解析类名的特殊ObjectInputStream子类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:41 <br/>
 */
public class ConfigurableObjectInputStream extends ObjectInputStream {
    private final ClassLoader classLoader;
    private final boolean acceptProxyClasses;

    /**
     * 为给定的InputStream和ClassLoader创建一个新的ConfigurationObjectInputStream。
     *
     * @param in          要读取的InputStream
     * @param classLoader 用于加载本地类的ClassLoader
     * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
     */
    public ConfigurableObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        this(in, classLoader, true);
    }

    /**
     * 为给定的InputStream和ClassLoader创建一个新的ConfigurationObjectInputStream。
     *
     * @param in                 要读取的InputStream
     * @param classLoader        用于加载本地类的ClassLoader
     * @param acceptProxyClasses 是否接受代理类的反序列化（可以作为安全措施停用）
     * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
     */
    public ConfigurableObjectInputStream(InputStream in, ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {
        super(in);
        this.classLoader = classLoader;
        this.acceptProxyClasses = acceptProxyClasses;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        try {
            if (this.classLoader != null) {
                // Use the specified ClassLoader to resolve local classes.
                return ClassUtils.forName(classDesc.getName(), this.classLoader);
            } else {
                // Use the default ClassLoader...
                return super.resolveClass(classDesc);
            }
        } catch (ClassNotFoundException ex) {
            return resolveFallbackIfPossible(classDesc.getName(), ex);
        }
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        if (!this.acceptProxyClasses) {
            throw new NotSerializableException("Not allowed to accept serialized proxy classes");
        }
        if (this.classLoader != null) {
            // 使用指定的ClassLoader解析本地代理类
            Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                try {
                    resolvedInterfaces[i] = ClassUtils.forName(interfaces[i], this.classLoader);
                } catch (ClassNotFoundException ex) {
                    resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
                }
            }
            try {
                return ClassUtils.createCompositeInterface(resolvedInterfaces, this.classLoader);
            } catch (IllegalArgumentException ex) {
                throw new ClassNotFoundException(null, ex);
            }
        } else {
            // 使用ObjectInputStream的默认ClassLoader...
            try {
                return super.resolveProxyClass(interfaces);
            } catch (ClassNotFoundException ex) {
                Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
                }
                return ClassUtils.createCompositeInterface(resolvedInterfaces, getFallbackClassLoader());
            }
        }
    }

    /**
     * 根据回退类加载器解析给定的类名。
     * <p>默认实现只是重新抛出原始异常，因为没有可用的回退。
     *
     * @param className 要解析的类名
     * @param ex        尝试加载类时引发的原始异常
     * @return 新解析的类（从不 {@code null}）
     */
    protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex) throws IOException, ClassNotFoundException {
        throw ex;
    }

    /**
     * 当未指定ClassLoader并且ObjectInputStream自己的默认类加载器失败时，返回要使用的回退ClassLoader。
     * <p>默认实现只返回 {@code null}，表示没有特定的回退可用。
     */
    protected ClassLoader getFallbackClassLoader() throws IOException {
        return null;
    }
}

