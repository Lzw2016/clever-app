package org.clever.beans;

import org.clever.core.BridgeMethodResolver;
import org.clever.core.GenericTypeResolver;
import org.clever.core.MethodParameter;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * 标准JavaBeans {@link PropertyDescriptor}类的扩展，重写{@code getPropertyType()}，以便根据包含的bean类解析泛型声明的类型变量
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:41 <br/>
 */
final class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {
    private final static Logger logger = LoggerFactory.getLogger(GenericTypeAwarePropertyDescriptor.class);

    private final Class<?> beanClass;
    private final Method readMethod;
    private final Method writeMethod;
    private volatile Set<Method> ambiguousWriteMethods;
    private MethodParameter writeMethodParameter;
    private Class<?> propertyType;
    private final Class<?> propertyEditorClass;

    public GenericTypeAwarePropertyDescriptor(Class<?> beanClass,
                                              String propertyName,
                                              Method readMethod,
                                              Method writeMethod,
                                              Class<?> propertyEditorClass) throws IntrospectionException {
        super(propertyName, null, null);
        this.beanClass = beanClass;
        Method readMethodToUse = (readMethod != null ? BridgeMethodResolver.findBridgedMethod(readMethod) : null);
        Method writeMethodToUse = (writeMethod != null ? BridgeMethodResolver.findBridgedMethod(writeMethod) : null);
        if (writeMethodToUse == null && readMethodToUse != null) {
            // Fallback: Original JavaBeans introspection might not have found matching setter
            // method due to lack of bridge method resolution, in case of the getter using a
            // covariant return type whereas the setter is defined for the concrete property type.
            Method candidate = ClassUtils.getMethodIfAvailable(
                    this.beanClass,
                    "set" + StringUtils.capitalize(getName()),
                    (Class<?>[]) null
            );
            if (candidate != null && candidate.getParameterCount() == 1) {
                writeMethodToUse = candidate;
            }
        }
        this.readMethod = readMethodToUse;
        this.writeMethod = writeMethodToUse;
        if (this.writeMethod != null) {
            if (this.readMethod == null) {
                // Write method not matched against read method: potentially ambiguous through
                // several overloaded variants, in which case an arbitrary winner has been chosen
                // by the JDK's JavaBeans Introspector...
                Set<Method> ambiguousCandidates = new HashSet<>();
                for (Method method : beanClass.getMethods()) {
                    if (method.getName().equals(writeMethodToUse.getName())
                            && !method.equals(writeMethodToUse)
                            && !method.isBridge()
                            && method.getParameterCount() == writeMethodToUse.getParameterCount()) {
                        ambiguousCandidates.add(method);
                    }
                }
                if (!ambiguousCandidates.isEmpty()) {
                    this.ambiguousWriteMethods = ambiguousCandidates;
                }
            }
            this.writeMethodParameter = new MethodParameter(this.writeMethod, 0).withContainingClass(this.beanClass);
        }
        if (this.readMethod != null) {
            this.propertyType = GenericTypeResolver.resolveReturnType(this.readMethod, this.beanClass);
        } else if (this.writeMethodParameter != null) {
            this.propertyType = this.writeMethodParameter.getParameterType();
        }
        this.propertyEditorClass = propertyEditorClass;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    @Override
    public Method getReadMethod() {
        return this.readMethod;
    }

    @Override
    public Method getWriteMethod() {
        return this.writeMethod;
    }

    public Method getWriteMethodForActualAccess() {
        Assert.state(this.writeMethod != null, "No write method available");
        Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
        if (ambiguousCandidates != null) {
            this.ambiguousWriteMethods = null;
            logger.debug("Non-unique JavaBean property '" + getName() +
                    "' being accessed! Ambiguous write methods found next to actually used ["
                    + this.writeMethod + "]: " + ambiguousCandidates
            );
        }
        return this.writeMethod;
    }

    public MethodParameter getWriteMethodParameter() {
        Assert.state(this.writeMethodParameter != null, "No write method available");
        return this.writeMethodParameter;
    }

    @Override
    public Class<?> getPropertyType() {
        return this.propertyType;
    }

    @Override
    public Class<?> getPropertyEditorClass() {
        return this.propertyEditorClass;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GenericTypeAwarePropertyDescriptor)) {
            return false;
        }
        GenericTypeAwarePropertyDescriptor otherPd = (GenericTypeAwarePropertyDescriptor) other;
        return (getBeanClass().equals(otherPd.getBeanClass()) && PropertyDescriptorUtils.equals(this, otherPd));
    }

    @Override
    public int hashCode() {
        int hashCode = getBeanClass().hashCode();
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getReadMethod());
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
        return hashCode;
    }
}
