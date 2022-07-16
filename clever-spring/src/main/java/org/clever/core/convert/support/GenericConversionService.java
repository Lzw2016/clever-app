package org.clever.core.convert.support;

import org.clever.core.ResolvableType;
import org.clever.core.convert.ConversionFailedException;
import org.clever.core.convert.ConverterNotFoundException;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.*;
import org.clever.core.convert.converter.GenericConverter.ConvertiblePair;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.StringUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 基本的ConversionService实现，适用于大多数环境
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:04 <br/>
 */
public class GenericConversionService implements ConfigurableConversionService {
    /**
     * 无操作转换器，不需要转换时使用
     */
    private static final GenericConverter NO_OP_CONVERTER = new NoOpConverter("NO_OP");
    /**
     * 表示没有可用转换器，用于表示缓存中的null值<br/>
     * 由于ConcurrentReferenceHashMap中不允许存放null值，所以使用当前值表示null值缓存
     */
    private static final GenericConverter NO_MATCH = new NoOpConverter("NO_MATCH");
    /**
     * 管理所有转换器的对象
     */
    private final Converters converters = new Converters();
    /**
     * 转换器缓存，提高从{@link #converters}中查询转换器的效率，{@link #converters}内容变化后需要清除缓存
     * <pre>{@code Map<ConverterCacheKey, GenericConverter>}</pre>
     */
    private final Map<ConverterCacheKey, GenericConverter> converterCache = new ConcurrentReferenceHashMap<>(64);

    // ConverterRegistry implementation

    @Override
    public void addConverter(Converter<?, ?> converter) {
        ResolvableType[] typeInfo = getRequiredTypeInfo(converter.getClass(), Converter.class);
        if (typeInfo == null) {
            throw new IllegalArgumentException(
                    "Unable to determine source type <S> and target type <T> for your " +
                            "Converter [" +
                            converter.getClass().getName() +
                            "]; does the class parameterize those types?"
            );
        }
        addConverter(new ConverterAdapter(converter, typeInfo[0], typeInfo[1]));
    }

    @Override
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter) {
        addConverter(new ConverterAdapter(converter, ResolvableType.forClass(sourceType), ResolvableType.forClass(targetType)));
    }

    @Override
    public void addConverter(GenericConverter converter) {
        this.converters.add(converter);
        invalidateCache();
    }

    @Override
    public void addConverterFactory(ConverterFactory<?, ?> factory) {
        ResolvableType[] typeInfo = getRequiredTypeInfo(factory.getClass(), ConverterFactory.class);
        if (typeInfo == null) {
            throw new IllegalArgumentException(
                    "Unable to determine source type <S> and target type <T> for your " +
                            "ConverterFactory [" +
                            factory.getClass().getName() +
                            "]; does the class parameterize those types?"
            );
        }
        addConverter(new ConverterFactoryAdapter(factory, new ConvertiblePair(typeInfo[0].toClass(), typeInfo[1].toClass())));
    }

    @Override
    public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
        this.converters.remove(sourceType, targetType);
        invalidateCache();
    }

    // ConversionService implementation

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        Assert.notNull(targetType, "Target type to convert to cannot be null");
        return canConvert((sourceType != null ? TypeDescriptor.valueOf(sourceType) : null), TypeDescriptor.valueOf(targetType));
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Assert.notNull(targetType, "Target type to convert to cannot be null");
        if (sourceType == null) {
            return true;
        }
        GenericConverter converter = getConverter(sourceType, targetType);
        return (converter != null);
    }

    /**
     * 判断是否可以绕过源类型和目标类型之间的转换 <br/>
     * 表示源对象可以直接赋值给目标类型的变量
     *
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     * @return 如果转换之后的返回对象就是源对象则返回true
     * @throws IllegalArgumentException 如果targetType值为 {@code null}
     */
    public boolean canBypassConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Assert.notNull(targetType, "Target type to convert to cannot be null");
        if (sourceType == null) {
            return true;
        }
        GenericConverter converter = getConverter(sourceType, targetType);
        return (converter == NO_OP_CONVERTER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        Assert.notNull(targetType, "Target type to convert to cannot be null");
        return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        Assert.notNull(targetType, "Target type to convert to cannot be null");
        if (sourceType == null) {
            Assert.isTrue(source == null, "Source must be [null] if source type == [null]");
            return handleResult(null, targetType, convertNullSource(null, targetType));
        }
        if (source != null && !sourceType.getObjectType().isInstance(source)) {
            throw new IllegalArgumentException(
                    "Source to convert from must be an instance of [" +
                            sourceType +
                            "]; instead it was a [" +
                            source.getClass().getName() +
                            "]"
            );
        }
        GenericConverter converter = getConverter(sourceType, targetType);
        if (converter != null) {
            Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
            return handleResult(sourceType, targetType, result);
        }
        return handleConverterNotFound(source, sourceType, targetType);
    }

    /**
     * 将源对象转换为指定targetType的便捷操作
     *
     * @param source     源对象
     * @param targetType 目标TypeDescriptor
     * @throws IllegalArgumentException 如果targetType为null，或sourceType为null但source不为null
     */
    public Object convert(Object source, TypeDescriptor targetType) {
        return convert(source, TypeDescriptor.forObject(source), targetType);
    }

    @Override
    public String toString() {
        return this.converters.toString();
    }

    // Protected template methods

    /**
     * 处理null值，根据targetType适配Java8的Optional类型 {@link java.util.Optional#empty()}
     *
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     */
    protected Object convertNullSource(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.getObjectType() == Optional.class) {
            return Optional.empty();
        }
        return null;
    }

    /**
     * 根据源TypeDescriptor和目标TypeDescriptor在converters中查找可用的转换器 <br/>
     * 使用converterCache缓存提高效率
     *
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     * @return 如果找不到合适的转换器就返回null
     * @see #getDefaultConverter(TypeDescriptor, TypeDescriptor)
     */
    protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
        // 从converterCache(缓存)中查找
        ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
        GenericConverter converter = this.converterCache.get(key);
        if (converter != null) {
            return (converter != NO_MATCH ? converter : null);
        }
        // 从converters中查找
        converter = this.converters.find(sourceType, targetType);
        if (converter == null) {
            converter = getDefaultConverter(sourceType, targetType);
        }
        // 缓存转换类型与转换器的对应关系，下次查找使用缓存
        if (converter != null) {
            this.converterCache.put(key, converter);
            return converter;
        }
        // 未找到也缓存一个NO_MATCH转换器，使得下次查找可以使用缓存提高效率
        this.converterCache.put(key, NO_MATCH);
        return null;
    }

    /**
     * 如果源类型的对象可以赋值给目标类型变量，就返回NO_OP_CONVERTER(无操作转换器) <br/>
     * 否则返回null，表示找不到合适的转换器
     *
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     */
    protected GenericConverter getDefaultConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (sourceType.isAssignableTo(targetType) ? NO_OP_CONVERTER : null);
    }

    // Internal helpers

    /**
     * 获取转换器类型的sourceType和targetType
     *
     * @param converterClass 带泛型的转换器类型
     * @param genericIfc     原始转换器类型(不带泛型)
     * @return 未找到sourceType和targetType返回null
     */
    private ResolvableType[] getRequiredTypeInfo(Class<?> converterClass, Class<?> genericIfc) {
        ResolvableType resolvableType = ResolvableType.forClass(converterClass).as(genericIfc);
        ResolvableType[] generics = resolvableType.getGenerics();
        if (generics.length < 2) {
            return null;
        }
        Class<?> sourceType = generics[0].resolve();
        Class<?> targetType = generics[1].resolve();
        if (sourceType == null || targetType == null) {
            return null;
        }
        return generics;
    }

    /**
     * 清空转换器缓存
     */
    private void invalidateCache() {
        this.converterCache.clear();
    }

    /**
     * 当未找到转换器时，返回转换后的结果<br/>
     * 可能返回null、源对象、抛出异常
     *
     * @param source     源对象
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     */
    private Object handleConverterNotFound(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            assertNotPrimitiveTargetType(sourceType, targetType);
            return null;
        }
        if ((sourceType == null || sourceType.isAssignableTo(targetType)) && targetType.getObjectType().isInstance(source)) {
            return source;
        }
        throw new ConverterNotFoundException(sourceType, targetType);
    }

    /**
     * 处理转换之后的对象
     *
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     * @param result     转换之后的对象
     */
    private Object handleResult(TypeDescriptor sourceType, TypeDescriptor targetType, Object result) {
        if (result == null) {
            assertNotPrimitiveTargetType(sourceType, targetType);
        }
        return result;
    }

    /**
     * 断言targetType不是Java语言基础类型
     */
    private void assertNotPrimitiveTargetType(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.isPrimitive()) {
            throw new ConversionFailedException(
                    sourceType,
                    targetType,
                    null,
                    new IllegalArgumentException("A null value cannot be assigned to a primitive type")
            );
        }
    }

    /**
     * Converter(1:1的转换器)适配器，实现{@link GenericConverter}和{@link ConditionalConverter}接口
     */
    private final class ConverterAdapter implements ConditionalGenericConverter {
        /**
         * 原始的转换器(被适配的转换器)
         */
        private final Converter<Object, Object> converter;
        /**
         * 转换的源类型和目标类型信息
         */
        private final ConvertiblePair typeInfo;
        /**
         * 转换的目标类型
         */
        private final ResolvableType targetType;

        @SuppressWarnings("unchecked")
        public ConverterAdapter(Converter<?, ?> converter, ResolvableType sourceType, ResolvableType targetType) {
            this.converter = (Converter<Object, Object>) converter;
            this.typeInfo = new ConvertiblePair(sourceType.toClass(), targetType.toClass());
            this.targetType = targetType;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(this.typeInfo);
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            // Check raw type first...
            if (this.typeInfo.getTargetType() != targetType.getObjectType()) {
                return false;
            }
            // Full check for complex generic type match required?
            ResolvableType rt = targetType.getResolvableType();
            if (!(rt.getType() instanceof Class) && !rt.isAssignableFrom(this.targetType) && !this.targetType.hasUnresolvableGenerics()) {
                return false;
            }
            return !(this.converter instanceof ConditionalConverter) || ((ConditionalConverter) this.converter).matches(sourceType, targetType);
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source == null) {
                return convertNullSource(sourceType, targetType);
            }
            return this.converter.convert(source);
        }

        @Override
        public String toString() {
            return (this.typeInfo + " : " + this.converter);
        }
    }

    /**
     * ConverterFactory(1:N的转换器)适配器，实现{@link GenericConverter}和{@link ConditionalConverter}接口
     */
    private final class ConverterFactoryAdapter implements ConditionalGenericConverter {
        /**
         * 原始的转换器(被适配的转换器)
         */
        private final ConverterFactory<Object, Object> converterFactory;
        /**
         * 转换的源类型和目标类型信息
         */
        private final ConvertiblePair typeInfo;

        @SuppressWarnings("unchecked")
        public ConverterFactoryAdapter(ConverterFactory<?, ?> converterFactory, ConvertiblePair typeInfo) {
            this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
            this.typeInfo = typeInfo;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(this.typeInfo);
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            boolean matches = true;
            if (this.converterFactory instanceof ConditionalConverter) {
                matches = ((ConditionalConverter) this.converterFactory).matches(sourceType, targetType);
            }
            if (matches) {
                Converter<?, ?> converter = this.converterFactory.getConverter(targetType.getType());
                if (converter instanceof ConditionalConverter) {
                    matches = ((ConditionalConverter) converter).matches(sourceType, targetType);
                }
            }
            return matches;
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source == null) {
                return convertNullSource(sourceType, targetType);
            }
            return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
        }

        @Override
        public String toString() {
            return (this.typeInfo + " : " + this.converterFactory);
        }
    }

    /**
     * 转换器缓存的Key值类型<br/>
     * 两个实例的源类型描述符(TypeDescriptor)与目标类型描述符(TypeDescriptor)分别相同(TypeDescriptor.equals==true)就是相同的两个实例
     */
    private static final class ConverterCacheKey implements Comparable<ConverterCacheKey> {
        private final TypeDescriptor sourceType;
        private final TypeDescriptor targetType;

        public ConverterCacheKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ConverterCacheKey)) {
                return false;
            }
            ConverterCacheKey otherKey = (ConverterCacheKey) other;
            return (this.sourceType.equals(otherKey.sourceType)) && this.targetType.equals(otherKey.targetType);
        }

        @Override
        public int hashCode() {
            return (this.sourceType.hashCode() * 29 + this.targetType.hashCode());
        }

        @Override
        public String toString() {
            return ("ConverterCacheKey [sourceType = " + this.sourceType + ", targetType = " + this.targetType + "]");
        }

        @Override
        public int compareTo(ConverterCacheKey other) {
            int result = this.sourceType.getResolvableType().toString().compareTo(other.sourceType.getResolvableType().toString());
            if (result == 0) {
                result = this.targetType.getResolvableType().toString().compareTo(other.targetType.getResolvableType().toString());
            }
            return result;
        }
    }

    /**
     * 用于管理所有注册的转换器
     */
    private static class Converters {
        /**
         * 全局转换器(实现了ConditionalConverter接口的转换器)
         */
        private final Set<GenericConverter> globalConverters = new CopyOnWriteArraySet<>();
        /**
         * 非全局装换器，能确定转换类型(ConvertiblePair)集合的转换器
         */
        private final Map<ConvertiblePair, ConvertersForPair> converters = new ConcurrentHashMap<>(256);

        /**
         * 增加一个转换器<br/>
         * 根据转换器特性自动分类是否是全局转换器
         */
        public void add(GenericConverter converter) {
            Set<ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
            if (convertibleTypes == null) {
                Assert.state(converter instanceof ConditionalConverter, "Only conditional converters may return null convertible types");
                this.globalConverters.add(converter);
            } else {
                for (ConvertiblePair convertiblePair : convertibleTypes) {
                    getMatchableConverters(convertiblePair).add(converter);
                }
            }
        }

        /**
         * 删除非全局装换器
         */
        public void remove(Class<?> sourceType, Class<?> targetType) {
            this.converters.remove(new ConvertiblePair(sourceType, targetType));
        }

        /**
         * 根据ConvertiblePair创建并缓存ConvertersForPair用于管理非全局装换器
         */
        private ConvertersForPair getMatchableConverters(ConvertiblePair convertiblePair) {
            return this.converters.computeIfAbsent(convertiblePair, k -> new ConvertersForPair());
        }

        /**
         * 根据源TypeDescriptor描述和目标TypeDescriptor描述查找GenericConverter<br/>
         * 尝试通过TypeDescriptor的类和接口层次结构来匹配所有可能的转换器
         *
         * @param sourceType 源TypeDescriptor
         * @param targetType 目标TypeDescriptor
         * @return 匹配的GenericConverter，如果找不到，则返回null
         */
        public GenericConverter find(TypeDescriptor sourceType, TypeDescriptor targetType) {
            // Search the full type hierarchy
            List<Class<?>> sourceCandidates = getClassHierarchy(sourceType.getType());
            List<Class<?>> targetCandidates = getClassHierarchy(targetType.getType());
            for (Class<?> sourceCandidate : sourceCandidates) {
                for (Class<?> targetCandidate : targetCandidates) {
                    ConvertiblePair convertiblePair = new ConvertiblePair(sourceCandidate, targetCandidate);
                    GenericConverter converter = getRegisteredConverter(sourceType, targetType, convertiblePair);
                    if (converter != null) {
                        return converter;
                    }
                }
            }
            return null;
        }

        /**
         * 获取对应的GenericConverter
         *
         * @param sourceType      源TypeDescriptor
         * @param targetType      目标TypeDescriptor
         * @param convertiblePair 转换器缓存Key
         * @return 未找到就返回null
         */
        private GenericConverter getRegisteredConverter(TypeDescriptor sourceType, TypeDescriptor targetType, ConvertiblePair convertiblePair) {
            // Check specifically registered converters
            ConvertersForPair convertersForPair = this.converters.get(convertiblePair);
            if (convertersForPair != null) {
                GenericConverter converter = convertersForPair.getConverter(sourceType, targetType);
                if (converter != null) {
                    return converter;
                }
            }
            // Check ConditionalConverters for a dynamic match
            for (GenericConverter globalConverter : this.globalConverters) {
                if (((ConditionalConverter) globalConverter).matches(sourceType, targetType)) {
                    return globalConverter;
                }
            }
            return null;
        }

        /**
         * 返回给定类型的有序的类型层次结构(就是类型的继承层次结构顺序)
         *
         * @param type Java类型(Class)
         */
        private List<Class<?>> getClassHierarchy(Class<?> type) {
            // 类型的有序层级容器
            List<Class<?>> hierarchy = new ArrayList<>(20);
            // 类型层次结构中所有的类型集合(用于去重)
            Set<Class<?>> visited = new HashSet<>(20);
            // 读取type的父类和实现的接口类
            addToClassHierarchy(0, ClassUtils.resolvePrimitiveIfNecessary(type), false, hierarchy, visited);
            boolean array = type.isArray();
            int i = 0;
            while (i < hierarchy.size()) {
                Class<?> candidate = hierarchy.get(i);
                candidate = (array ? candidate.getComponentType() : ClassUtils.resolvePrimitiveIfNecessary(candidate));
                Class<?> superclass = candidate.getSuperclass();
                if (superclass != null && superclass != Object.class && superclass != Enum.class) {
                    addToClassHierarchy(i + 1, candidate.getSuperclass(), array, hierarchy, visited);
                }
                addInterfacesToClassHierarchy(candidate, array, hierarchy, visited);
                i++;
            }
            // 加入枚举类型
            if (Enum.class.isAssignableFrom(type)) {
                addToClassHierarchy(hierarchy.size(), Enum.class, array, hierarchy, visited);
                addToClassHierarchy(hierarchy.size(), Enum.class, false, hierarchy, visited);
                addInterfacesToClassHierarchy(Enum.class, array, hierarchy, visited);
            }
            // 加入最底层的Object类型
            addToClassHierarchy(hierarchy.size(), Object.class, array, hierarchy, visited);
            addToClassHierarchy(hierarchy.size(), Object.class, false, hierarchy, visited);
            return hierarchy;
        }

        /**
         * 加入类型的所有接口类型到类型的有序层级容器中
         *
         * @param type      Java对象类型
         * @param asArray   是否需要转换为对应的数组类型
         * @param hierarchy 类型的有序层级容器
         * @param visited   类型层次结构中所有的类型集合
         */
        private void addInterfacesToClassHierarchy(Class<?> type, boolean asArray, List<Class<?>> hierarchy, Set<Class<?>> visited) {
            for (Class<?> implementedInterface : type.getInterfaces()) {
                addToClassHierarchy(hierarchy.size(), implementedInterface, asArray, hierarchy, visited);
            }
        }

        /**
         * 加入类型到类型的有序层级容器中
         *
         * @param index     类型的层级位置
         * @param type      Java对象类型
         * @param asArray   是否需要转换为对应的数组类型
         * @param hierarchy 类型的有序层级容器
         * @param visited   类型层次结构中所有的类型集合
         */
        private void addToClassHierarchy(int index, Class<?> type, boolean asArray, List<Class<?>> hierarchy, Set<Class<?>> visited) {
            if (asArray) {
                type = Array.newInstance(type, 0).getClass();
            }
            if (visited.add(type)) {
                hierarchy.add(index, type);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ConversionService converters =\n");
            for (String converterString : getConverterStrings()) {
                builder.append('\t').append(converterString).append('\n');
            }
            return builder.toString();
        }

        private List<String> getConverterStrings() {
            List<String> converterStrings = new ArrayList<>();
            for (ConvertersForPair convertersForPair : this.converters.values()) {
                converterStrings.add(convertersForPair.toString());
            }
            Collections.sort(converterStrings);
            return converterStrings;
        }
    }

    /**
     * 管理基于{@link ConvertiblePair}的转换器
     */
    private static class ConvertersForPair {
        /**
         * 基于GenericConverter的双端队列容器
         */
        private final Deque<GenericConverter> converters = new ConcurrentLinkedDeque<>();

        /**
         * 增加一个GenericConverter到队列的头部
         */
        public void add(GenericConverter converter) {
            this.converters.addFirst(converter);
        }

        /**
         * 根据源TypeDescriptor和目标TypeDescriptor获取符合要求的GenericConverter
         *
         * @return 未找到就返货null
         */
        public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
            for (GenericConverter converter : this.converters) {
                if (!(converter instanceof ConditionalGenericConverter) || ((ConditionalGenericConverter) converter).matches(sourceType, targetType)) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return StringUtils.collectionToCommaDelimitedString(this.converters);
        }
    }

    /**
     * 不执行任何操作的内部转换器
     */
    private static class NoOpConverter implements GenericConverter {
        /**
         * 自定义名称
         */
        private final String name;

        public NoOpConverter(String name) {
            this.name = name;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return null;
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return source;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
