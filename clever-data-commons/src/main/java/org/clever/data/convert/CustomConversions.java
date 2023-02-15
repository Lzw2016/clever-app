package org.clever.data.convert;

import org.clever.core.GenericTypeResolver;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.core.convert.converter.ConverterRegistry;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.core.convert.converter.GenericConverter.ConvertiblePair;
import org.clever.core.convert.support.GenericConversionService;
import org.clever.data.convert.ConverterBuilder.ConverterAware;
import org.clever.data.mapping.model.SimpleTypeHolder;
import org.clever.data.util.Streamable;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 用于捕获自定义转换的值对象。
 * 这本质上是转换器的 {@link List} 和围绕它们的一些附加逻辑。
 * 转换器构建了两组类型，可以将特定于商店的基本类型转换成和从中转换。
 * 这些类型将被视为简单类型 (这意味着它们既不需要更深入的检查也不需要嵌套转换) 。
 * 因此 {@link CustomConversions} 也充当 {@link SimpleTypeHolder} 的工厂。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:08 <br/>
 */
public class CustomConversions {
    private static final Logger logger = LoggerFactory.getLogger(CustomConversions.class);
    private static final String READ_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as reading converter although it doesn't convert from a store-supported type; You might want to check your annotation setup at the converter implementation";
    private static final String WRITE_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as writing converter although it doesn't convert to a store-supported type; You might want to check your annotation setup at the converter implementation";
    private static final String NOT_A_CONVERTER = "Converter %s is neither a Spring Converter, GenericConverter or ConverterFactory";
    private static final String CONVERTER_FILTER = "converter from %s to %s as %s converter";
    private static final String ADD_CONVERTER = "Adding %s" + CONVERTER_FILTER;
    private static final String SKIP_CONVERTER = "Skipping " + CONVERTER_FILTER + " %s is not a store supported simple type";
    private static final List<Object> DEFAULT_CONVERTERS;

    static {
        List<Object> defaults = new ArrayList<>();
        defaults.addAll(JodaTimeConverters.getConvertersToRegister());
        defaults.addAll(Jsr310Converters.getConvertersToRegister());
        // defaults.addAll(ThreeTenBackPortConverters.getConvertersToRegister());
        // defaults.addAll(JMoleculesConverters.getConvertersToRegister());
        DEFAULT_CONVERTERS = Collections.unmodifiableList(defaults);
    }

    private final SimpleTypeHolder simpleTypeHolder;
    private final List<Object> converters;
    private final Set<ConvertiblePair> readingPairs = new LinkedHashSet<>();
    private final Set<ConvertiblePair> writingPairs = new LinkedHashSet<>();
    private final Set<Class<?>> customSimpleTypes = new HashSet<>();
    private final ConversionTargetsCache customReadTargetTypes = new ConversionTargetsCache();
    private final ConversionTargetsCache customWriteTargetTypes = new ConversionTargetsCache();
    private final ConverterConfiguration converterConfiguration;
    private final Function<ConvertiblePair, Class<?>> getReadTarget = convertiblePair -> getCustomTarget(convertiblePair.getSourceType(), convertiblePair.getTargetType(), readingPairs);
    private final Function<ConvertiblePair, Class<?>> getWriteTarget = convertiblePair -> getCustomTarget(convertiblePair.getSourceType(), convertiblePair.getTargetType(), writingPairs);
    private final Function<ConvertiblePair, Class<?>> getRawWriteTarget = convertiblePair -> getCustomTarget(convertiblePair.getSourceType(), null, writingPairs);

    /**
     * @param converterConfiguration the {@link ConverterConfiguration} to apply.
     */
    public CustomConversions(ConverterConfiguration converterConfiguration) {
        this.converterConfiguration = converterConfiguration;
        List<Object> registeredConverters = collectPotentialConverterRegistrations(
                converterConfiguration.getStoreConversions(), converterConfiguration.getUserConverters()).stream()
                .filter(this::isSupportedConverter)
                .filter(this::shouldRegister)
                .map(ConverterRegistrationIntent::getConverterRegistration)
                .map(this::register)
                .distinct()
                .collect(Collectors.toList());
        Collections.reverse(registeredConverters);
        this.converters = Collections.unmodifiableList(registeredConverters);
        this.simpleTypeHolder = new SimpleTypeHolder(
                customSimpleTypes,
                converterConfiguration.getStoreConversions().getStoreTypeHolder()
        );
    }

    /**
     * 创建一个新的 {@link CustomConversions} 实例，注册所有给定的用户定义的转换器，
     * 并根据 {@link StoreConversions#getSimpleTypeHolder() store simple types}
     * 从 {@link StoreConversions} 选择 {@link Converter converters} 只考虑那些从商店转换为支持的类型。
     *
     * @param storeConversions 不能为 {@literal null}
     * @param converters       不能为 {@literal null}
     */
    public CustomConversions(StoreConversions storeConversions, Collection<?> converters) {
        this(new ConverterConfiguration(storeConversions, new ArrayList<>(converters)));
    }

    /**
     * 返回基础 {@link SimpleTypeHolder}
     */
    public SimpleTypeHolder getSimpleTypeHolder() {
        return simpleTypeHolder;
    }

    /**
     * 返回给定类型是否被认为是简单的。这意味着它要么是一般的简单类型，要么我们有一个为特定类型注册的写作 {@link Converter}
     *
     * @see SimpleTypeHolder#isSimpleType(Class)
     */
    public boolean isSimpleType(Class<?> type) {
        Assert.notNull(type, "Type must not be null");
        return simpleTypeHolder.isSimpleType(type);
    }

    /**
     * 使用注册的转换器填充给定的 {@link GenericConversionService}
     */
    public void registerConvertersIn(ConverterRegistry conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        converters.forEach(it -> registerConverterIn(it, conversionService));
    }

    /**
     * 获取所有转换器并添加来源信息
     */
    private List<ConverterRegistrationIntent> collectPotentialConverterRegistrations(StoreConversions storeConversions, Collection<?> converters) {
        List<ConverterRegistrationIntent> converterRegistrations = new ArrayList<>();
        converters.stream()
                .map(storeConversions::getRegistrationsFor)
                .flatMap(Streamable::stream)
                .map(ConverterRegistrationIntent::userConverters)
                .forEach(converterRegistrations::add);
        storeConversions.getStoreConverters().stream()
                .map(storeConversions::getRegistrationsFor)
                .flatMap(Streamable::stream)
                .map(ConverterRegistrationIntent::storeConverters)
                .forEach(converterRegistrations::add);
        DEFAULT_CONVERTERS.stream()
                .map(storeConversions::getRegistrationsFor)
                .flatMap(Streamable::stream)
                .map(ConverterRegistrationIntent::defaultConverters)
                .forEach(converterRegistrations::add);
        return converterRegistrations;
    }

    /**
     * 在给定的 {@link GenericConversionService} 中注册给定的转换器
     *
     * @param candidate         不能为 {@literal null}
     * @param conversionService 不能为 {@literal null}
     */
    @SuppressWarnings("rawtypes")
    private void registerConverterIn(Object candidate, ConverterRegistry conversionService) {
        if (candidate instanceof Converter) {
            conversionService.addConverter((Converter) candidate);
            return;
        }
        if (candidate instanceof ConverterFactory) {
            conversionService.addConverterFactory((ConverterFactory) candidate);
            return;
        }
        if (candidate instanceof GenericConverter) {
            conversionService.addConverter((GenericConverter) candidate);
            return;
        }
        if (candidate instanceof ConverterAware) {
            ((ConverterAware) candidate).getConverters().forEach(it -> registerConverterIn(it, conversionService));
            return;
        }
        throw new IllegalArgumentException(String.format(NOT_A_CONVERTER, candidate));
    }

    /**
     * 将给定的 {@link ConvertiblePair} 注册为读或写对，具体取决于类型边是基本类型
     */
    private Object register(ConverterRegistration converterRegistration) {
        Assert.notNull(converterRegistration, "Converter registration must not be null");
        ConvertiblePair pair = converterRegistration.getConvertiblePair();
        if (converterRegistration.isReading()) {
            readingPairs.add(pair);
            if (logger.isWarnEnabled() && !converterRegistration.isSimpleSourceType()) {
                logger.warn(String.format(READ_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
            }
        }
        if (converterRegistration.isWriting()) {
            writingPairs.add(pair);
            customSimpleTypes.add(pair.getSourceType());
            if (logger.isWarnEnabled() && !converterRegistration.isSimpleTargetType()) {
                logger.warn(String.format(WRITE_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
            }
        }
        return converterRegistration.getConverter();
    }

    /**
     * 在特定设置中验证给定的 {@link ConverterRegistration}。<br />
     * 仅当 {@link ConverterRegistrationIntent#isSimpleTargetType() 目标类型} 被认为是存储简单类型时，
     * 才认为非 {@link ReadingConverter reading} 和用户定义的 {@link Converter 转换器} 被支持。
     *
     * @return {@literal true} 如果支持
     */
    private boolean isSupportedConverter(ConverterRegistrationIntent registrationIntent) {
        boolean register = registrationIntent.isUserConverter()
                || registrationIntent.isStoreConverter()
                || (registrationIntent.isReading() && registrationIntent.isSimpleSourceType())
                || (registrationIntent.isWriting() && registrationIntent.isSimpleTargetType());
        if (logger.isDebugEnabled()) {
            if (register) {
                logger.debug(String.format(
                        ADD_CONVERTER,
                        registrationIntent.isUserConverter() ? "user defined " : "",
                        registrationIntent.getSourceType(),
                        registrationIntent.getTargetType(),
                        registrationIntent.isReading() ? "reading" : "writing"
                ));
            } else {
                logger.debug(String.format(
                        SKIP_CONVERTER,
                        registrationIntent.getSourceType(),
                        registrationIntent.getTargetType(),
                        registrationIntent.isReading() ? "reading" : "writing",
                        registrationIntent.isReading() ? registrationIntent.getSourceType() : registrationIntent.getTargetType()
                ));
            }
        }
        return register;
    }

    /**
     * @param intent 不能为 {@literal null}
     * @return {@literal false} 如果给定的 {@link ConverterRegistration} 应该被跳过
     */
    private boolean shouldRegister(ConverterRegistrationIntent intent) {
        return !intent.isDefaultConverter() || converterConfiguration.shouldRegister(intent.getConverterRegistration().getConvertiblePair());
    }

    /**
     * 返回要转换为的目标类型，以防我们注册了自定义转换以将给定源类型转换为存储本机类型
     *
     * @param sourceType 不能为 {@literal null}
     */
    public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
        Assert.notNull(sourceType, "Source type must not be null");
        Class<?> target = customWriteTargetTypes.computeIfAbsent(sourceType, getRawWriteTarget);
        return Void.class.equals(target) || target == null ? Optional.empty() : Optional.of(target);
    }

    /**
     * 返回我们可以读取给定源类型注入的目标类型。
     * 不过，返回的类型可能是给定预期类型的子类。
     * 如果 {@code requestedTargetType} 是 {@literal null} 我们将简单地返回第一个目标类型匹配或 {@literal null} 如果找不到转换。
     *
     * @param sourceType          不能为 {@literal null}
     * @param requestedTargetType 不能为 {@literal null}
     */
    public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType, Class<?> requestedTargetType) {
        Assert.notNull(sourceType, "Source type must not be null");
        Assert.notNull(requestedTargetType, "Target type must not be null");
        Class<?> target = customWriteTargetTypes.computeIfAbsent(sourceType, requestedTargetType, getWriteTarget);
        return Void.class.equals(target) || target == null ? Optional.empty() : Optional.of(target);
    }

    /**
     * 返回我们是否注册了自定义转换以将 {@code sourceType} 读入本机类型。
     * 不过，返回的类型可能是给定预期类型的子类。
     *
     * @param sourceType 不能为 {@literal null}
     */
    public boolean hasCustomWriteTarget(Class<?> sourceType) {
        Assert.notNull(sourceType, "Source type must not be null");
        return getCustomWriteTarget(sourceType).isPresent();
    }

    /**
     * 返回我们是否注册了自定义转换以将给定源类型的对象读入给定本机目标类型的对象
     *
     * @param sourceType 不能为 {@literal null}
     * @param targetType 不能为 {@literal null}
     */
    public boolean hasCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {
        Assert.notNull(sourceType, "Source type must not be null");
        Assert.notNull(targetType, "Target type must not be null");
        return getCustomWriteTarget(sourceType, targetType).isPresent();
    }

    /**
     * 返回我们是否注册了自定义转换以将给定源读入给定目标类型
     *
     * @param sourceType 不能为 {@literal null}
     * @param targetType 不能为 {@literal null}
     */
    public boolean hasCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
        Assert.notNull(sourceType, "Source type must not be null");
        Assert.notNull(targetType, "Target type must not be null");
        return getCustomReadTarget(sourceType, targetType) != null;
    }

    /**
     * 返回给定 {@code sourceType} 和 {@code targetType} 的实际目标类型。
     * 请注意，返回的 {@link Class} 可以是给定 {@code targetType} 的可分配类型。
     *
     * @param sourceType 不能为 {@literal null}
     * @param targetType 不能为 {@literal null}
     */
    private Class<?> getCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
        return customReadTargetTypes.computeIfAbsent(sourceType, targetType, getReadTarget);
    }

    /**
     * 检查给定的 {@link ConvertiblePair ConvertiblePairs} 是否具有源兼容类型作为源。
     * 如果给定，还检查目标类型的可分配性。
     *
     * @param sourceType 不能为 {@literal null}
     * @param targetType 可以是 {@literal null}
     * @param pairs      不能为 {@literal null}
     */
    private Class<?> getCustomTarget(Class<?> sourceType, Class<?> targetType, Collection<ConvertiblePair> pairs) {
        if (targetType != null && pairs.contains(new ConvertiblePair(sourceType, targetType))) {
            return targetType;
        }
        for (ConvertiblePair pair : pairs) {
            if (!hasAssignableSourceType(pair, sourceType)) {
                continue;
            }
            Class<?> candidate = pair.getTargetType();
            if (!requestedTargetTypeIsAssignable(targetType, candidate)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static boolean hasAssignableSourceType(ConvertiblePair pair, Class<?> sourceType) {
        return pair.getSourceType().isAssignableFrom(sourceType);
    }

    private static boolean requestedTargetTypeIsAssignable(Class<?> requestedTargetType, Class<?> targetType) {
        return requestedTargetType == null || targetType.isAssignableFrom(requestedTargetType);
    }

    /**
     * 用于缓存自定义转换目标的值对象
     */
    static class ConversionTargetsCache {
        private final Map<Class<?>, TargetTypes> customReadTargetTypes = new ConcurrentHashMap<>();

        /**
         * 获取或计算给定其 {@code sourceType} 的目标类型。
         * 如果计算了一次值（present/absent 目标），则返回缓存的 {@link Optional}。
         * 否则，使用 {@link Function mappingFunction} 来确定可能存在的目标类型。
         *
         * @param sourceType      不能为 {@literal null}
         * @param mappingFunction 不能为 {@literal null}
         * @return 可选的目标类型
         */
        public Class<?> computeIfAbsent(Class<?> sourceType, Function<ConvertiblePair, Class<?>> mappingFunction) {
            return computeIfAbsent(sourceType, AbsentTargetTypeMarker.class, mappingFunction);
        }

        /**
         * 获取或计算给定其 {@code sourceType} 和 {@code targetType} 的目标类型。
         * 如果计算了一次值（present/absent 目标），则返回缓存的 {@link Optional}。
         * 否则，使用 {@link Function mappingFunction} 来确定可能存在的目标类型。
         *
         * @param sourceType      不能为 {@literal null}
         * @param targetType      不能为 {@literal null}
         * @param mappingFunction 不能为 {@literal null}
         * @return the optional target type.
         */

        public Class<?> computeIfAbsent(Class<?> sourceType, Class<?> targetType, Function<ConvertiblePair, Class<?>> mappingFunction) {
            TargetTypes targetTypes = customReadTargetTypes.get(sourceType);
            if (targetTypes == null) {
                targetTypes = customReadTargetTypes.computeIfAbsent(sourceType, TargetTypes::new);
            }
            return targetTypes.computeIfAbsent(targetType, mappingFunction);
        }

        /**
         * 缺少目标类型缓存的标记类型
         */
        interface AbsentTargetTypeMarker {
        }
    }

    /**
     * 特定 {@code Class source type} 的值对象以确定可能的目标转换类型
     */
    static class TargetTypes {
        private final Class<?> sourceType;
        private final Map<Class<?>, Class<?>> conversionTargets = new ConcurrentHashMap<>();

        TargetTypes(Class<?> sourceType) {
            this.sourceType = sourceType;
        }

        /**
         * 获取或计算给定其 {@code targetType} 的目标类型。
         * 如果计算了一次值（present/absent 目标），则返回缓存的 {@link Optional}。
         * 否则，使用 {@link Function mappingFunction} 来确定可能存在的目标类型。
         *
         * @param targetType      不能为 {@literal null}
         * @param mappingFunction 不能为 {@literal null}
         * @return 可选的目标类型
         */
        public Class<?> computeIfAbsent(Class<?> targetType, Function<ConvertiblePair, Class<?>> mappingFunction) {
            Class<?> optionalTarget = conversionTargets.get(targetType);
            if (optionalTarget == null) {
                optionalTarget = mappingFunction.apply(new ConvertiblePair(sourceType, targetType));
                conversionTargets.put(targetType, optionalTarget == null ? Void.class : optionalTarget);
            }
            return Void.class.equals(optionalTarget) ? null : optionalTarget;
        }
    }

    /**
     * 值类将 {@link ConverterRegistration} 及其 {@link ConverterOrigin origin} 绑定在一起，以允许基于商店支持的类型进行细粒度注册。
     */
    protected static class ConverterRegistrationIntent {
        private final ConverterRegistration delegate;
        private final ConverterOrigin origin;

        ConverterRegistrationIntent(ConverterRegistration delegate, ConverterOrigin origin) {
            this.delegate = delegate;
            this.origin = origin;
        }

        static ConverterRegistrationIntent userConverters(ConverterRegistration delegate) {
            return new ConverterRegistrationIntent(delegate, ConverterOrigin.USER_DEFINED);
        }

        static ConverterRegistrationIntent storeConverters(ConverterRegistration delegate) {
            return new ConverterRegistrationIntent(delegate, ConverterOrigin.STORE);
        }

        static ConverterRegistrationIntent defaultConverters(ConverterRegistration delegate) {
            return new ConverterRegistrationIntent(delegate, ConverterOrigin.DEFAULT);
        }

        Class<?> getSourceType() {
            return delegate.getConvertiblePair().getSourceType();
        }

        Class<?> getTargetType() {
            return delegate.getConvertiblePair().getTargetType();
        }

        public boolean isWriting() {
            return delegate.isWriting();
        }

        public boolean isReading() {
            return delegate.isReading();
        }

        public boolean isSimpleSourceType() {
            return delegate.isSimpleSourceType();
        }

        public boolean isSimpleTargetType() {
            return delegate.isSimpleTargetType();
        }

        public boolean isUserConverter() {
            return isConverterOfSource(ConverterOrigin.USER_DEFINED);
        }

        public boolean isStoreConverter() {
            return isConverterOfSource(ConverterOrigin.STORE);
        }

        public boolean isDefaultConverter() {
            return isConverterOfSource(ConverterOrigin.DEFAULT);
        }

        public ConverterRegistration getConverterRegistration() {
            return delegate;
        }

        private boolean isConverterOfSource(ConverterOrigin source) {
            return origin.equals(source);
        }

        protected enum ConverterOrigin {
            DEFAULT, USER_DEFINED, STORE
        }
    }

    /**
     * 转换注册信息
     */
    private static class ConverterRegistration {
        private final Object converter;
        private final ConvertiblePair convertiblePair;
        private final StoreConversions storeConversions;
        private final boolean reading;
        private final boolean writing;

        private ConverterRegistration(Object converter, ConvertiblePair convertiblePair, StoreConversions storeConversions, boolean reading, boolean writing) {
            this.converter = converter;
            this.convertiblePair = convertiblePair;
            this.storeConversions = storeConversions;
            this.reading = reading;
            this.writing = writing;
        }

        /**
         * 返回转换器是否用于写入
         */
        public boolean isWriting() {
            return writing || (!reading && isSimpleTargetType());
        }

        /**
         * 返回转换器是否用于读取
         */
        public boolean isReading() {
            return reading || (!writing && isSimpleSourceType());
        }

        /**
         * 返回实际的转换对
         */
        public ConvertiblePair getConvertiblePair() {
            return convertiblePair;
        }

        /**
         * 返回源类型是否为简单类型
         */
        public boolean isSimpleSourceType() {
            return storeConversions.isStoreSimpleType(convertiblePair.getSourceType());
        }

        /**
         * 返回目标类型是否为简单类型
         */
        public boolean isSimpleTargetType() {
            return storeConversions.isStoreSimpleType(convertiblePair.getTargetType());
        }

        Object getConverter() {
            return converter;
        }
    }

    /**
     * 用于捕获 {@link CustomConversions} 的商店特定扩展的值类型。
     * 允许转发存储特定的默认转换和一组应该被认为是简单的类型。
     */
    public static class StoreConversions {
        public static final StoreConversions NONE = StoreConversions.of(SimpleTypeHolder.DEFAULT, Collections.emptyList());

        private final SimpleTypeHolder storeTypeHolder;
        private final Collection<?> storeConverters;

        private StoreConversions(SimpleTypeHolder storeTypeHolder, Collection<?> storeConverters) {
            this.storeTypeHolder = storeTypeHolder;
            this.storeConverters = storeConverters;
        }

        /**
         * 为给定的商店特定 {@link SimpleTypeHolder} 和给定的转换器创建一个新的 {@link StoreConversions}
         *
         * @param storeTypeHolder 不能为 {@literal null}
         * @param converters      不能为 {@literal null}
         */
        public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Object... converters) {
            Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null");
            Assert.notNull(converters, "Converters must not be null");
            return new StoreConversions(storeTypeHolder, Arrays.asList(converters));
        }

        /**
         * 为给定的商店特定 {@link SimpleTypeHolder} 和给定的转换器创建一个新的 {@link StoreConversions}
         *
         * @param storeTypeHolder 不能为 {@literal null}
         * @param converters      不能为 {@literal null}
         */
        public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Collection<?> converters) {
            Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null");
            Assert.notNull(converters, "Converters must not be null");
            return new StoreConversions(storeTypeHolder, converters);
        }

        /**
         * 返回给定转换器的 {@link ConverterRegistration}
         *
         * @param converter 不能为 {@literal null}
         */
        public Streamable<ConverterRegistration> getRegistrationsFor(Object converter) {
            Assert.notNull(converter, "Converter must not be null");
            Class<?> type = converter.getClass();
            boolean isWriting = type.isAnnotationPresent(WritingConverter.class);
            boolean isReading = type.isAnnotationPresent(ReadingConverter.class);
            if (converter instanceof ConverterAware) {
                return Streamable.of(() -> ((ConverterAware) converter).getConverters().stream().flatMap(it -> getRegistrationsFor(it).stream()));
            } else if (converter instanceof GenericConverter) {
                Set<ConvertiblePair> convertibleTypes = ((GenericConverter) converter).getConvertibleTypes();
                return convertibleTypes == null ? Streamable.empty() : Streamable.of(convertibleTypes).map(it -> register(converter, it, isReading, isWriting));
            } else if (converter instanceof ConverterFactory) {
                return getRegistrationFor(converter, ConverterFactory.class, isReading, isWriting);
            } else if (converter instanceof Converter) {
                return getRegistrationFor(converter, Converter.class, isReading, isWriting);
            } else {
                throw new IllegalArgumentException(String.format("Unsupported converter type %s", converter));
            }
        }

        private Streamable<ConverterRegistration> getRegistrationFor(Object converter, Class<?> type, boolean isReading, boolean isWriting) {
            Class<?> converterType = converter.getClass();
            Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(converterType, type);
            if (arguments == null) {
                throw new IllegalStateException(String.format("Couldn't resolve type arguments for %s", converterType));
            }
            return Streamable.of(register(converter, arguments[0], arguments[1], isReading, isWriting));
        }

        private ConverterRegistration register(Object converter, Class<?> source, Class<?> target, boolean isReading, boolean isWriting) {
            return register(converter, new ConvertiblePair(source, target), isReading, isWriting);
        }

        private ConverterRegistration register(Object converter, ConvertiblePair pair, boolean isReading, boolean isWriting) {
            return new ConverterRegistration(converter, pair, this, isReading, isWriting);
        }

        private boolean isStoreSimpleType(Class<?> type) {
            return storeTypeHolder.isSimpleType(type);
        }

        SimpleTypeHolder getStoreTypeHolder() {
            return this.storeTypeHolder;
        }

        Collection<?> getStoreConverters() {
            return this.storeConverters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StoreConversions)) {
                return false;
            }
            StoreConversions that = (StoreConversions) o;
            if (!ObjectUtils.nullSafeEquals(storeTypeHolder, that.storeTypeHolder)) {
                return false;
            }
            return ObjectUtils.nullSafeEquals(storeConverters, that.storeConverters);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(storeTypeHolder);
            result = 31 * result + ObjectUtils.nullSafeHashCode(storeConverters);
            return result;
        }

        @Override
        public String toString() {
            return "StoreConversions{" + "storeTypeHolder=" + storeTypeHolder + ", storeConverters=" + storeConverters + '}';
        }
    }

    /**
     * 值对象包含为注册配置的实际 {@link StoreConversions} 和自定义 {@link StoreConversions}
     */
    protected static class ConverterConfiguration {
        private final StoreConversions storeConversions;
        private final List<?> userConverters;
        private final Predicate<ConvertiblePair> converterRegistrationFilter;

        /**
         * 创建一个新的转换器配置，其中包含给定的 {@link StoreConversions} 和用户定义的转换器。
         *
         * @param storeConversions 不能为 {@literal null}
         * @param userConverters   不能为 {@literal null} 使用 {@link Collections#emptyList()}
         */
        public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters) {
            this(storeConversions, userConverters, it -> true);
        }

        /**
         * 创建一个新的转换器配置，其中包含给定的 {@link StoreConversions} 和用户定义的转换器，
         * 以及 {@link ConvertiblePair} 的 {@link ConvertiblePair} 集合，以跳过默认转换器的注册。
         * <br />
         * 这允许商店实现根据特定需求和配置修改默认转换器注册。用户定义的转换器永远不会被过滤。
         *
         * @param storeConversions            不能为 {@literal null}
         * @param userConverters              不能为 {@literal null} 使用 {@link Collections#emptyList()}
         * @param converterRegistrationFilter 不能为 {@literal null}
         */
        public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters, Predicate<ConvertiblePair> converterRegistrationFilter) {
            this.storeConversions = storeConversions;
            this.userConverters = new ArrayList<>(userConverters);
            this.converterRegistrationFilter = converterRegistrationFilter;
        }

        /**
         * @return 从不为 {@literal null}
         */
        StoreConversions getStoreConversions() {
            return storeConversions;
        }

        /**
         * @return 从不为 {@literal null}
         */
        List<?> getUserConverters() {
            return userConverters;
        }

        /**
         * @return 从不为 {@literal null}
         */
        boolean shouldRegister(ConvertiblePair candidate) {
            return this.converterRegistrationFilter.test(candidate);
        }
    }
}