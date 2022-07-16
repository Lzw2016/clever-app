package org.clever.core.annotation;

import org.clever.core.BridgeMethodResolver;
import org.clever.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.clever.core.annotation.MergedAnnotation.Adapt;
import org.clever.core.annotation.MergedAnnotations.SearchStrategy;
import org.clever.util.CollectionUtils;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ReflectionUtils;
import org.clever.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 用于处理注解、处理元注解、桥接方法（编译器为泛型声明生成）以及父类方法（用于可选注解继承）的通用实用类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:49 <br/>
 */
public abstract class AnnotationUtils {
    /**
     * 具有单个element的注解的属性名称
     */
    public static final String VALUE = MergedAnnotation.VALUE;
    private static final AnnotationFilter JAVA_LANG_ANNOTATION_FILTER = AnnotationFilter.packages("java.lang.annotation");
    private static final Map<Class<? extends Annotation>, Map<String, DefaultValueHolder>> defaultValuesCache = new ConcurrentReferenceHashMap<>();

    /**
     * 确定给定类是否是承载指定注解之一的候选类（在类型、方法或字段级别）
     *
     * @param clazz           class对象
     * @param annotationTypes 可搜索的注解类型
     * @see #isCandidateClass(Class, Class)
     * @see #isCandidateClass(Class, String)
     */
    public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (isCandidateClass(clazz, annotationType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 确定给定类是否是承载指定注解之一的候选类（在类型、方法或字段级别）
     *
     * @param clazz          class对象
     * @param annotationType 可搜索的注解类型
     * @see #isCandidateClass(Class, String)
     */
    public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return isCandidateClass(clazz, annotationType.getName());
    }

    /**
     * 确定给定类是否是承载指定注解之一的候选类（在类型、方法或字段级别）
     *
     * @param clazz          class对象
     * @param annotationName 可搜索注解类型的全名称
     * @see #isCandidateClass(Class, Class)
     */
    public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
        if (annotationName.startsWith("java.")) {
            return true;
        }
        return !AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz);
    }

    /**
     * 从提供的注解中获取{@code annotationType}的单个注解：给定注解本身或其直接元注解<br/>
     * 请注意，此方法仅支持单个级别的元注解。要支持任意级别的元注解，请改用{@code find*()}方法
     *
     * @param annotation     要检查的注解
     * @param annotationType 要查找的注解类型，包括本地注解和元注解
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
        // Shortcut: directly present on the element, with no merging needed?
        if (annotationType.isInstance(annotation)) {
            return synthesizeAnnotation((A) annotation, annotationType);
        }
        // Shortcut: no searchable annotations to be found on plain Java classes and core clever types...
        if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotation)) {
            return null;
        }
        // Exhaustive retrieval of merged annotations...
        return MergedAnnotations.from(
                annotation,
                new Annotation[]{annotation},
                RepeatableContainers.none()
        ).get(annotationType).withNonMergedAttributes().synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
    }

    /**
     * 从提供的{@link AnnotatedElement}获取{@code annotationType}的单个注解，其中注解在{@link AnnotatedElement}上存在或元存在<br/>
     * 请注意，此方法仅支持单个级别的元注解。为了支持任意级别的元注解，请改用{@link #findAnnotation(AnnotatedElement, Class)}
     *
     * @param annotatedElement 从中获取注解的{@code AnnotatedElement}
     * @param annotationType   要查找的注解类型，包括本地注解和元注解
     */
    public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) || AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
            return annotatedElement.getAnnotation(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return MergedAnnotations.from(
                annotatedElement,
                SearchStrategy.INHERITED_ANNOTATIONS,
                RepeatableContainers.none()
        ).get(annotationType).withNonMergedAttributes().synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
    }

    private static <A extends Annotation> boolean isSingleLevelPresent(MergedAnnotation<A> mergedAnnotation) {
        int distance = mergedAnnotation.getDistance();
        return (distance == 0 || distance == 1);
    }

    /**
     * 从提供的方法中获取{@code annotationType}的单个注解，其中注解在方法上存在或元存在<br/>
     * 正确处理编译器生成的桥接方法<br/>
     * 请注意，此方法仅支持单个级别的元注解。为了支持任意级别的元注解，请改用{@link #findAnnotation(Method, Class)}
     *
     * @param method         查找注解的方法
     * @param annotationType 要查找的注解类型
     * @see #getAnnotation(AnnotatedElement, Class)
     */
    public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
        Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
        return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
    }

    /**
     * 在提供的{@link AnnotatedElement}上查找{@code annotationType}的单个注解<br/>
     * 如果注解没有直接出现在所提供的元素上，将搜索元注解<br/>
     * 警告：此方法通常对带注解的元素进行操作。
     * 换句话说，此方法不会对类或方法执行专门的搜索算法。
     * 如果需要{@link #findAnnotation(Class, Class)}或{@link #findAnnotation(Method, Class)}更具体的语义，
     * 请调用其中一个方法
     *
     * @param annotatedElement 要在其上查找注解的{@code AnnotatedElement}
     * @param annotationType   要查找的注解类型，包括本地注解和元注解
     */
    public static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) || AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
            return annotatedElement.getDeclaredAnnotation(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return MergedAnnotations.from(
                annotatedElement,
                SearchStrategy.INHERITED_ANNOTATIONS,
                RepeatableContainers.none()
        ).get(annotationType).withNonMergedAttributes().synthesize(MergedAnnotation::isPresent).orElse(null);
    }

    /**
     * 在提供的方法上查找{@code annotationType}的单个注解，如果注解没有直接出现在给定方法本身上，则遍历其超级方法(即从超类和接口)<br/>
     * 正确处理编译器生成的桥接方法。如果注解没有直接出现在方法上，将搜索元注解。<br/>
     * 默认情况下，方法上的注解不会被继承，因此我们需要显式地处理这个问题
     *
     * @param method         查找注解的方法
     * @param annotationType 要查找的注解类型
     * @see #getAnnotation(Method, Class)
     */
    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) || AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
            return method.getDeclaredAnnotation(annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return MergedAnnotations.from(
                method,
                SearchStrategy.TYPE_HIERARCHY,
                RepeatableContainers.none()
        ).get(annotationType).withNonMergedAttributes().synthesize(MergedAnnotation::isPresent).orElse(null);
    }

    /**
     * 在提供的类上查找{@code annotationType}的单个注解，如果注解没有直接出现在给定的类本身上，则遍历其接口、注解和超类<br/>
     * 此方法显式处理未声明为继承的类级注解以及元注解和接口上的注解，该算法的操作如下：<br/>
     * 1.搜索给定类上的注解，如果找到，则返回它<br/>
     * 2.递归地搜索给定类声明的所有注解<br/>
     * 3.递归地搜索给定类声明的所有接口<br/>
     * 4.递归搜索给定类的超类层次结构<br/>
     * 注意：在此上下文中，该术语递归地表示搜索过程将继续，返回步骤#1，将当前接口、注解或超类作为查找注解的类
     *
     * @param clazz          要在其上查找注解的类
     * @param annotationType 要查找的注解类型
     */
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        // Shortcut: directly present on the element, with no merging needed?
        if (AnnotationFilter.PLAIN.matches(annotationType) || AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
            A annotation = clazz.getDeclaredAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            // For backwards compatibility, perform a superclass search with plain annotations
            // even if not marked as @Inherited: e.g. a findAnnotation search for @Deprecated
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null || superclass == Object.class) {
                return null;
            }
            return findAnnotation(superclass, annotationType);
        }
        // Exhaustive retrieval of merged annotations...
        return MergedAnnotations.from(
                clazz,
                SearchStrategy.TYPE_HIERARCHY,
                RepeatableContainers.none()
        ).get(annotationType).withNonMergedAttributes().synthesize(MergedAnnotation::isPresent).orElse(null);
    }

    /**
     * 确定指定{@code annotationType}的注解是否在提供的clazz上本地声明(即直接存在)<br/>
     * 提供的类可以表示任何类型。不会搜索元注解<br/>
     * 注意：此方法不确定注解是否继承{@linkplain java.lang.annotation.Inherited inherited}
     *
     * @param annotationType 要查找的注解类型
     * @param clazz          要在其上检查注解的类
     * @see java.lang.Class#getDeclaredAnnotations()
     * @see java.lang.Class#getDeclaredAnnotation(Class)
     */
    public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
        return MergedAnnotations.from(clazz).get(annotationType).isDirectlyPresent();
    }

    /**
     * 确定提供的注解是否在JDK {@code java.lang.annotation}包中定义
     *
     * @param annotation 要检查的注解
     */
    public static boolean isInJavaLangAnnotationPackage(Annotation annotation) {
        return (annotation != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotation));
    }

    /**
     * 确定所提供名称的注解是否在JDK {@code java.lang.annotation}包中定义
     *
     * @param annotationType 要检查的注解类型的名称
     */
    public static boolean isInJavaLangAnnotationPackage(String annotationType) {
        return (annotationType != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotationType));
    }

    /**
     * 检查给定注解的声明属性，特别是针对类值的Google App Engine延迟到达{@code TypeNotPresentExceptionProxy} {@code Class}，<br/>
     * 而不是早期的{@code Class.getAnnotations()}失败<br/>
     * 此方法未失败表示{@link #getAnnotationAttributes(Annotation)}也不会失败(稍后尝试时)
     *
     * @param annotation 要验证的注解
     * @throws IllegalStateException 如果无法读取声明的类属性
     * @see Class#getAnnotations()
     * @see #getAnnotationAttributes(Annotation)
     */
    public static void validateAnnotation(Annotation annotation) {
        AttributeMethods.forAnnotationType(annotation.annotationType()).validate(annotation);
    }

    /**
     * 检索给定注解的属性，保留所有属性类型。相当于调用{@link #getAnnotationAttributes(Annotation, boolean, boolean)}，<br/>
     * {@code classValuesAsString}和{@code nestedAnnotationsAsMap}参数设置为false<br/>
     * 注意：此方法实际上返回{@link AnnotationAttributes}实例<br/>
     * 但是，为了实现二进制兼容性，已保留映射签名
     *
     * @param annotation 要检索其属性的注解
     * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
     * @see #getAnnotationAttributes(Annotation, boolean, boolean)
     * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
     */
    public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
        return getAnnotationAttributes(null, annotation);
    }

    /**
     * 检索给定注解的属性。等效于调用{@link #getAnnotationAttributes(Annotation, boolean, boolean)}，<br/>
     * 并将{@code nestedAnnotationsAsMap}参数设置为false<br/>
     * 注意：此方法实际上返回{@link AnnotationAttributes}实例<br/>
     * 但是，为了实现二进制兼容性，已保留映射签名
     *
     * @param annotation          要检索其属性的注解
     * @param classValuesAsString 是将类引用转换为字符串(为了与AnnotationMetadata兼容)，还是将它们保留为类引用
     * @see #getAnnotationAttributes(Annotation, boolean, boolean)
     */
    public static Map<String, Object> getAnnotationAttributes(Annotation annotation, boolean classValuesAsString) {
        return getAnnotationAttributes(annotation, classValuesAsString, false);
    }

    /**
     * 以{@link AnnotationAttributes}映射的形式检索给定注解的属性。<br/>
     * 这种方法提供了完全递归的注解读取功能，与基于反射的StandardAnnotationMetadata
     *
     * @param annotation             要检索其属性的注解
     * @param classValuesAsString    是将类引用转换为字符串(为了与AnnotationMetadata兼容)，还是将它们保留为类引用
     * @param nestedAnnotationsAsMap 是将嵌套注解转换为{@link AnnotationAttributes}映射(为了与AnnotationMetadata兼容)，还是将其保留为注解实例
     */
    public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
        return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
    }

    /**
     * 以{@link AnnotationAttributes}映射的形式检索给定注解的属性。<br/>
     * 相当于调用{@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)}，<br/>
     * {@code classValuesAsString}和{@code nestedAnnotationsAsMap}参数设置为false
     *
     * @param annotatedElement 使用提供的{@code AnnotatedElement}；如果未知，则可能为 null
     * @param annotation       要检索其属性的注解
     * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
     */
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement, Annotation annotation) {
        return getAnnotationAttributes(annotatedElement, annotation, false, false);
    }

    /**
     * 以{@link AnnotationAttributes}映射的形式检索给定注解的属性。<br/>
     * 这种方法提供了完全递归的注解读取功能，与基于反射的StandardAnnotationMetadata
     *
     * @param annotatedElement       使用提供的{@code AnnotatedElement}；如果未知，则可能为 null
     * @param annotation             要检索其属性的注解
     * @param classValuesAsString    是将类引用转换为字符串(为了与AnnotationMetadata兼容)，还是将它们保留为类引用
     * @param nestedAnnotationsAsMap 是将嵌套注解转换为{@link AnnotationAttributes}映射(为了与AnnotationMetadata兼容)，还是将其保留为注解实例
     */
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
                                                               Annotation annotation,
                                                               boolean classValuesAsString,
                                                               boolean nestedAnnotationsAsMap) {
        Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
        return MergedAnnotation.from(annotatedElement, annotation)
                .withNonMergedAttributes()
                .asMap(mergedAnnotation -> new AnnotationAttributes(mergedAnnotation.getType(), true), adaptations);
    }

    /**
     * 为给定属性注册注解声明的默认值(如果可用)
     *
     * @param attributes 要处理的注解属性
     */
    public static void registerDefaultValues(AnnotationAttributes attributes) {
        Class<? extends Annotation> annotationType = attributes.annotationType();
        if (annotationType != null && Modifier.isPublic(annotationType.getModifiers()) && !AnnotationFilter.PLAIN.matches(annotationType)) {
            Map<String, DefaultValueHolder> defaultValues = getDefaultValues(annotationType);
            defaultValues.forEach(attributes::putIfAbsent);
        }
    }

    private static Map<String, DefaultValueHolder> getDefaultValues(Class<? extends Annotation> annotationType) {
        return defaultValuesCache.computeIfAbsent(annotationType, AnnotationUtils::computeDefaultValues);
    }

    private static Map<String, DefaultValueHolder> computeDefaultValues(Class<? extends Annotation> annotationType) {
        AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
        if (!methods.hasDefaultValueMethod()) {
            return Collections.emptyMap();
        }
        Map<String, DefaultValueHolder> result = CollectionUtils.newLinkedHashMap(methods.size());
        if (!methods.hasNestedAnnotation()) {
            // Use simpler method if there are no nested annotations
            for (int i = 0; i < methods.size(); i++) {
                Method method = methods.get(i);
                Object defaultValue = method.getDefaultValue();
                if (defaultValue != null) {
                    result.put(method.getName(), new DefaultValueHolder(defaultValue));
                }
            }
        } else {
            // If we have nested annotations, we need them as nested maps
            AnnotationAttributes attributes = MergedAnnotation.of(annotationType)
                    .asMap(annotation -> new AnnotationAttributes(annotation.getType(), true), Adapt.ANNOTATION_TO_MAP);
            for (Map.Entry<String, Object> element : attributes.entrySet()) {
                result.put(element.getKey(), new DefaultValueHolder(element.getValue()));
            }
        }
        return result;
    }

    /**
     * 对提供的{@link AnnotationAttributes}进行后期处理，将嵌套注解保留为注解实例。<br/>
     * 具体而言，此方法为使用{@link AliasFor @AliasFor}注解的注解属性强制属性别名语义，并用其原始默认值替换默认值占位符
     *
     * @param annotatedElement    使用提供的{@code AnnotatedElement}；如果未知，则可能为 null
     * @param attributes          要后期处理的注解属性
     * @param classValuesAsString 是将类引用转换为字符串(为了与AnnotationMetadata兼容)，还是将它们保留为类引用
     * @see #getDefaultValue(Class, String)
     */
    public static void postProcessAnnotationAttributes(Object annotatedElement, AnnotationAttributes attributes, boolean classValuesAsString) {
        if (attributes == null) {
            return;
        }
        if (!attributes.validated) {
            Class<? extends Annotation> annotationType = attributes.annotationType();
            if (annotationType == null) {
                return;
            }
            AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
            for (int i = 0; i < mapping.getMirrorSets().size(); i++) {
                MirrorSet mirrorSet = mapping.getMirrorSets().get(i);
                int resolved = mirrorSet.resolve(attributes.displayName, attributes, AnnotationUtils::getAttributeValueForMirrorResolution);
                if (resolved != -1) {
                    Method attribute = mapping.getAttributes().get(resolved);
                    Object value = attributes.get(attribute.getName());
                    for (int j = 0; j < mirrorSet.size(); j++) {
                        Method mirror = mirrorSet.get(j);
                        if (mirror != attribute) {
                            attributes.put(mirror.getName(), adaptValue(annotatedElement, value, classValuesAsString));
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
            String attributeName = attributeEntry.getKey();
            Object value = attributeEntry.getValue();
            if (value instanceof DefaultValueHolder) {
                value = ((DefaultValueHolder) value).defaultValue;
                attributes.put(attributeName, adaptValue(annotatedElement, value, classValuesAsString));
            }
        }
    }

    private static Object getAttributeValueForMirrorResolution(Method attribute, Object attributes) {
        Object result = ((AnnotationAttributes) attributes).get(attribute.getName());
        return (result instanceof DefaultValueHolder ? ((DefaultValueHolder) result).defaultValue : result);
    }

    private static Object adaptValue(Object annotatedElement, Object value, boolean classValuesAsString) {
        if (classValuesAsString) {
            if (value instanceof Class) {
                return ((Class<?>) value).getName();
            }
            if (value instanceof Class[]) {
                Class<?>[] classes = (Class<?>[]) value;
                String[] names = new String[classes.length];
                for (int i = 0; i < classes.length; i++) {
                    names[i] = classes[i].getName();
                }
                return names;
            }
        }
        if (value instanceof Annotation) {
            Annotation annotation = (Annotation) value;
            return MergedAnnotation.from(annotatedElement, annotation).synthesize();
        }
        if (value instanceof Annotation[]) {
            Annotation[] annotations = (Annotation[]) value;
            Annotation[] synthesized = (Annotation[]) Array.newInstance(annotations.getClass().getComponentType(), annotations.length);
            for (int i = 0; i < annotations.length; i++) {
                synthesized[i] = MergedAnnotation.from(annotatedElement, annotations[i]).synthesize();
            }
            return synthesized;
        }
        return value;
    }

    /**
     * 给定注解实例，检索单个元素注解的value属性的值
     *
     * @param annotation 要从中检索值的注解实例
     * @see #getValue(Annotation, String)
     */
    public static Object getValue(Annotation annotation) {
        return getValue(annotation, VALUE);
    }

    /**
     * 获取注解的属性值
     *
     * @see #getValue(Annotation)
     */
    public static Object getValue(Annotation annotation, String attributeName) {
        if (annotation == null || !StringUtils.hasText(attributeName)) {
            return null;
        }
        try {
            Method method = annotation.annotationType().getDeclaredMethod(attributeName);
            ReflectionUtils.makeAccessible(method);
            return method.invoke(annotation);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (InvocationTargetException ex) {
            rethrowAnnotationConfigurationException(ex.getTargetException());
            throw new IllegalStateException("Could not obtain value for annotation attribute '" + attributeName + "' in " + annotation, ex);
        } catch (Throwable ex) {
            handleIntrospectionFailure(annotation.getClass(), ex);
            return null;
        }
    }

    /**
     * 如果提供的throwable是{@link AnnotationConfigurationException}，
     * 则它将被转换为{@code AnnotationConfigurationException}并抛出，
     * 从而允许它传播到调用方。否则，此方法不会执行任何操作
     */
    static void rethrowAnnotationConfigurationException(Throwable ex) {
        if (ex instanceof AnnotationConfigurationException) {
            throw (AnnotationConfigurationException) ex;
        }
    }

    /**
     * 处理提供的注解内省异常。
     * 如果提供的异常是{@link AnnotationConfigurationException}，则只会抛出它，允许它传播到调用方，而不会记录任何内容。
     * 否则，此方法会在继续之前记录一个内省失败(尤其是对于{@link TypeNotPresentException})，假设嵌套类值在注解属性中无法解析，从而有效地假装指定元素上没有注解
     *
     * @param element 我们试图对注解进行内省的元素
     * @param ex      遇到的异常
     * @see #rethrowAnnotationConfigurationException
     * @see IntrospectionFailureLogger
     */
    static void handleIntrospectionFailure(AnnotatedElement element, Throwable ex) {
        rethrowAnnotationConfigurationException(ex);
        IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
        boolean meta = false;
        if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
            // Meta-annotation or (default) value lookup on an annotation type
            logger = IntrospectionFailureLogger.DEBUG;
            meta = true;
        }
        if (logger.isEnabled()) {
            String message = meta ? "Failed to meta-introspect annotation " : "Failed to introspect annotations on ";
            logger.log(message + element + ": " + ex);
        }
    }

    /**
     * 获取注解value属性的默认值
     *
     * @param annotation 注解实例
     * @see #getDefaultValue(Annotation, String)
     */
    public static Object getDefaultValue(Annotation annotation) {
        return getDefaultValue(annotation, VALUE);
    }

    /**
     * 获取注解属性默认值
     *
     * @see #getDefaultValue(Class, String)
     */
    public static Object getDefaultValue(Annotation annotation, String attributeName) {
        return (annotation != null ? getDefaultValue(annotation.annotationType(), attributeName) : null);
    }

    /**
     * 获取注解value属性的默认值
     *
     * @see #getDefaultValue(Class, String)
     */
    public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
        return getDefaultValue(annotationType, VALUE);
    }

    /**
     * 获取注解属性的默认值
     *
     * @see #getDefaultValue(Annotation, String)
     */
    public static Object getDefaultValue(Class<? extends Annotation> annotationType, String attributeName) {
        if (annotationType == null || !StringUtils.hasText(attributeName)) {
            return null;
        }
        return MergedAnnotation.of(annotationType).getDefaultValue(attributeName).orElse(null);
    }

    /**
     * 通过将所提供的注解包装在动态代理中来合成注解，动态代理透明地强制使用{@link AliasFor @AliasFor}注解的注解属性的属性别名语义
     *
     * @param annotation       合成的注解
     * @param annotatedElement 使用提供的{@code AnnotatedElement}；如果未知，则可能为 null
     * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
     * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
     * @see #synthesizeAnnotation(Class)
     */
    public static <A extends Annotation> A synthesizeAnnotation(A annotation, AnnotatedElement annotatedElement) {
        if (annotation instanceof SynthesizedAnnotation || AnnotationFilter.PLAIN.matches(annotation)) {
            return annotation;
        }
        return MergedAnnotation.from(annotatedElement, annotation).synthesize();
    }

    /**
     * 根据注解的默认属性值合成注解。
     * 此方法仅委托{@link #synthesizeAnnotation(Map, Class, AnnotatedElement)}，为源属性值提供空映射，为{@link AnnotatedElement}提供空映射
     *
     * @param annotationType 要合成的注解类型
     * @return 合成注解
     * @throws IllegalArgumentException         如果缺少必需的属性
     * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
     * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
     * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
     */
    public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
        return synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
    }

    /**
     * 通过将Map包装在动态代理中，从提供的注解属性映射中合成注解，
     * 动态代理实现指定{@code annotationType}的注解，
     * 并透明地强制使用{@link AliasFor @AliasFor}注解的注解属性的属性别名语义。
     * 所提供的映射必须包含在所提供的{@code annotationType}中定义的每个属性的键值对，该属性没有别名或没有默认值。
     * 嵌套映射和嵌套映射数组将分别递归合成为嵌套注解或嵌套注解数组。
     * 请注意{@link AnnotationAttributes}是一种特殊类型的映射，是此方法的{@code attributes}参数的理想候选对象。
     *
     * @param attributes       要合成的注解属性映射
     * @param annotationType   要合成的注解类型
     * @param annotatedElement 使用提供的{@code AnnotatedElement}；如果未知，则可能为 null
     * @return 合成注解
     * @throws IllegalArgumentException         如果缺少必需的属性或属性的类型不正确
     * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
     * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
     * @see #synthesizeAnnotation(Class)
     * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
     * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
     */
    public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes, Class<A> annotationType, AnnotatedElement annotatedElement) {
        try {
            return MergedAnnotation.of(annotatedElement, annotationType, attributes).synthesize();
        } catch (NoSuchElementException | IllegalStateException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * 通过创建相同大小和类型的新数组，并使用输入数组中的注解的合成版本填充该数组，从提供的注解数组中合成注解数组
     *
     * @param annotations      要合成的注解数组
     * @param annotatedElement 使用提供的{@code AnnotatedElement}；如果未知，则可能为 null
     * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
     * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
     * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
     */
    static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, AnnotatedElement annotatedElement) {
        if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
            return annotations;
        }
        Annotation[] synthesized = (Annotation[]) Array.newInstance(annotations.getClass().getComponentType(), annotations.length);
        for (int i = 0; i < annotations.length; i++) {
            synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
        }
        return synthesized;
    }

    /**
     * 清除内部注解元数据缓存
     */
    public static void clearCache() {
        AnnotationTypeMappings.clearCache();
        AnnotationsScanner.clearCache();
    }

    /**
     * 用于包装默认值的内部类
     */
    private static class DefaultValueHolder {
        final Object defaultValue;

        public DefaultValueHolder(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String toString() {
            return "*" + this.defaultValue;
        }
    }
}
