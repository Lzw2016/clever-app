package org.clever.boot.context.config;

import org.clever.core.env.Environment;
import org.clever.core.env.PropertySource;
import org.clever.util.Assert;

import java.util.*;
import java.util.function.Consumer;

/**
 * 从 {@link ConfigDataResource} 加载的配置数据，可能最终为 {@link Environment} 贡献(contribute) {@link PropertySource 属性源}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:11 <br/>
 *
 * @see ConfigDataLocationResolver
 * @see ConfigDataLoader
 */
public final class ConfigData {
    /**
     * 不包含数据的 {@link ConfigData} 实例
     */
    public static final ConfigData EMPTY = new ConfigData(Collections.emptySet());

    private final List<PropertySource<?>> propertySources;
    private final PropertySourceOptions propertySourceOptions;

    /**
     * 使用应用于每个源的相同选项创建新的 {@link ConfigData} 实例
     *
     * @param propertySources config data属性按优先级升序显示源
     * @param options         应用于每个源的配置数据选项
     * @see #ConfigData(Collection, PropertySourceOptions)
     */
    public ConfigData(Collection<? extends PropertySource<?>> propertySources, Option... options) {
        this(propertySources, PropertySourceOptions.always(Options.of(options)));
    }

    /**
     * 使用特定的属性源选项创建新的 {@link ConfigData} 实例
     *
     * @param propertySources       config data属性按优先级升序显示源
     * @param propertySourceOptions 属性源选项
     */
    public ConfigData(Collection<? extends PropertySource<?>> propertySources, PropertySourceOptions propertySourceOptions) {
        Assert.notNull(propertySources, "PropertySources must not be null");
        Assert.notNull(propertySourceOptions, "PropertySourceOptions must not be null");
        this.propertySources = Collections.unmodifiableList(new ArrayList<>(propertySources));
        this.propertySourceOptions = propertySourceOptions;
    }

    /**
     * 按优先级升序返回配置数据属性源。如果同一密钥包含在多个源中，则后一个源将获胜。
     *
     * @return 配置数据属性源
     */
    public List<PropertySource<?>> getPropertySources() {
        return this.propertySources;
    }

    /**
     * 返回应用于给定源的 {@link Options 配置数据选项}
     *
     * @param propertySource 要检查的属性源
     * @return 适用的选项
     */
    public Options getOptions(PropertySource<?> propertySource) {
        Options options = this.propertySourceOptions.get(propertySource);
        return (options != null) ? options : Options.NONE;
    }

    /**
     * 用于为给定 {@link PropertySource} 提供 {@link Options} 的策略接口。
     */
    @FunctionalInterface
    public interface PropertySourceOptions {
        /**
         * {@link PropertySourceOptions} 始终返回的实例 {@link Options#NONE}.
         */
        PropertySourceOptions ALWAYS_NONE = new AlwaysPropertySourceOptions(Options.NONE);

        /**
         * 返回应应用于给定属性源的选项
         *
         * @param propertySource property source
         * @return 要应用的选项
         */
        Options get(PropertySource<?> propertySource);

        /**
         * 创建一个新的 {@link PropertySourceOptions} 实例，无论属性源如何，该实例始终返回相同的选项。
         *
         * @param options 返回的选项
         * @return 一个新的 {@link PropertySourceOptions} 实例
         */
        static PropertySourceOptions always(Option... options) {
            return always(Options.of(options));
        }

        /**
         * 创建一个新的 {@link PropertySourceOptions} 实例，无论属性源如何，该实例始终返回相同的选项。
         *
         * @param options 返回的选项
         * @return 一个新的 {@link PropertySourceOptions} 实例
         */
        static PropertySourceOptions always(Options options) {
            if (options == Options.NONE) {
                return ALWAYS_NONE;
            }
            return new AlwaysPropertySourceOptions(options);
        }
    }

    /**
     * 总是返回相同结果的 {@link PropertySourceOptions}
     */
    private static class AlwaysPropertySourceOptions implements PropertySourceOptions {
        private final Options options;

        AlwaysPropertySourceOptions(Options options) {
            this.options = options;
        }

        @Override
        public Options get(PropertySource<?> propertySource) {
            return this.options;
        }
    }

    /**
     * 一组 {@link Option} 标志
     */
    public static final class Options {
        /**
         * 没有选择
         */
        public static final Options NONE = new Options(Collections.emptySet());

        private final Set<Option> options;

        private Options(Set<Option> options) {
            this.options = Collections.unmodifiableSet(options);
        }

        Set<Option> asSet() {
            return this.options;
        }

        /**
         * 如果给定选项包含在此集中，则返回。
         *
         * @param option 检查选项
         * @return 选项为true
         */
        public boolean contains(Option option) {
            return this.options.contains(option);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Options other = (Options) obj;
            return this.options.equals(other.options);
        }

        @Override
        public int hashCode() {
            return this.options.hashCode();
        }

        @Override
        public String toString() {
            return this.options.toString();
        }

        /**
         * 创建一个新的 {@link Options} 实例，该实例包含该集中的选项，不包括给定的选项。
         *
         * @param option 排除选项
         * @return 一个新的 {@link Options} 实例
         */
        public Options without(Option option) {
            return copy((options) -> options.remove(option));
        }

        /**
         * 创建一个新的 {@link Options} 实例，该实例包含该集中的选项，包括给定的选项
         *
         * @param option 包括的选项
         * @return 一个新的 {@link Options} 实例
         */
        public Options with(Option option) {
            return copy((options) -> options.add(option));
        }

        private Options copy(Consumer<EnumSet<Option>> processor) {
            EnumSet<Option> options = EnumSet.noneOf(Option.class);
            options.addAll(this.options);
            processor.accept(options);
            return new Options(options);
        }

        /**
         * 一组 {@link Option} 标志
         *
         * @param options 要包括的选项
         * @return 一个新的 {@link Options} 实例
         */
        public static Options of(Option... options) {
            Assert.notNull(options, "Options must not be null");
            if (options.length == 0) {
                return NONE;
            }
            return new Options(EnumSet.copyOf(Arrays.asList(options)));
        }
    }

    /**
     * 可以应用的选项标志。
     */
    public enum Option {
        /**
         * 忽略源中的所有导入属性。
         */
        IGNORE_IMPORTS,
        /**
         * 忽略所有配置文件激活并包括属性。
         */
        IGNORE_PROFILES,
        /**
         * 指示源为“profile specific”，应在特定于配置文件的同级导入之后包含。
         */
        PROFILE_SPECIFIC;
    }
}
