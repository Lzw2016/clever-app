package org.clever.core.convert;

import org.clever.core.MethodParameter;
import org.clever.core.ResolvableType;
import org.clever.core.annotation.AnnotatedElementUtils;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * java类型描述对象，能够表示数组和泛型集合类型
 * 作者：lizw <br/>
 * 创建时间：2022/04/17 23:30 <br/>
 */
public class TypeDescriptor implements Serializable {
    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];
    private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);
    private static final Class<?>[] CACHED_COMMON_TYPES = {
            boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
            double.class, Double.class, float.class, Float.class, int.class, Integer.class,
            long.class, Long.class, short.class, Short.class, String.class, Object.class};

    static {
        for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
            commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
        }
    }

    private final Class<?> type;
    private final ResolvableType resolvableType;
    private final AnnotatedElementAdapter annotatedElement;

    /**
     * 从{@link MethodParameter}创建新的类型描述符。
     * 当源或目标转换点是构造函数参数、方法参数或方法返回值时，请使用此构造函数
     *
     * @param methodParameter 方法参数
     */
    public TypeDescriptor(MethodParameter methodParameter) {
        this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
        this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
        this.annotatedElement = new AnnotatedElementAdapter(
                methodParameter.getParameterIndex() == -1 ?
                        methodParameter.getMethodAnnotations() :
                        methodParameter.getParameterAnnotations()
        );
    }

    /**
     * 从字段({@link Field})创建新的类型描述符。
     * 当源或目标转换点是字段时，请使用此构造函数
     *
     * @param field 字段
     */
    public TypeDescriptor(Field field) {
        this.resolvableType = ResolvableType.forField(field);
        this.type = this.resolvableType.resolve(field.getType());
        this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
    }

    /**
     * 从属性({@link Property})创建新的类型描述符。
     * 当源或目标转换点是Java类上的属性时，请使用此构造函数
     *
     * @param property 属性
     */
    public TypeDescriptor(Property property) {
        Assert.notNull(property, "Property must not be null");
        this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
        this.type = this.resolvableType.resolve(property.getType());
        this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
    }

    /**
     * 从{@link ResolvableType}创建新的类型描述符。
     * 此构造函数在内部使用，也可由支持非Java语言和扩展类型系统的子类使用
     *
     * @param resolvableType 可解析类型
     * @param type           支持类型(如果应该解析，则为{@code null})
     * @param annotations    类型注解
     */
    public TypeDescriptor(ResolvableType resolvableType, Class<?> type, Annotation[] annotations) {
        this.resolvableType = resolvableType;
        this.type = (type != null ? type : resolvableType.toClass());
        this.annotatedElement = new AnnotatedElementAdapter(annotations);
    }

    /**
     * {@link #getType()}的变体，通过返回其对象包装器类型来说明基元类型。
     * 这对于希望规范化为基于对象的类型而不直接使用基元类型的转换服务实现非常有用
     */
    public Class<?> getObjectType() {
        return ClassUtils.resolvePrimitiveIfNecessary(getType());
    }

    /**
     * 此TypeDescriptor描述的支持类、方法参数、字段或属性的类型。
     * 按原样返回基元类型。有关此操作的变体，请参见{@link #getObjectType()}，该操作可在必要时将基元类型解析为相应的对象类型
     *
     * @see #getObjectType()
     */
    public Class<?> getType() {
        return this.type;
    }

    public ResolvableType getResolvableType() {
        return this.resolvableType;
    }

    /**
     * 返回描述符的基础源。
     * 将根据{@link TypeDescriptor}的构造方式返回{@link Field}、{@link MethodParameter}或{@link Type}。
     * 此方法主要是提供对其他JVM语言可能提供的其他类型信息或元数据的访问
     */
    public Object getSource() {
        return this.resolvableType.getSource();
    }

    /**
     * 通过将其类型设置为所提供值的类来缩小此{@link TypeDescriptor}的范围。
     * 如果该值为null，则不会执行缩小操作，并且返回的TypeDescriptor将保持不变。
     * 设计用于绑定框架在读取属性、字段或方法返回值时调用。
     * 允许此类框架缩小从声明的属性、字段或方法返回值类型构建的TypeDescriptor。
     * 例如，声明为java的字段。对象将缩小到{@code java.util.HashMap}值。
     * 然后可以使用缩小的TypeDescriptor将HashMap转换为其他类型。狭窄的副本保留注解和嵌套类型上下文。
     *
     * @param value 用于缩小此类型描述符的值
     */
    public TypeDescriptor narrow(Object value) {
        if (value == null) {
            return this;
        }
        ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
        return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
    }

    /**
     * 将此{@link TypeDescriptor}强制转换为保留注解和嵌套类型上下文的超类或实现的接口
     *
     * @param superType 父类类型(可以为null)
     * @throws IllegalArgumentException 如果此类型不能分配给超父类类型
     */
    public TypeDescriptor upcast(Class<?> superType) {
        if (superType == null) {
            return null;
        }
        Assert.isAssignable(superType, getType());
        return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
    }

    /**
     * 返回此类型的名称：完全限定的类名
     */
    public String getName() {
        return ClassUtils.getQualifiedName(getType());
    }

    /**
     * 此类型是基元类型吗？
     */
    public boolean isPrimitive() {
        return getType().isPrimitive();
    }

    /**
     * 返回与此类型描述符关联的注解（如果有）
     */
    public Annotation[] getAnnotations() {
        return this.annotatedElement.getAnnotations();
    }

    /**
     * 确定此类型描述符是否具有指定的注解，此方法支持任意级别的元注解
     *
     * @param annotationType 注解类型
     */
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        if (this.annotatedElement.isEmpty()) {
            // Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
            // to return a copy of the array, whereas we can do it more efficiently here.
            return false;
        }
        return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
    }

    /**
     * 获取此类型描述符上指定{@code annotationType}的注解，此方法支持任意级别的元注解
     *
     * @param annotationType 注解类型
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        if (this.annotatedElement.isEmpty()) {
            // Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
            // to return a copy of the array, whereas we can do it more efficiently here.
            return null;
        }
        return AnnotatedElementUtils.getMergedAnnotation(this.annotatedElement, annotationType);
    }

    /**
     * 如果此类型描述符的对象可以分配到给定类型描述符描述的位置，则返回true。
     * 例如，{@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}返回true，因为可以将字符串值分配给CharSequence变量。
     * 另一方面，{@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}返回false，因为虽然所有整数都是数字，但并非所有数字都是整数。
     * <pre>{@code
     * 对于Array、collections和maps，如果已声明，则检查元素和key/value类型。
     * 例如，List<字符串>字段值可分配给Collection<CharSequence>字段，但List<Number>不可分配给List<Integer>
     * }</pre>
     *
     * @see #getObjectType()
     */
    public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
        boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
        if (!typesAssignable) {
            return false;
        }
        if (isArray() && typeDescriptor.isArray()) {
            return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
        } else if (isCollection() && typeDescriptor.isCollection()) {
            return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
        } else if (isMap() && typeDescriptor.isMap()) {
            return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor())
                    && isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
        } else {
            return true;
        }
    }

    private boolean isNestedAssignable(TypeDescriptor nestedTypeDescriptor, TypeDescriptor otherNestedTypeDescriptor) {
        return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null || nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
    }

    /**
     * 此类型是{@link Collection}类型吗?
     */
    public boolean isCollection() {
        return Collection.class.isAssignableFrom(getType());
    }

    /**
     * 此类型是数组类型吗?
     */
    public boolean isArray() {
        return getType().isArray();
    }

    /**
     * 如果此类型是array，则返回数组的组件类型。
     * 如果此类型是{@code Stream}，则返回流的组件类型。
     * 如果此类型是{@link Collection}并且已参数化，则返回集合的元素类型。如果集合未参数化，则返回null，表示未声明元素类型
     *
     * @see #elementTypeDescriptor(Object)
     */
    public TypeDescriptor getElementTypeDescriptor() {
        if (getResolvableType().isArray()) {
            return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
        }
        if (Stream.class.isAssignableFrom(getType())) {
            return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
        }
        return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
    }

    /**
     * 如果此类型是{@link Collection}或array，则从提供的集合或数组元素创建元素TypeDescriptor。
     * 将elementType属性缩小到所提供集合或数组元素的类。
     * 例如，如果这描述了一个{@code java.util.List<java.lang.Number>}，返回的TypeDescriptor将是{@code java.lang.Integer}。
     * 如果这描述了{@code java.util.List<?>}元素参数是{@code java.lang.Integer}，返回的TypeDescriptor将是{@code java.lang.Integer}。
     * 注解和嵌套类型上下文将保留在返回的狭窄TypeDescriptor中
     *
     * @param element 集合或数组元素
     * @see #getElementTypeDescriptor()
     * @see #narrow(Object)
     */
    public TypeDescriptor elementTypeDescriptor(Object element) {
        return narrow(element, getElementTypeDescriptor());
    }

    /**
     * 这种类型是{@link Map}类型吗？
     */
    public boolean isMap() {
        return Map.class.isAssignableFrom(getType());
    }

    /**
     * 如果此类型是{@link Map}且其key类型已参数化，则返回{@link Map}的key类型。
     * 如果{@link Map}的key类型未参数化，则返回null，表示未声明key类型
     *
     * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
     */
    public TypeDescriptor getMapKeyTypeDescriptor() {
        Assert.state(isMap(), "Not a [java.util.Map]");
        return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
    }

    /**
     * 如果此类型是{@link Map}，则从提供的{@link Map} key创建{@link Map} key TypeDescriptor。
     * 将mapKeyType属性缩小到提供的映射键的类。
     * 例如，如果这描述了一个{@code java.util.Map<java.lang.Number, java.lang.String>} key参数是{@code java.lang.Integer}，返回的TypeDescriptor将是{@code java.lang.Integer}。
     * 如果这描述了{@code java.util.Map<?, ?>} key参数是{@code java.lang.Integer}，返回的TypeDescriptor将是{@code java.lang.Integer}。
     * 注解和嵌套类型上下文将保留在返回的狭窄TypeDescriptor中
     *
     * @param mapKey map key
     * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
     * @see #narrow(Object)
     */
    public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
        return narrow(mapKey, getMapKeyTypeDescriptor());
    }

    /**
     * 如果此类型是{@link Map}且其值类型已参数化，则返回{@link Map}的值类型。
     * 如果{@link Map}的值类型未参数化，则返回null，表示未声明值类型
     *
     * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
     */
    public TypeDescriptor getMapValueTypeDescriptor() {
        Assert.state(isMap(), "Not a [java.util.Map]");
        return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
    }

    /**
     * 如果此类型是{@link Map}，则从提供的映射值创建mapValue {@link TypeDescriptor}。
     * 将mapValueType属性缩小到提供的{@link Map}值的类。
     * 例如，如果这描述了一个{@code java.util.Map<java.lang.String, java.lang.Number>}和value参数是{@code java.lang.Integer}，返回的TypeDescriptor将是{@code java.lang.Integer}。
     * 如果这描述了{@code java.util.Map<?, ?>} 值参数是{@code java.lang.Integer}，返回的TypeDescriptor将是{@code java.lang.Integer}。
     * 注解和嵌套类型上下文将保留在返回的狭窄TypeDescriptor中
     *
     * @param mapValue map value
     * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
     * @see #narrow(Object)
     */
    public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
        return narrow(mapValue, getMapValueTypeDescriptor());
    }

    private TypeDescriptor narrow(Object value, TypeDescriptor typeDescriptor) {
        if (typeDescriptor != null) {
            return typeDescriptor.narrow(value);
        }
        if (value != null) {
            return narrow(value);
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TypeDescriptor)) {
            return false;
        }
        TypeDescriptor otherDesc = (TypeDescriptor) other;
        if (getType() != otherDesc.getType()) {
            return false;
        }
        if (!annotationsMatch(otherDesc)) {
            return false;
        }
        if (isCollection() || isArray()) {
            return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), otherDesc.getElementTypeDescriptor());
        } else if (isMap()) {
            return ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), otherDesc.getMapKeyTypeDescriptor())
                    && ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), otherDesc.getMapValueTypeDescriptor());
        } else {
            return true;
        }
    }

    private boolean annotationsMatch(TypeDescriptor otherDesc) {
        Annotation[] anns = getAnnotations();
        Annotation[] otherAnns = otherDesc.getAnnotations();
        if (anns == otherAnns) {
            return true;
        }
        if (anns.length != otherAnns.length) {
            return false;
        }
        if (anns.length > 0) {
            for (int i = 0; i < anns.length; i++) {
                if (!annotationEquals(anns[i], otherAnns[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
        // Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
        return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
    }

    @Override
    public int hashCode() {
        return getType().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Annotation ann : getAnnotations()) {
            builder.append('@').append(ann.annotationType().getName()).append(' ');
        }
        builder.append(getResolvableType());
        return builder.toString();
    }

    /**
     * 为对象创建新的类型描述符。
     * 在要求转换系统将源对象转换为其他类型之前，请使用此工厂方法内省源对象。
     * 如果提供的对象为null，则返回null，否则调用{@link #valueOf(Class)}从对象的类构建TypeDescriptor
     *
     * @param source 源对象
     * @return 类型描述符
     */
    public static TypeDescriptor forObject(Object source) {
        return (source != null ? valueOf(source.getClass()) : null);
    }

    /**
     * 从给定类型创建新的类型描述符。
     * 当没有类型位置（如方法参数或字段）可用于提供额外的转换上下文时，使用此命令可指示转换系统将对象转换为特定的目标类型。
     * 通常更喜欢使用{@link #forObject(Object)}从源对象构造类型描述符，因为它处理空对象的情况
     *
     * @param type 类型(可以为null以指示{@code Object.class})
     * @return 对应的类型描述符
     */
    public static TypeDescriptor valueOf(Class<?> type) {
        if (type == null) {
            type = Object.class;
        }
        TypeDescriptor desc = commonTypesCache.get(type);
        return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
    }

    /**
     * 从集合类型({@link java.util.Collection})创建新的类型描述符。
     * 用于转换为类型化集合。
     * 例如，可以通过转换为使用此方法生成的targetType将列表转换为列表。
     * 构造这样一个TypeDescriptor的方法调用如下所示：{@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
     *
     * @param collectionType        集合类型，必须实现 {@link Collection}.
     * @param elementTypeDescriptor 集合元素类型的描述符，用于转换集合元素
     * @return 集合类型描述符
     */
    public static TypeDescriptor collection(Class<?> collectionType, TypeDescriptor elementTypeDescriptor) {
        Assert.notNull(collectionType, "Collection type must not be null");
        if (!Collection.class.isAssignableFrom(collectionType)) {
            throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
        }
        ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
        return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
    }

    /**
     * 从{@link java.util.Map}类型创建新的类型描述符。
     * 用于转换为类型化映射。
     * 例如，{@code Map<String, String>}可以通过转换为使用此方法生成的targetType来转换为{@code Map<Id，EmailAddress>}：构造此类TypeDescriptor的方法调用如下所示
     * <pre class="code">
     * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
     * </pre>
     *
     * @param mapType             映射类型，必须实现{@link Map}
     * @param keyTypeDescriptor   映射键类型的描述符，用于转换映射键
     * @param valueTypeDescriptor 地图的值类型，用于转换地图值
     * @return 映射类型描述符
     */
    public static TypeDescriptor map(Class<?> mapType, TypeDescriptor keyTypeDescriptor, TypeDescriptor valueTypeDescriptor) {
        Assert.notNull(mapType, "Map type must not be null");
        if (!Map.class.isAssignableFrom(mapType)) {
            throw new IllegalArgumentException("Map type must be a [java.util.Map]");
        }
        ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
        ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
        return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
    }

    /**
     * 创建一个新的类型描述符作为指定类型的数组。
     * 例如，要创建{@code Map<String,String>[]}，请使用
     * <pre class="code">
     * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
     * </pre>
     *
     * @param elementTypeDescriptor 数组元素的TypeDescriptor或null
     * @return 数组TypeDescriptor，如果elementTypeDescriptor为null，则为null
     */
    public static TypeDescriptor array(TypeDescriptor elementTypeDescriptor) {
        if (elementTypeDescriptor == null) {
            return null;
        }
        return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType), null, elementTypeDescriptor.getAnnotations());
    }

    /**
     * 为方法参数中声明的嵌套类型创建类型描述符
     *
     * @param methodParameter 嵌套级别为1的方法参数
     * @param nestingLevel    方法参数中collection/array元素或map key/value声明的嵌套级别
     * @return 指定嵌套级别的嵌套类型描述符，如果无法获取，则为null
     * @throws IllegalArgumentException 如果输入MethodParameter参数的嵌套级别不是1，或者如果达到指定嵌套级别的类型不是集合、数组或映射类型
     */
    public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
        if (methodParameter.getNestingLevel() != 1) {
            throw new IllegalArgumentException(
                    "MethodParameter nesting level must be 1: " +
                            "use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal"
            );
        }
        return nested(new TypeDescriptor(methodParameter), nestingLevel);
    }

    /**
     * 为字段中声明的嵌套类型创建类型描述符
     *
     * @param field        字段
     * @param nestingLevel 方法参数中collection/array元素或map key/value声明的嵌套级别
     * @return 指定嵌套级别的嵌套类型描述符，如果无法获取，则为null
     * @throws IllegalArgumentException 如果达到指定嵌套级别的类型不是集合、数组或映射类型
     */
    public static TypeDescriptor nested(Field field, int nestingLevel) {
        return nested(new TypeDescriptor(field), nestingLevel);
    }

    /**
     * 为属性中声明的嵌套类型创建类型描述符
     *
     * @param property     属性
     * @param nestingLevel 方法参数中collection/array元素或map key/value声明的嵌套级别
     * @return 指定嵌套级别的嵌套类型描述符，如果无法获取，则为null
     * @throws IllegalArgumentException 如果达到指定嵌套级别的类型不是集合、数组或映射类型
     */
    public static TypeDescriptor nested(Property property, int nestingLevel) {
        return nested(new TypeDescriptor(property), nestingLevel);
    }

    private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
        ResolvableType nested = typeDescriptor.resolvableType;
        for (int i = 0; i < nestingLevel; i++) {
            // noinspection StatementWithEmptyBody
            if (Object.class == nested.getType()) {
                // Could be a collection type but we don't know about its element type,
                // so let's just assume there is an element type of type Object...
            } else {
                nested = nested.getNested(2);
            }
        }
        if (nested == ResolvableType.NONE) {
            return null;
        }
        return getRelatedIfResolvable(typeDescriptor, nested);
    }

    private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
        if (type.resolve() == null) {
            return null;
        }
        return new TypeDescriptor(type, null, source.getAnnotations());
    }

    /**
     * 适配器类，用于将{@code TypeDescriptor}的注解作为{@link AnnotatedElement}公开，尤其是向{@link AnnotatedElementUtils}公开
     *
     * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
     * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
     */
    private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {
        private final Annotation[] annotations;

        public AnnotatedElementAdapter(Annotation[] annotations) {
            this.annotations = annotations;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            for (Annotation annotation : getAnnotations()) {
                if (annotation.annotationType() == annotationClass) {
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (Annotation annotation : getAnnotations()) {
                if (annotation.annotationType() == annotationClass) {
                    return (T) annotation;
                }
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return (this.annotations != null ? this.annotations.clone() : EMPTY_ANNOTATION_ARRAY);
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return getAnnotations();
        }

        public boolean isEmpty() {
            return ObjectUtils.isEmpty(this.annotations);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof AnnotatedElementAdapter && Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.annotations);
        }

        @Override
        public String toString() {
            return TypeDescriptor.this.toString();
        }
    }
}
