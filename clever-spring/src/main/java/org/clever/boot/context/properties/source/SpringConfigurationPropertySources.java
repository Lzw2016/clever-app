package org.clever.boot.context.properties.source;

import org.clever.boot.origin.OriginLookup;
import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.MutablePropertySources;
import org.clever.core.env.PropertySource;
import org.clever.core.env.PropertySource.StubPropertySource;
import org.clever.util.Assert;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ConcurrentReferenceHashMap.ReferenceType;

import java.util.*;
import java.util.function.Function;

/**
 * 将{@link MutablePropertySources}转换为{@link ConfigurationPropertySource ConfigurationPropertySources}的适配器。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:56 <br/>
 */
class SpringConfigurationPropertySources implements Iterable<ConfigurationPropertySource> {
    private final Iterable<PropertySource<?>> sources;
    private final Map<PropertySource<?>, ConfigurationPropertySource> cache = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    SpringConfigurationPropertySources(Iterable<PropertySource<?>> sources) {
        Assert.notNull(sources, "Sources must not be null");
        this.sources = sources;
    }

    boolean isUsingSources(Iterable<PropertySource<?>> sources) {
        return this.sources == sources;
    }

    @Override
    public Iterator<ConfigurationPropertySource> iterator() {
        return new SourcesIterator(this.sources.iterator(), this::adapt);
    }

    private ConfigurationPropertySource adapt(PropertySource<?> source) {
        ConfigurationPropertySource result = this.cache.get(source);
        // Most PropertySources test equality only using the source name, so we need to
        // check the actual source hasn't also changed.
        if (result != null && result.getUnderlyingSource() == source) {
            return result;
        }
        result = SpringConfigurationPropertySource.from(source);
        if (source instanceof OriginLookup) {
            result = result.withPrefix(((OriginLookup<?>) source).getPrefix());
        }
        this.cache.put(source, result);
        return result;
    }

    private static class SourcesIterator implements Iterator<ConfigurationPropertySource> {
        private final Deque<Iterator<PropertySource<?>>> iterators;
        private ConfigurationPropertySource next;
        private final Function<PropertySource<?>, ConfigurationPropertySource> adapter;

        SourcesIterator(Iterator<PropertySource<?>> iterator, Function<PropertySource<?>, ConfigurationPropertySource> adapter) {
            this.iterators = new ArrayDeque<>(4);
            this.iterators.push(iterator);
            this.adapter = adapter;
        }

        @Override
        public boolean hasNext() {
            return fetchNext() != null;
        }

        @Override
        public ConfigurationPropertySource next() {
            ConfigurationPropertySource next = fetchNext();
            if (next == null) {
                throw new NoSuchElementException();
            }
            this.next = null;
            return next;
        }

        private ConfigurationPropertySource fetchNext() {
            if (this.next == null) {
                if (this.iterators.isEmpty()) {
                    return null;
                }
                if (!this.iterators.peek().hasNext()) {
                    this.iterators.pop();
                    return fetchNext();
                }
                PropertySource<?> candidate = this.iterators.peek().next();
                if (candidate.getSource() instanceof ConfigurableEnvironment) {
                    push((ConfigurableEnvironment) candidate.getSource());
                    return fetchNext();
                }
                if (isIgnored(candidate)) {
                    return fetchNext();
                }
                this.next = this.adapter.apply(candidate);
            }
            return this.next;
        }

        private void push(ConfigurableEnvironment environment) {
            this.iterators.push(environment.getPropertySources().iterator());
        }

        private boolean isIgnored(PropertySource<?> candidate) {
            return (candidate instanceof StubPropertySource || candidate instanceof ConfigurationPropertySourcesPropertySource);
        }
    }
}
