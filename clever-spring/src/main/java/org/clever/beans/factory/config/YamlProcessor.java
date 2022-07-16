package org.clever.beans.factory.config;

import org.clever.core.CollectionFactory;
import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML工厂的基类。
 *
 * <p>需要SnakeYAML 1.18或更高版本。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:45 <br/>
 */
public abstract class YamlProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;
    private Resource[] resources = new Resource[0];
    private List<DocumentMatcher> documentMatchers = Collections.emptyList();
    private boolean matchDefault = true;
    private Set<String> supportedTypes = Collections.emptySet();

    /**
     * 文档匹配器的映射，允许调用者仅选择性地使用YAML资源中的部分文档。
     * 在YAML中，文档由{@code ---}行分隔，在进行匹配之前，每个文档都转换为属性。
     * 例如:
     * <pre>{@code
     * environment: dev
     * url: https://dev.bar.com
     * name: Developer Setup
     * ---
     * environment: prod
     * url:https://foo.bar.com
     * name: My Cool App
     * }</pre>
     * 映射时使用
     * <pre>{@code
     *  setDocumentMatchers(
     *      properties ->
     *          ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND)
     *  );
     * }</pre>
     * 最终将成为
     * <pre>{@code
     * environment=prod
     * url=https://foo.bar.com
     * name=My Cool App
     * }</pre>
     */
    public void setDocumentMatchers(DocumentMatcher... matchers) {
        this.documentMatchers = Arrays.asList(matchers);
    }

    /**
     * Flag indicating that a document for which all the
     * {@link #setDocumentMatchers(DocumentMatcher...) document matchers} abstain will
     * nevertheless match. Default is {@code true}.
     */
    public void setMatchDefault(boolean matchDefault) {
        this.matchDefault = matchDefault;
    }

    /**
     * 用于解析资源的方法。
     * 每个资源都将转换为一个映射，因此此属性用于决定在此工厂的最终输出中保留哪些Map条目。
     * 默认值为 {@link ResolutionMethod#OVERRIDE}.
     */
    public void setResolutionMethod(ResolutionMethod resolutionMethod) {
        Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
        this.resolutionMethod = resolutionMethod;
    }

    /**
     * 设置要加载的YAML{@link Resource 资源}的位置。
     *
     * @see ResolutionMethod
     */
    public void setResources(Resource... resources) {
        this.resources = resources;
    }

    /**
     * 设置可从YAML文档加载的支持类型。
     * <p>如果没有配置支持的类型，则只支持YAML文档中遇到的Java标准类（如{@link org.yaml.snakeyaml.constructor.SafeConstructor}中所定义）。
     * 如果遇到不支持的类型，则在处理相应的YAML节点时将抛出{@link IllegalStateException}
     *
     * @param supportedTypes 支持的类型，或清除支持的类型的空数组
     * @see #createYaml()
     */
    public void setSupportedTypes(Class<?>... supportedTypes) {
        if (ObjectUtils.isEmpty(supportedTypes)) {
            this.supportedTypes = Collections.emptySet();
        } else {
            Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
            this.supportedTypes = Arrays.stream(supportedTypes)
                    .map(Class::getName)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toSet(),
                            Collections::unmodifiableSet
                    ));
        }
    }

    /**
     * 为子类提供处理从提供的资源解析的Yaml的机会。
     * 依次解析每个资源，并根据{@link #setDocumentMatchers(DocumentMatcher...) 匹配器}检查其中的文档。
     * 如果文档与之匹配，则将其传递到回调中，并将其表示为属性。
     * 根据{@link #setResolutionMethod(ResolutionMethod)}，并非所有文档都将被解析。
     *
     * @param callback 找到匹配的文档后，回调委托给
     * @see #createYaml()
     */
    protected void process(MatchCallback callback) {
        Yaml yaml = createYaml();
        for (Resource resource : this.resources) {
            boolean found = process(callback, yaml, resource);
            if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
                return;
            }
        }
    }

    /**
     * 创建要使用的{@link Yaml}实例。
     * <p>默认实现将“allowDuplicateKeys”标志设置为false，从而启用SnakeYAML 1.18+中内置的重复密钥处理。
     * <p>如果已经配置了自定义{@linkplain #setSupportedTypes 支持的类型}，则默认实现将创建一个Yaml实例，
     * 用于过滤Yaml文档中遇到的不支持的类型。如果遇到不支持的类型，则在处理节点时将引发{@link IllegalStateException}
     *
     * @see LoaderOptions#setAllowDuplicateKeys(boolean)
     */
    protected Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        return new Yaml(
                new FilteringConstructor(loaderOptions),
                new Representer(),
                new DumperOptions(), loaderOptions
        );
    }

    private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
        int count = 0;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading from YAML: " + resource);
            }
            try (Reader reader = new UnicodeReader(resource.getInputStream())) {
                for (Object object : yaml.loadAll(reader)) {
                    if (object != null && process(asMap(object), callback)) {
                        count++;
                        if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
                            break;
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "")
                            + " from YAML resource: " + resource
                    );
                }
            }
        } catch (IOException ex) {
            handleProcessError(resource, ex);
        }
        return (count > 0);
    }

    private void handleProcessError(Resource resource, IOException ex) {
        if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND
                && this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
            throw new IllegalStateException(ex);
        }
        if (logger.isWarnEnabled()) {
            logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object object) {
        // YAML can have numbers as keys
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            // A document can be a text literal
            result.put("document", object);
            return result;
        }
        Map<Object, Object> map = (Map<Object, Object>) object;
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                value = asMap(value);
            }
            if (key instanceof CharSequence) {
                result.put(key.toString(), value);
            } else {
                // It has to be a map key in this case
                result.put("[" + key.toString() + "]", value);
            }
        });
        return result;
    }

    private boolean process(Map<String, Object> map, MatchCallback callback) {
        Properties properties = CollectionFactory.createStringAdaptingProperties();
        properties.putAll(getFlattenedMap(map));
        if (this.documentMatchers.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Merging document (no matchers set): " + map);
            }
            callback.process(properties, map);
            return true;
        }
        MatchStatus result = MatchStatus.ABSTAIN;
        for (DocumentMatcher matcher : this.documentMatchers) {
            MatchStatus match = matcher.matches(properties);
            result = MatchStatus.getMostSpecific(match, result);
            if (match == MatchStatus.FOUND) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Matched document with document matcher: " + properties);
                }
                callback.process(properties, map);
                return true;
            }
        }
        if (result == MatchStatus.ABSTAIN && this.matchDefault) {
            if (logger.isDebugEnabled()) {
                logger.debug("Matched document with default matcher: " + map);
            }
            callback.process(properties, map);
            return true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Unmatched document: " + map);
        }
        return false;
    }

    /**
     * 返回给定映射的展平版本，递归地跟随任何嵌套的映射或集合值。
     * 结果映射中的条目保留与源相同的顺序。
     * 从{@link MatchCallback}使用映射调用时，结果将包含与{@link MatchCallback}属性相同的值。
     *
     * @param source 源地图
     * @return 展平的地图
     */
    protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        source.forEach((key, value) -> {
            if (StringUtils.hasText(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                if (collection.isEmpty()) {
                    result.put(key, "");
                } else {
                    int count = 0;
                    for (Object object : collection) {
                        buildFlattenedMap(
                                result,
                                Collections.singletonMap("[" + (count++) + "]", object),
                                key
                        );
                    }
                }
            } else {
                result.put(key, (value != null ? value : ""));
            }
        });
    }

    /**
     * 用于处理YAML解析结果的回调接口。
     */
    @FunctionalInterface
    public interface MatchCallback {
        /**
         * 处理解析结果的给定表示。
         *
         * @param properties 要处理的属性（对于集合或映射，作为带索引键的平坦表示）
         * @param map        结果图（保留YAML文档中的原始值结构）
         */
        void process(Properties properties, Map<String, Object> map);
    }

    /**
     * 用于测试属性是否匹配的策略接口。
     */
    @FunctionalInterface
    public interface DocumentMatcher {
        /**
         * 测试给定属性是否匹配。
         *
         * @param properties 要测试的属性
         * @return 匹配的状态
         */
        MatchStatus matches(Properties properties);
    }

    /**
     * 从{@link DocumentMatcher#matches(java.util.Properties)}返回的状态
     */
    public enum MatchStatus {
        /**
         * 找到匹配项。
         */
        FOUND,
        /**
         * 未找到匹配项。
         */
        NOT_FOUND,
        /**
         * 不应考虑匹配器。
         */
        ABSTAIN;

        /**
         * 比较两个{@link MatchStatus}项，返回最具体的状态。
         */
        public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
            return (a.ordinal() < b.ordinal() ? a : b);
        }
    }

    /**
     * 用于解析资源的方法。
     */
    public enum ResolutionMethod {
        /**
         * 替换列表中前面的值。
         */
        OVERRIDE,
        /**
         * 替换列表中早期的值，忽略任何故障。
         */
        OVERRIDE_AND_IGNORE,
        /**
         * 取列表中存在的第一个资源并仅使用它。
         */
        FIRST_FOUND
    }

    /**
     * {@link Constructor} 支持过滤不支持的类型。
     * <p>如果在YAML文档中遇到不支持的类型，则会从{@link #getClassForName}中引发{@link IllegalStateException}。
     */
    private class FilteringConstructor extends Constructor {
        FilteringConstructor(LoaderOptions loaderOptions) {
            super(loaderOptions);
        }

        @Override
        protected Class<?> getClassForName(String name) throws ClassNotFoundException {
            Assert.state(
                    YamlProcessor.this.supportedTypes.contains(name),
                    () -> "Unsupported type encountered in YAML document: " + name
            );
            return super.getClassForName(name);
        }
    }
}
