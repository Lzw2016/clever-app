package org.clever.core.annotation;

import org.clever.util.Assert;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ObjectUtils;
import org.clever.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 支持重复标注多个注解的类，能支持Java的{@link Repeatable @Repeatable}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:53 <br/>
 */
public abstract class RepeatableContainers {
    private final RepeatableContainers parent;

    private RepeatableContainers(RepeatableContainers parent) {
        this.parent = parent;
    }

    /**
     * 在一个可重复注解和容器注解之间建立关系
     *
     * @param container  容器注解类型
     * @param repeatable 可重复的注解类型
     */
    public RepeatableContainers and(Class<? extends Annotation> container, Class<? extends Annotation> repeatable) {
        return new ExplicitRepeatableContainer(this, repeatable, container);
    }

    /**
     * 查找重复的注解
     */
    Annotation[] findRepeatedAnnotations(Annotation annotation) {
        if (this.parent == null) {
            return null;
        }
        return this.parent.findRepeatedAnnotations(annotation);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(this.parent, ((RepeatableContainers) other).parent);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.parent);
    }

    /**
     * 创建一个{@link RepeatableContainers}实例，使用Java的{@link Repeatable @Repeatable}注释进行搜索
     */
    public static RepeatableContainers standardRepeatable() {
        return StandardRepeatableContainers.INSTANCE;
    }

    /**
     * 在一个可重复注解和容器注解之间建立关系
     *
     * @param repeatable 可重复的注解类型
     * @param container  容器注解类型(可为null)，如果未指定就使用{@link Repeatable @Repeatable}注解来判断
     */
    public static RepeatableContainers of(Class<? extends Annotation> repeatable, Class<? extends Annotation> container) {
        return new ExplicitRepeatableContainer(null, repeatable, container);
    }

    /**
     * 查找重复的注解总是返回null值的{@link RepeatableContainers}
     */
    public static RepeatableContainers none() {
        return NoRepeatableContainers.INSTANCE;
    }

    /**
     * 使用Java的{@link Repeatable @Repeatable}注解进行搜索的标准{@link RepeatableContainers}
     */
    private static class StandardRepeatableContainers extends RepeatableContainers {
        private static final Map<Class<? extends Annotation>, Object> cache = new ConcurrentReferenceHashMap<>();
        private static final Object NONE = new Object();
        private static final StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

        StandardRepeatableContainers() {
            super(null);
        }

        @Override
        Annotation[] findRepeatedAnnotations(Annotation annotation) {
            Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
            if (method != null) {
                return (Annotation[]) ReflectionUtils.invokeMethod(method, annotation);
            }
            return super.findRepeatedAnnotations(annotation);
        }

        private static Method getRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
            Object result = cache.computeIfAbsent(annotationType, StandardRepeatableContainers::computeRepeatedAnnotationsMethod);
            return (result != NONE ? (Method) result : null);
        }

        private static Object computeRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
            AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
            if (methods.hasOnlyValueAttribute()) {
                Method method = methods.get(0);
                Class<?> returnType = method.getReturnType();
                if (returnType.isArray()) {
                    Class<?> componentType = returnType.getComponentType();
                    if (Annotation.class.isAssignableFrom(componentType) && componentType.isAnnotationPresent(Repeatable.class)) {
                        return method;
                    }
                }
            }
            return NONE;
        }
    }

    /**
     * 显示映射的{@link RepeatableContainers}
     */
    private static class ExplicitRepeatableContainer extends RepeatableContainers {
        private final Class<? extends Annotation> repeatable;
        private final Class<? extends Annotation> container;
        private final Method valueMethod;

        /**
         * @param parent     父级{@link RepeatableContainers}
         * @param repeatable 可重复的注解
         * @param container  容器注解
         */
        ExplicitRepeatableContainer(RepeatableContainers parent, Class<? extends Annotation> repeatable, Class<? extends Annotation> container) {
            super(parent);
            Assert.notNull(repeatable, "Repeatable must not be null");
            if (container == null) {
                container = deduceContainer(repeatable);
            }
            Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
            try {
                if (valueMethod == null) {
                    throw new NoSuchMethodException("No value method found");
                }
                Class<?> returnType = valueMethod.getReturnType();
                if (!returnType.isArray() || returnType.getComponentType() != repeatable) {
                    throw new AnnotationConfigurationException("Container type [" + container.getName() + "] must declare a 'value' attribute for an array of type [" + repeatable.getName() + "]");
                }
            } catch (AnnotationConfigurationException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new AnnotationConfigurationException("Invalid declaration of container type [" + container.getName() + "] for repeatable annotation [" + repeatable.getName() + "]", ex);
            }
            this.repeatable = repeatable;
            this.container = container;
            this.valueMethod = valueMethod;
        }

        private Class<? extends Annotation> deduceContainer(Class<? extends Annotation> repeatable) {
            Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
            Assert.notNull(annotation, () -> "Annotation type must be a repeatable annotation: " + "failed to resolve container type for " + repeatable.getName());
            return annotation.value();
        }

        @Override
        Annotation[] findRepeatedAnnotations(Annotation annotation) {
            if (this.container.isAssignableFrom(annotation.annotationType())) {
                return (Annotation[]) ReflectionUtils.invokeMethod(this.valueMethod, annotation);
            }
            return super.findRepeatedAnnotations(annotation);
        }

        @Override
        public boolean equals(Object other) {
            if (!super.equals(other)) {
                return false;
            }
            ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
            return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
        }

        @Override
        public int hashCode() {
            int hashCode = super.hashCode();
            hashCode = 31 * hashCode + this.container.hashCode();
            hashCode = 31 * hashCode + this.repeatable.hashCode();
            return hashCode;
        }
    }

    /**
     * 查找重复的注解总是返回null值的{@link RepeatableContainers}
     */
    private static class NoRepeatableContainers extends RepeatableContainers {
        private static final NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

        NoRepeatableContainers() {
            super(null);
        }
    }
}