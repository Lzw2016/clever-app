package org.clever.boot.context.properties.bind;

import org.clever.beans.PropertyEditorRegistry;
import org.clever.boot.context.properties.bind.Bindable.BindRestriction;
import org.clever.boot.context.properties.source.*;
import org.clever.boot.convert.ApplicationConversionService;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.ConverterNotFoundException;
import org.clever.core.env.Environment;
import org.clever.format.support.DefaultFormattingConversionService;
import org.clever.util.Assert;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 绑定来自一个或多个 {@link ConfigurationPropertySource ConfigurationPropertySources} 的对象的容器对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:33 <br/>
 */
public class Binder {
    private static final Set<Class<?>> NON_BEAN_CLASSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(Object.class, Class.class))
    );

    // private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Iterable<ConfigurationPropertySource> sources;
    private final PlaceholdersResolver placeholdersResolver;
    private final BindConverter bindConverter;
    private final BindHandler defaultBindHandler;
    private final List<DataObjectBinder> dataObjectBinders;

    /**
     * 为指定的源创建新的 {@link Binder} 实例。
     * {@link DefaultFormattingConversionService} 将用于所有转换。
     *
     * @param sources 用于绑定的源
     */
    public Binder(ConfigurationPropertySource... sources) {
        this((sources != null) ? Arrays.asList(sources) : null, null, null, null);
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例。
     * {@link DefaultFormattingConversionService} 将用于所有转换。
     *
     * @param sources 用于绑定的源
     */
    public Binder(Iterable<ConfigurationPropertySource> sources) {
        this(sources, null, null, null);
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例
     *
     * @param sources              用于绑定的源
     * @param placeholdersResolver 解决任何属性占位符的策略
     */
    public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver) {
        this(sources, placeholdersResolver, null, null);
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例
     *
     * @param sources              用于绑定的源
     * @param placeholdersResolver 解决任何属性占位符的策略
     * @param conversionService    用于转换值的转换服务(或null以使用 {@link ApplicationConversionService})
     */
    public Binder(Iterable<ConfigurationPropertySource> sources,
                  PlaceholdersResolver placeholdersResolver,
                  ConversionService conversionService) {
        this(sources, placeholdersResolver, conversionService, null);
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例
     *
     * @param sources                   用于绑定的源
     * @param placeholdersResolver      解决任何属性占位符的策略
     * @param conversionService         用于转换值的转换服务 (或null以使用 {@link ApplicationConversionService})
     * @param propertyEditorInitializer 用于配置可以转换值的属性编辑器的初始值设定项（如果不需要初始化，则为null）
     */
    public Binder(Iterable<ConfigurationPropertySource> sources,
                  PlaceholdersResolver placeholdersResolver,
                  ConversionService conversionService,
                  Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
        this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, null);
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例
     *
     * @param sources                   用于绑定的源
     * @param placeholdersResolver      解决任何属性占位符的策略
     * @param conversionService         用于转换值的转换服务 (或null以使用 {@link ApplicationConversionService})
     * @param propertyEditorInitializer 用于配置可以转换值的属性编辑器的初始值设定项（如果不需要初始化，则为null）
     * @param defaultBindHandler        如果绑定时未指定任何绑定处理程序，则使用默认的绑定处理程序
     */
    public Binder(Iterable<ConfigurationPropertySource> sources,
                  PlaceholdersResolver placeholdersResolver,
                  ConversionService conversionService,
                  Consumer<PropertyEditorRegistry> propertyEditorInitializer,
                  BindHandler defaultBindHandler) {
        this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, defaultBindHandler, null);
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例
     *
     * @param sources                   用于绑定的源
     * @param placeholdersResolver      解决任何属性占位符的策略
     * @param conversionService         用于转换值的转换服务 (或null以使用 {@link ApplicationConversionService})
     * @param propertyEditorInitializer 用于配置可以转换值的属性编辑器的初始值设定项（如果不需要初始化，则为null）
     * @param defaultBindHandler        如果绑定时未指定任何绑定处理程序，则使用默认的绑定处理程序
     * @param constructorProvider       提供绑定构造函数以在绑定时使用的构造函数提供程序
     */
    public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver,
                  ConversionService conversionService, Consumer<PropertyEditorRegistry> propertyEditorInitializer,
                  BindHandler defaultBindHandler, BindConstructorProvider constructorProvider) {
        this(
                sources,
                placeholdersResolver,
                (conversionService != null) ? Collections.singletonList(conversionService) : null,
                propertyEditorInitializer,
                defaultBindHandler,
                constructorProvider
        );
    }

    /**
     * 为指定的源创建新的 {@link Binder} 实例
     *
     * @param sources                   用于绑定的源
     * @param placeholdersResolver      解决任何属性占位符的策略
     * @param conversionServices        转换服务用于转换值 (或null以使用 {@link ApplicationConversionService})
     * @param propertyEditorInitializer 用于配置可以转换值的属性编辑器的初始值设定项（如果不需要初始化，则为null）。
     * @param defaultBindHandler        如果绑定时未指定任何绑定处理程序，则使用默认的绑定处理程序
     * @param constructorProvider       提供绑定构造函数以在绑定时使用的构造函数提供程序
     */
    public Binder(Iterable<ConfigurationPropertySource> sources,
                  PlaceholdersResolver placeholdersResolver,
                  List<ConversionService> conversionServices,
                  Consumer<PropertyEditorRegistry> propertyEditorInitializer,
                  BindHandler defaultBindHandler,
                  BindConstructorProvider constructorProvider) {
        Assert.notNull(sources, "Sources must not be null");
        for (ConfigurationPropertySource source : sources) {
            Assert.notNull(source, "Sources must not contain null elements");
        }
        this.sources = sources;
        this.placeholdersResolver = (placeholdersResolver != null) ? placeholdersResolver : PlaceholdersResolver.NONE;
        this.bindConverter = BindConverter.get(conversionServices, propertyEditorInitializer);
        this.defaultBindHandler = (defaultBindHandler != null) ? defaultBindHandler : BindHandler.DEFAULT;
        if (constructorProvider == null) {
            constructorProvider = BindConstructorProvider.DEFAULT;
        }
        ValueObjectBinder valueObjectBinder = new ValueObjectBinder(constructorProvider);
        JavaBeanBinder javaBeanBinder = JavaBeanBinder.INSTANCE;
        this.dataObjectBinders = Collections.unmodifiableList(Arrays.asList(valueObjectBinder, javaBeanBinder));
    }

    /**
     * 使用此绑定器的{@link ConfigurationPropertySource 属性源}绑定指定的目标{@link Class}
     *
     * @param name   要绑定的配置属性名称
     * @param target 目标阶层
     * @param <T>    绑定类型
     * @return 绑定结果（从不为null）
     * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
     */
    public <T> BindResult<T> bind(String name, Class<T> target) {
        return bind(name, Bindable.of(target));
    }

    /**
     * 使用此{@link Bindable}的属性{@link ConfigurationPropertySource 源绑定}指定的可绑定目标。
     *
     * @param name   要绑定的配置属性名称
     * @param target 目标可绑定
     * @param <T>    绑定类型
     * @return 绑定结果（从不为null）
     * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
     */
    public <T> BindResult<T> bind(String name, Bindable<T> target) {
        return bind(ConfigurationPropertyName.of(name), target, null);
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}
     *
     * @param name   要绑定的配置属性名称
     * @param target 目标可绑定
     * @param <T>    绑定类型
     * @return 绑定结果（从不为null）
     * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
     */
    public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target) {
        return bind(name, target, null);
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}
     *
     * @param name    要绑定的配置属性名称
     * @param target  目标可绑定
     * @param handler 绑定处理程序（可能为空）
     * @param <T>     绑定类型
     * @return 绑定结果（从不为null）
     */
    public <T> BindResult<T> bind(String name, Bindable<T> target, BindHandler handler) {
        return bind(ConfigurationPropertyName.of(name), target, handler);
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}
     *
     * @param name    要绑定的配置属性名称
     * @param target  目标可绑定
     * @param handler 绑定处理程序（可能为空）
     * @param <T>     绑定类型
     * @return 绑定结果（从不为null）
     */
    public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler) {
        T bound = bind(name, target, handler, false);
        return BindResult.of(bound);
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}，
     * 或者如果绑定结果为空，则使用 {@link Bindable} 类型创建新实例。
     *
     * @param name   要绑定的配置属性名称
     * @param target 目标阶层
     * @param <T>    绑定类型
     * @return 绑定或创建的对象
     * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
     */
    public <T> T bindOrCreate(String name, Class<T> target) {
        return bindOrCreate(name, Bindable.of(target));
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}，
     * 或者如果绑定结果为空，则使用 {@link Bindable} 类型创建新实例。
     *
     * @param name   要绑定的配置属性名称
     * @param target 目标可绑定
     * @param <T>    绑定类型
     * @return 绑定或创建的对象
     * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
     */
    public <T> T bindOrCreate(String name, Bindable<T> target) {
        return bindOrCreate(ConfigurationPropertyName.of(name), target, null);
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}，
     * 或者如果绑定结果为空，则使用 {@link Bindable} 类型创建新实例。
     *
     * @param name    要绑定的配置属性名称
     * @param target  目标可绑定
     * @param handler 绑定处理程序
     * @param <T>     绑定类型
     * @return 绑定或创建的对象
     * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
     */
    public <T> T bindOrCreate(String name, Bindable<T> target, BindHandler handler) {
        return bindOrCreate(ConfigurationPropertyName.of(name), target, handler);
    }

    /**
     * 使用此绑定器的 {@link ConfigurationPropertySource 属性源} 绑定指定的目标 {@link Bindable}，
     * 或者如果绑定结果为空，则使用 {@link Bindable} 类型创建新实例。
     *
     * @param name    要绑定的配置属性名称
     * @param target  目标可绑定
     * @param handler 绑定处理程序（可能为空）
     * @param <T>     绑定或创建的类型
     * @return 绑定或创建的对象
     */
    public <T> T bindOrCreate(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler) {
        return bind(name, target, handler, true);
    }

    private <T> T bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler, boolean create) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(target, "Target must not be null");
        handler = (handler != null) ? handler : this.defaultBindHandler;
        Context context = new Context();
        return bind(name, target, handler, context, false, create);
    }

    private <T> T bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler, Context context, boolean allowRecursiveBinding, boolean create) {
        try {
            Bindable<T> replacementTarget = handler.onStart(name, target, context);
            if (replacementTarget == null) {
                return handleBindResult(name, target, handler, context, null, create);
            }
            target = replacementTarget;
            Object bound = bindObject(name, target, handler, context, allowRecursiveBinding);
            return handleBindResult(name, target, handler, context, bound, create);
        } catch (Exception ex) {
            return handleBindError(name, target, handler, context, ex);
        }
    }

    private <T> T handleBindResult(ConfigurationPropertyName name,
                                   Bindable<T> target,
                                   BindHandler handler,
                                   Context context,
                                   Object result,
                                   boolean create) throws Exception {
        if (result != null) {
            result = handler.onSuccess(name, target, context, result);
            result = context.getConverter().convert(result, target);
        }
        if (result == null && create) {
            result = create(target, context);
            result = handler.onCreate(name, target, context, result);
            result = context.getConverter().convert(result, target);
            Assert.state(result != null, () -> "Unable to create instance for " + target.getType());
        }
        handler.onFinish(name, target, context, result);
        return context.getConverter().convert(result, target);
    }

    private Object create(Bindable<?> target, Context context) {
        for (DataObjectBinder dataObjectBinder : this.dataObjectBinders) {
            Object instance = dataObjectBinder.create(target, context);
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    private <T> T handleBindError(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler, Context context, Exception error) {
        try {
            Object result = handler.onFailure(name, target, context, error);
            return context.getConverter().convert(result, target);
        } catch (Exception ex) {
            if (ex instanceof BindException) {
                throw (BindException) ex;
            }
            throw new BindException(name, target, context.getConfigurationProperty(), ex);
        }
    }

    private <T> Object bindObject(ConfigurationPropertyName name,
                                  Bindable<T> target,
                                  BindHandler handler,
                                  Context context,
                                  boolean allowRecursiveBinding) {
        ConfigurationProperty property = findProperty(name, target, context);
        if (property == null && context.depth != 0 && containsNoDescendantOf(context.getSources(), name)) {
            return null;
        }
        AggregateBinder<?> aggregateBinder = getAggregateBinder(target, context);
        if (aggregateBinder != null) {
            return bindAggregate(name, target, handler, context, aggregateBinder);
        }
        if (property != null) {
            try {
                return bindProperty(target, context, property);
            } catch (ConverterNotFoundException ex) {
                // We might still be able to bind it using the recursive binders
                Object instance = bindDataObject(name, target, handler, context, allowRecursiveBinding);
                if (instance != null) {
                    return instance;
                }
                throw ex;
            }
        }
        return bindDataObject(name, target, handler, context, allowRecursiveBinding);
    }

    private AggregateBinder<?> getAggregateBinder(Bindable<?> target, Context context) {
        Class<?> resolvedType = target.getType().resolve(Object.class);
        if (Map.class.isAssignableFrom(resolvedType)) {
            return new MapBinder(context);
        }
        if (Collection.class.isAssignableFrom(resolvedType)) {
            return new CollectionBinder(context);
        }
        if (target.getType().isArray()) {
            return new ArrayBinder(context);
        }
        return null;
    }

    private <T> Object bindAggregate(ConfigurationPropertyName name,
                                     Bindable<T> target,
                                     BindHandler handler,
                                     Context context,
                                     AggregateBinder<?> aggregateBinder) {
        AggregateElementBinder elementBinder = (itemName, itemTarget, source) -> {
            boolean allowRecursiveBinding = aggregateBinder.isAllowRecursiveBinding(source);
            Supplier<?> supplier = () -> bind(itemName, itemTarget, handler, context, allowRecursiveBinding, false);
            return context.withSource(source, supplier);
        };
        return context.withIncreasedDepth(() -> aggregateBinder.bind(name, target, elementBinder));
    }

    private <T> ConfigurationProperty findProperty(ConfigurationPropertyName name, Bindable<T> target, Context context) {
        if (name.isEmpty() || target.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)) {
            return null;
        }
        for (ConfigurationPropertySource source : context.getSources()) {
            ConfigurationProperty property = source.getConfigurationProperty(name);
            if (property != null) {
                return property;
            }
        }
        return null;
    }

    private <T> Object bindProperty(Bindable<T> target, Context context, ConfigurationProperty property) {
        context.setConfigurationProperty(property);
        Object result = property.getValue();
        result = this.placeholdersResolver.resolvePlaceholders(result);
        result = context.getConverter().convert(result, target);
        return result;
    }

    private Object bindDataObject(ConfigurationPropertyName name,
                                  Bindable<?> target,
                                  BindHandler handler,
                                  Context context,
                                  boolean allowRecursiveBinding) {
        if (isUnbindableBean(name, target, context)) {
            return null;
        }
        Class<?> type = target.getType().resolve(Object.class);
        if (!allowRecursiveBinding && context.isBindingDataObject(type)) {
            return null;
        }
        DataObjectPropertyBinder propertyBinder = (propertyName, propertyTarget) -> bind(
                name.append(propertyName), propertyTarget, handler, context, false, false
        );
        return context.withDataObject(type, () -> {
            for (DataObjectBinder dataObjectBinder : this.dataObjectBinders) {
                Object instance = dataObjectBinder.bind(name, target, context, propertyBinder);
                if (instance != null) {
                    return instance;
                }
            }
            return null;
        });
    }

    private boolean isUnbindableBean(ConfigurationPropertyName name, Bindable<?> target, Context context) {
        for (ConfigurationPropertySource source : context.getSources()) {
            if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
                // We know there are properties to bind so we can't bypass anything
                return false;
            }
        }
        Class<?> resolved = target.getType().resolve(Object.class);
        if (resolved.isPrimitive() || NON_BEAN_CLASSES.contains(resolved)) {
            return true;
        }
        return resolved.getName().startsWith("java.");
    }

    private boolean containsNoDescendantOf(Iterable<ConfigurationPropertySource> sources, ConfigurationPropertyName name) {
        for (ConfigurationPropertySource source : sources) {
            if (source.containsDescendantOf(name) != ConfigurationPropertyState.ABSENT) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从指定的环境创建新的 {@link Binder} 实例
     *
     * @param environment 环境来源 (必须已附加 {@link ConfigurationPropertySources})
     * @return {@link Binder} 对象
     */
    public static Binder get(Environment environment) {
        return get(environment, null);
    }

    /**
     * 从指定的环境创建新的 {@link Binder} 实例
     *
     * @param environment        环境来源 (必须已附加 {@link ConfigurationPropertySources})
     * @param defaultBindHandler 如果绑定时未指定任何绑定处理程序，则使用默认的绑定处理程序
     * @return {@link Binder} 对象
     */
    public static Binder get(Environment environment, BindHandler defaultBindHandler) {
        Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
        PropertySourcesPlaceholdersResolver placeholdersResolver = new PropertySourcesPlaceholdersResolver(environment);
        return new Binder(sources, placeholdersResolver, null, null, defaultBindHandler);
    }

    /**
     * 绑定和 {@link BindContext} 实现时使用的上下文
     */
    final class Context implements BindContext {
        private int depth;
        @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
        private final List<ConfigurationPropertySource> source = Arrays.asList((ConfigurationPropertySource) null);
        private int sourcePushCount;
        private final Deque<Class<?>> dataObjectBindings = new ArrayDeque<>();
        private final Deque<Class<?>> constructorBindings = new ArrayDeque<>();
        private ConfigurationProperty configurationProperty;

        private void increaseDepth() {
            this.depth++;
        }

        private void decreaseDepth() {
            this.depth--;
        }

        private <T> T withSource(ConfigurationPropertySource source, Supplier<T> supplier) {
            if (source == null) {
                return supplier.get();
            }
            this.source.set(0, source);
            this.sourcePushCount++;
            try {
                return supplier.get();
            } finally {
                this.sourcePushCount--;
            }
        }

        private <T> T withDataObject(Class<?> type, Supplier<T> supplier) {
            this.dataObjectBindings.push(type);
            try {
                return withIncreasedDepth(supplier);
            } finally {
                this.dataObjectBindings.pop();
            }
        }

        private boolean isBindingDataObject(Class<?> type) {
            return this.dataObjectBindings.contains(type);
        }

        private <T> T withIncreasedDepth(Supplier<T> supplier) {
            increaseDepth();
            try {
                return supplier.get();
            } finally {
                decreaseDepth();
            }
        }

        void setConfigurationProperty(ConfigurationProperty configurationProperty) {
            this.configurationProperty = configurationProperty;
        }

        void clearConfigurationProperty() {
            this.configurationProperty = null;
        }

        void pushConstructorBoundTypes(Class<?> value) {
            this.constructorBindings.push(value);
        }

        boolean isNestedConstructorBinding() {
            return !this.constructorBindings.isEmpty();
        }

        void popConstructorBoundTypes() {
            this.constructorBindings.pop();
        }

        PlaceholdersResolver getPlaceholdersResolver() {
            return Binder.this.placeholdersResolver;
        }

        BindConverter getConverter() {
            return Binder.this.bindConverter;
        }

        @Override
        public Binder getBinder() {
            return Binder.this;
        }

        @Override
        public int getDepth() {
            return this.depth;
        }

        @Override
        public Iterable<ConfigurationPropertySource> getSources() {
            if (this.sourcePushCount > 0) {
                return this.source;
            }
            return Binder.this.sources;
        }

        @Override
        public ConfigurationProperty getConfigurationProperty() {
            return this.configurationProperty;
        }
    }
}
