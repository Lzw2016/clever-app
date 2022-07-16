package org.clever.core;

import kotlin.Unit;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 函数(包括构造函数)参数类型信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 10:51 <br/>
 */
public class MethodParameter {
    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];
    /**
     * 当前的函数对象(包括构造函数)<br/>
     * Executable类是Method和Constructor的父类
     */
    private final Executable executable;
    /**
     * 参数位置
     */
    private final int parameterIndex;
    /**
     * 当前函数{@link #parameterIndex}位置的参数对象
     */
    private volatile Parameter parameter;
    /**
     * 目标类型的嵌套等级
     */
    private int nestingLevel;
    /**
     * <pre>{@code Map<嵌套等级, 参数位置>}</pre>
     */
    Map<Integer, Integer> typeIndexesPerLevel;
    /**
     * 当前函数所属的class，可以通过覆盖{@link #getContainingClass()}函数自定义
     */
    private volatile Class<?> containingClass;
    /**
     * 当前参数类型
     */
    private volatile Class<?> parameterType;
    /**
     * 当前泛型参数类型
     */
    private volatile Type genericParameterType;
    /**
     * 当前参数的注解
     */
    private volatile Annotation[] parameterAnnotations;
    /**
     * 函数的参数名称获取器
     */
    private volatile ParameterNameDiscoverer parameterNameDiscoverer;
    /**
     * 当前参数名
     */
    private volatile String parameterName;
    /**
     * 内嵌的方法参数
     */
    private volatile MethodParameter nestedMethodParameter;

    /**
     * 基于{@code Method}创建一个嵌套级别为1的{@code MethodParameter}
     *
     * @param method         指定参数的函数
     * @param parameterIndex 参数的索引位置：-1表示方法的返回类型；0表示第一个方法参数；1表示第二个方法参数，以此类推
     */
    public MethodParameter(Method method, int parameterIndex) {
        this(method, parameterIndex, 1);
    }

    /**
     * 基于{@code Method}创建{@code MethodParameter}
     *
     * @param method         指定参数的函数
     * @param parameterIndex 参数的索引位置：-1表示方法的返回类型；0表示第一个方法参数；1表示第二个方法参数，以此类推
     * @param nestingLevel   目标类型的嵌套等级(通常为1；例如，对于列表列表，1表示嵌套列表，而2表示嵌套列表的元素)
     */
    public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
        Assert.notNull(method, "Method must not be null");
        this.executable = method;
        this.parameterIndex = validateIndex(method, parameterIndex);
        this.nestingLevel = nestingLevel;
    }

    /**
     * 基于{@code Constructor}创建一个嵌套级别为1的{@code MethodParameter}
     *
     * @param constructor    指定参数的构造函数
     * @param parameterIndex 参数的位置
     */
    public MethodParameter(Constructor<?> constructor, int parameterIndex) {
        this(constructor, parameterIndex, 1);
    }

    /**
     * 基于{@code Constructor}创建{@code MethodParameter}
     *
     * @param constructor    指定参数的构造函数
     * @param parameterIndex 参数的位置
     * @param nestingLevel   目标类型的嵌套等级(通常为1；例如，对于列表列表，1表示嵌套列表，而2表示嵌套列表的元素)
     */
    public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
        Assert.notNull(constructor, "Constructor must not be null");
        this.executable = constructor;
        this.parameterIndex = validateIndex(constructor, parameterIndex);
        this.nestingLevel = nestingLevel;
    }

    /**
     * 内部构造函数，创建{@code MethodParameter}，手动指定{@code containingClass}
     *
     * @param executable      设置executable
     * @param parameterIndex  参数的位置
     * @param containingClass 设置containingClass
     */
    MethodParameter(Executable executable, int parameterIndex, Class<?> containingClass) {
        Assert.notNull(executable, "Executable must not be null");
        this.executable = executable;
        this.parameterIndex = validateIndex(executable, parameterIndex);
        this.nestingLevel = 1;
        this.containingClass = containingClass;
    }

    /**
     * 用于Copy的构造函数，从原始MethodParameter对象复制一个相同的新MethodParameter对象
     *
     * @param original 原始MethodParameter对象
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public MethodParameter(MethodParameter original) {
        Assert.notNull(original, "Original must not be null");
        this.executable = original.executable;
        this.parameterIndex = original.parameterIndex;
        this.parameter = original.parameter;
        this.nestingLevel = original.nestingLevel;
        this.typeIndexesPerLevel = original.typeIndexesPerLevel;
        this.containingClass = original.containingClass;
        this.parameterType = original.parameterType;
        this.genericParameterType = original.genericParameterType;
        this.parameterAnnotations = original.parameterAnnotations;
        this.parameterNameDiscoverer = original.parameterNameDiscoverer;
        this.parameterName = original.parameterName;
    }

    /**
     * 返回包装的函数
     *
     * @return 函数对象，如果没有就返回{@code null}
     */
    public Method getMethod() {
        return (this.executable instanceof Method ? (Method) this.executable : null);
    }

    /**
     * 返回包装的构造函数
     *
     * @return 构造函数对象，如果没有就返回{@code null}
     */
    public Constructor<?> getConstructor() {
        return (this.executable instanceof Constructor ? (Constructor<?>) this.executable : null);
    }

    /**
     * 返回声明底层函数或者构造函数的类
     */
    public Class<?> getDeclaringClass() {
        return this.executable.getDeclaringClass();
    }

    /**
     * 返回已包装的成员
     *
     * @return 函数或构造函数作为成员
     */
    public Member getMember() {
        return this.executable;
    }

    /**
     * 返回已包装的注解元素
     *
     * @return 函数或构造函数作为注解元素
     */
    public AnnotatedElement getAnnotatedElement() {
        return this.executable;
    }

    public Executable getExecutable() {
        return this.executable;
    }

    /**
     * 返回函数或构造函数参数对象
     */
    public Parameter getParameter() {
        if (this.parameterIndex < 0) {
            throw new IllegalStateException("Cannot retrieve Parameter descriptor for method return type");
        }
        Parameter parameter = this.parameter;
        if (parameter == null) {
            parameter = getExecutable().getParameters()[this.parameterIndex];
            this.parameter = parameter;
        }
        return parameter;
    }

    public int getParameterIndex() {
        return this.parameterIndex;
    }

    /**
     * 目标类型的嵌套等级(通常为1；例如，对于列表列表，1表示嵌套列表，而2表示嵌套列表的元素)
     */
    public int getNestingLevel() {
        return this.nestingLevel;
    }

    /**
     * 返回此{@code MethodParameter}的变体，类型为当前级别设置的指定值
     *
     * @param typeIndex 新的类型索引
     */
    public MethodParameter withTypeIndex(int typeIndex) {
        return nested(this.nestingLevel, typeIndex);
    }

    /**
     * 返回当前嵌套等级的类型索引
     *
     * @return 对应的类型索引(或默认类型索引为null)
     * @see #getNestingLevel()
     */

    public Integer getTypeIndexForCurrentLevel() {
        return getTypeIndexForLevel(this.nestingLevel);
    }

    /**
     * 返回指定嵌套等级的类型索引
     *
     * @param nestingLevel 要检查的嵌套等级
     * @return 对应的类型索引(或默认类型索引为null)
     */
    public Integer getTypeIndexForLevel(int nestingLevel) {
        return getTypeIndexesPerLevel().get(nestingLevel);
    }

    /**
     * 获取(延迟构造的) {@code Map<嵌套等级, 参数位置>}
     */
    private Map<Integer, Integer> getTypeIndexesPerLevel() {
        if (this.typeIndexesPerLevel == null) {
            this.typeIndexesPerLevel = new HashMap<>(4);
        }
        return this.typeIndexesPerLevel;
    }

    /**
     * 返回此{@code MethodParameter}的变体，它指向同一个参数，但嵌套层次更深
     */
    public MethodParameter nested() {
        return nested(null);
    }

    /**
     * 返回此{@code MethodParameter}的变体,它指向同一个参数，但嵌套层次更深
     *
     * @param typeIndex 新嵌套等级的类型索引
     */
    public MethodParameter nested(Integer typeIndex) {
        MethodParameter nestedParam = this.nestedMethodParameter;
        if (nestedParam != null && typeIndex == null) {
            return nestedParam;
        }
        nestedParam = nested(this.nestingLevel + 1, typeIndex);
        if (typeIndex == null) {
            this.nestedMethodParameter = nestedParam;
        }
        return nestedParam;
    }

    /**
     * 返回此{@code MethodParameter}的变体，它指向给定嵌套等级，以及给定的类型索引
     *
     * @param nestingLevel 嵌套等级
     * @param typeIndex    类型索引
     */
    private MethodParameter nested(int nestingLevel, Integer typeIndex) {
        MethodParameter copy = clone();
        copy.nestingLevel = nestingLevel;
        if (this.typeIndexesPerLevel != null) {
            copy.typeIndexesPerLevel = new HashMap<>(this.typeIndexesPerLevel);
        }
        if (typeIndex != null) {
            copy.getTypeIndexesPerLevel().put(copy.nestingLevel, typeIndex);
        }
        copy.parameterType = null;
        copy.genericParameterType = null;
        return copy;
    }

    /**
     * 当前参数是否是可选参数：以Java8的{@link java.util.Optional}形式、
     * 参数级{@code Nullable}注解的任何变体(例如来自JSR-305或FindBugs注释集)
     * 或者Kotlin中的语言集可为null的类型声明
     */
    public boolean isOptional() {
        return getParameterType() == Optional.class
                || hasNullableAnnotation()
                || (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) && KotlinDelegate.isOptional(this));
    }

    /**
     * 检查此方法的参数注解是否带有{@code Nullable}注解的任何变体，
     * 比如：{@code javax.annotation.Nullable} 或者 {@code edu.umd.cs.findbugs.annotations.Nullable}
     */
    private boolean hasNullableAnnotation() {
        for (Annotation ann : getParameterAnnotations()) {
            if ("Nullable".equals(ann.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回{@code MethodParameter}的变体，
     * 该变体执行同一个参数但是在声明{@link java.util.Optional}的情况下，
     * 嵌套级别更深
     */
    public MethodParameter nestedIfOptional() {
        return (getParameterType() == Optional.class ? nested() : this);
    }

    /**
     * 返回{@code MethodParameter}的变体，该变体指向给定的containingClass
     *
     * @param containingClass 指定的containingClass(可能是声明类的子类，比如替换类型变量)
     * @see #getParameterType()
     */
    public MethodParameter withContainingClass(Class<?> containingClass) {
        MethodParameter result = clone();
        result.containingClass = containingClass;
        result.parameterType = null;
        return result;
    }

    /**
     * 返回方法参数的containingClass
     *
     * @return 指定的containingClass(可能是声明类的子类 ， 否则只是声明类本身)
     * @see #getDeclaringClass()
     */
    public Class<?> getContainingClass() {
        Class<?> containingClass = this.containingClass;
        return (containingClass != null ? containingClass : getDeclaringClass());
    }

    /**
     * 返回函数或构造函数的当前参数类型
     *
     * @return 当前参数类型(不能为null)
     */
    public Class<?> getParameterType() {
        Class<?> paramType = this.parameterType;
        if (paramType != null) {
            return paramType;
        }
        if (getContainingClass() != getDeclaringClass()) {
            paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
        }
        if (paramType == null) {
            paramType = computeParameterType();
        }
        this.parameterType = paramType;
        return paramType;
    }

    /**
     * 返回函数或构造函数的泛型参数类型
     *
     * @return 泛型参数类型(不能为null)
     */
    public Type getGenericParameterType() {
        Type paramType = this.genericParameterType;
        if (paramType == null) {
            if (this.parameterIndex < 0) {
                // 函数返回值类型
                Method method = getMethod();
                paramType = (
                        method != null ?
                                (
                                        KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) ?
                                                KotlinDelegate.getGenericReturnType(method)
                                                : method.getGenericReturnType()
                                )
                                : void.class
                );
            } else {
                // 函数参数类型
                Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
                int index = this.parameterIndex;
                if (this.executable instanceof Constructor
                        && ClassUtils.isInnerClass(this.executable.getDeclaringClass())
                        && genericParameterTypes.length == this.executable.getParameterCount() - 1) {
                    // Bug in javac: type array excludes enclosing instance parameter
                    // for inner classes with at least one generic constructor parameter,
                    // so access it with the actual parameter index lowered by 1
                    index = this.parameterIndex - 1;
                }
                paramType = (index >= 0 && index < genericParameterTypes.length ? genericParameterTypes[index] : computeParameterType());
            }
            this.genericParameterType = paramType;
        }
        return paramType;
    }

    /**
     * 计算参数类型，如果当前参数索引小于0，返回method的返回值类型；否则返回executable的参数类型
     */
    private Class<?> computeParameterType() {
        if (this.parameterIndex < 0) {
            Method method = getMethod();
            if (method == null) {
                return void.class;
            }
            if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass())) {
                return KotlinDelegate.getReturnType(method);
            }
            return method.getReturnType();
        }
        return this.executable.getParameterTypes()[this.parameterIndex];
    }

    /**
     * 返回method/constructor参数嵌套类型
     *
     * @return 参数类型(不能为null)
     * @see #getNestingLevel()
     */
    @SuppressWarnings("DuplicatedCode")
    public Class<?> getNestedParameterType() {
        if (this.nestingLevel > 1) {
            Type type = getGenericParameterType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    Integer index = getTypeIndexForLevel(i);
                    type = args[index != null ? index : args.length - 1];
                }
                // Object.class if unresolvable
            }
            if (type instanceof Class) {
                return (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) type).getRawType();
                if (arg instanceof Class) {
                    return (Class<?>) arg;
                }
            }
            return Object.class;
        } else {
            return getParameterType();
        }
    }

    /**
     * 返回method/constructor参数的嵌套泛型类型
     *
     * @return 泛型参数类型(不能为null)
     * @see #getNestingLevel()
     */
    @SuppressWarnings("DuplicatedCode")
    public Type getNestedGenericParameterType() {
        if (this.nestingLevel > 1) {
            Type type = getGenericParameterType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    Integer index = getTypeIndexForLevel(i);
                    type = args[index != null ? index : args.length - 1];
                }
            }
            return type;
        } else {
            return getGenericParameterType();
        }
    }

    /**
     * 返回与目标method或constructor本身关联的经过后处理的注解
     */
    public Annotation[] getMethodAnnotations() {
        return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
    }

    /**
     * 返回给定类型的method/constructor注解(如果有)
     *
     * @param annotationType 要查找的注解类型
     * @return 注解对象，如果没有找到返回{@code null}
     */
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
        A annotation = getAnnotatedElement().getAnnotation(annotationType);
        return (annotation != null ? adaptAnnotation(annotation) : null);
    }

    /**
     * 返回method/constructor是否使用给定类型进行注释
     *
     * @param annotationType 要查找的注解
     * @see #getMethodAnnotation(Class)
     */
    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
        return getAnnotatedElement().isAnnotationPresent(annotationType);
    }

    /**
     * 返回与指定(当前)method/constructor参数关联的注解数组
     */
    public Annotation[] getParameterAnnotations() {
        Annotation[] paramAnns = this.parameterAnnotations;
        if (paramAnns == null) {
            Annotation[][] annotationArray = this.executable.getParameterAnnotations();
            int index = this.parameterIndex;
            if (this.executable instanceof Constructor
                    && ClassUtils.isInnerClass(this.executable.getDeclaringClass())
                    && annotationArray.length == this.executable.getParameterCount() - 1) {
                // Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
                // for inner classes, so access it with the actual parameter index lowered by 1
                index = this.parameterIndex - 1;
            }
            paramAnns = (index >= 0 && index < annotationArray.length ? adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);
            this.parameterAnnotations = paramAnns;
        }
        return paramAnns;
    }

    /**
     * 如果参数至少有一个注解，返回ture;如果没有，返回false
     *
     * @see #getParameterAnnotations()
     */
    public boolean hasParameterAnnotations() {
        return (getParameterAnnotations().length != 0);
    }

    /**
     * 返回给定类型的参数注解，如果可用
     *
     * @param annotationType 要查找的注解类型
     * @return 注解对象，如果没有找到{@code null}
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
        Annotation[] anns = getParameterAnnotations();
        for (Annotation ann : anns) {
            if (annotationType.isInstance(ann)) {
                return (A) ann;
            }
        }
        return null;
    }

    /**
     * 返回参数是否具有给定的注解类型声明
     *
     * @param annotationType 要查找的注解类型
     * @see #getParameterAnnotation(Class)
     */
    public <A extends Annotation> boolean hasParameterAnnotation(Class<A> annotationType) {
        return (getParameterAnnotation(annotationType) != null);
    }

    /**
     * 初始化此方法参数的参数名称发现器，
     * 此时，该方法实际上并不会尝试检索参数名称；它只允许在应用程序调用{@link #getParameterName()}时进行发现
     */
    public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    /**
     * 返回method/constructor参数的名称
     *
     * @return 参数名(如果在类文件中不包含任何参数名称元数据, 或者没有设置initParameterNameDiscovery, 则为null)
     */
    public String getParameterName() {
        if (this.parameterIndex < 0) {
            return null;
        }
        ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
        if (discoverer != null) {
            String[] parameterNames = null;
            if (this.executable instanceof Method) {
                parameterNames = discoverer.getParameterNames((Method) this.executable);
            } else if (this.executable instanceof Constructor) {
                parameterNames = discoverer.getParameterNames((Constructor<?>) this.executable);
            }
            if (parameterNames != null) {
                this.parameterName = parameterNames[this.parameterIndex];
            }
            this.parameterNameDiscoverer = null;
        }
        return this.parameterName;
    }

    /**
     * 将给定的注解返回给调用方之前对他进行后处理的模板方法<br/>
     * 默认实例简单照原样返回给定的注解
     *
     * @param annotation 将要返回的注解
     * @return 处理后的注解数组(或者简单原注解数组)
     */
    protected <A extends Annotation> A adaptAnnotation(A annotation) {
        return annotation;
    }

    /**
     * 将给定的注解数组返回给调用方之前对他进行后处理的模板方法<br/>
     * 默认实例简单照原样返回给定的注解数组
     *
     * @param annotations 将要返回的注解数组
     * @return 处理后的注解数组(或者简单原注解数组)
     */
    protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
        return annotations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodParameter)) {
            return false;
        }
        MethodParameter otherParam = (MethodParameter) other;
        return getContainingClass() == otherParam.getContainingClass() &&
                ObjectUtils.nullSafeEquals(this.typeIndexesPerLevel, otherParam.typeIndexesPerLevel) &&
                this.nestingLevel == otherParam.nestingLevel &&
                this.parameterIndex == otherParam.parameterIndex &&
                this.executable.equals(otherParam.executable);
    }

    @Override
    public int hashCode() {
        return (31 * this.executable.hashCode() + this.parameterIndex);
    }

    @Override
    public String toString() {
        Method method = getMethod();
        return (method != null ? "method '" + method.getName() + "'" : "constructor") + " parameter " + this.parameterIndex;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public MethodParameter clone() {
        return new MethodParameter(this);
    }

    /**
     * 为给定的method或者constructor创建一个新的MethodParameter对象<br/>
     * 这是一个方便的工厂方法，适用于以通用方式处理method或者constructor引用情况
     *
     * @param executable     method或者constructor以指定参数
     * @param parameterIndex 参数位置
     * @return 对应的MethodParameter实例
     */
    public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
        if (executable instanceof Method) {
            return new MethodParameter((Method) executable, parameterIndex);
        } else if (executable instanceof Constructor) {
            return new MethodParameter((Constructor<?>) executable, parameterIndex);
        } else {
            throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
        }
    }

    /**
     * 为给定的参数描述符创建一个新的MethodParameter对象<br/>
     * 对于Java8 {@link Parameter}描述符已经可用的情况，这个一个编辑的工厂方法
     *
     * @param parameter 参数描述符
     * @return 对应的MethodParameter实例
     */
    public static MethodParameter forParameter(Parameter parameter) {
        return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
    }

    /**
     * 找出参数索引
     */
    protected static int findParameterIndex(Parameter parameter) {
        Executable executable = parameter.getDeclaringExecutable();
        Parameter[] allParams = executable.getParameters();
        // Try first with identity checks for greater performance.
        for (int i = 0; i < allParams.length; i++) {
            if (parameter == allParams[i]) {
                return i;
            }
        }
        // Potentially try again with object equality checks in order to avoid race
        // conditions while invoking java.lang.reflect.Executable.getParameters().
        for (int i = 0; i < allParams.length; i++) {
            if (parameter.equals(allParams[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Given parameter [" + parameter + "] does not match any parameter in the declaring executable");
    }

    /**
     * 验证参数位置
     *
     * @param executable     method或者constructor对象
     * @param parameterIndex 参数位置
     */
    private static int validateIndex(Executable executable, int parameterIndex) {
        int count = executable.getParameterCount();
        Assert.isTrue(parameterIndex >= -1 && parameterIndex < count, () -> "Parameter index needs to be between -1 and " + (count - 1));
        return parameterIndex;
    }

    /**
     * 内部类，以避免在运行时严重依赖Kotlin
     */
    private static class KotlinDelegate {
        /**
         * 检查给定的{@link MethodParameter}是否表示nullable Kotlin类型或者optional参数(在Kotlin声明中具有默认值)
         */
        public static boolean isOptional(MethodParameter param) {
            Method method = param.getMethod();
            int index = param.getParameterIndex();
            if (method != null && index == -1) {
                KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
                return (function != null && function.getReturnType().isMarkedNullable());
            }
            KFunction<?> function;
            Predicate<KParameter> predicate;
            if (method != null) {
                if (param.getParameterType().getName().equals("kotlin.coroutines.Continuation")) {
                    return true;
                }
                function = ReflectJvmMapping.getKotlinFunction(method);
                predicate = p -> KParameter.Kind.VALUE.equals(p.getKind());
            } else {
                Constructor<?> ctor = param.getConstructor();
                Assert.state(ctor != null, "Neither method nor constructor found");
                function = ReflectJvmMapping.getKotlinFunction(ctor);
                predicate = p -> (KParameter.Kind.VALUE.equals(p.getKind()) ||
                        KParameter.Kind.INSTANCE.equals(p.getKind()));
            }
            if (function != null) {
                int i = 0;
                for (KParameter kParameter : function.getParameters()) {
                    if (predicate.test(kParameter)) {
                        if (index == i++) {
                            return (kParameter.getType().isMarkedNullable() || kParameter.isOptional());
                        }
                    }
                }
            }
            return false;
        }

        /**
         * 返回方法的泛型返回类型,通过Kotlin反射支持暂停功能
         */
        private static Type getGenericReturnType(Method method) {
            try {
                KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
                if (function != null && function.isSuspend()) {
                    return ReflectJvmMapping.getJavaType(function.getReturnType());
                }
            } catch (UnsupportedOperationException ex) {
                // probably a synthetic class - let's use java reflection instead
            }
            return method.getGenericReturnType();
        }

        /**
         * 返回method的返回类型,通过Kotlin反射支持暂停功能
         */
        private static Class<?> getReturnType(Method method) {
            try {
                KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
                if (function != null && function.isSuspend()) {
                    Type paramType = ReflectJvmMapping.getJavaType(function.getReturnType());
                    if (paramType == Unit.class) {
                        paramType = void.class;
                    }
                    return ResolvableType.forType(paramType).resolve(method.getReturnType());
                }
            } catch (UnsupportedOperationException ex) {
                // probably a synthetic class - let's use java reflection instead
            }
            return method.getReturnType();
        }
    }
}
