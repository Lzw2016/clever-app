package org.clever.boot.env;

import org.clever.beans.factory.config.YamlProcessor;
import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginTrackedValue;
import org.clever.boot.origin.TextResourceOrigin;
import org.clever.core.io.Resource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.clever.boot.origin.TextResourceOrigin.Location;

/**
 * 要加载的类{@code .yml}文件转换成{@code String}到{@link OriginTrackedValue}的映射。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:45 <br/>
 */
class OriginTrackedYamlLoader extends YamlProcessor {
    private final Resource resource;

    OriginTrackedYamlLoader(Resource resource) {
        this.resource = resource;
        setResources(resource);
    }

    @Override
    protected Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        loaderOptions.setAllowRecursiveKeys(true);
        return createYaml(loaderOptions);
    }

    private Yaml createYaml(LoaderOptions loaderOptions) {
        BaseConstructor constructor = new OriginTrackingConstructor(loaderOptions);
        Representer representer = new Representer();
        DumperOptions dumperOptions = new DumperOptions();
        LimitedResolver resolver = new LimitedResolver();
        return new Yaml(constructor, representer, dumperOptions, loaderOptions, resolver);
    }

    List<Map<String, Object>> load() {
        final List<Map<String, Object>> result = new ArrayList<>();
        process((properties, map) -> result.add(getFlattenedMap(map)));
        return result;
    }

    /**
     * {@link Constructor} 追踪property起源。
     */
    private class OriginTrackingConstructor extends SafeConstructor {
        OriginTrackingConstructor(LoaderOptions loadingConfig) {
            super(loadingConfig);
        }

        @Override
        public Object getData() throws NoSuchElementException {
            Object data = super.getData();
            if (data instanceof CharSequence && ((CharSequence) data).length() == 0) {
                return null;
            }
            return data;
        }

        @Override
        protected Object constructObject(Node node) {
            if (node instanceof CollectionNode && ((CollectionNode<?>) node).getValue().isEmpty()) {
                return constructTrackedObject(node, super.constructObject(node));
            }
            if (node instanceof ScalarNode) {
                if (!(node instanceof KeyScalarNode)) {
                    return constructTrackedObject(node, super.constructObject(node));
                }
            }
            if (node instanceof MappingNode) {
                replaceMappingNodeKeys((MappingNode) node);
            }
            return super.constructObject(node);
        }

        private void replaceMappingNodeKeys(MappingNode node) {
            node.setValue(node.getValue().stream().map(KeyScalarNode::get).collect(Collectors.toList()));
        }

        private Object constructTrackedObject(Node node, Object value) {
            Origin origin = getOrigin(node);
            return OriginTrackedValue.of(getValue(value), origin);
        }

        private Object getValue(Object value) {
            return (value != null) ? value : "";
        }

        private Origin getOrigin(Node node) {
            Mark mark = node.getStartMark();
            Location location = new Location(mark.getLine(), mark.getColumn());
            return new TextResourceOrigin(OriginTrackedYamlLoader.this.resource, location);
        }
    }

    /**
     * {@link ScalarNode}，用于替换{@link NodeTuple}中的key节点。
     */
    private static class KeyScalarNode extends ScalarNode {
        KeyScalarNode(ScalarNode node) {
            super(node.getTag(), node.getValue(), node.getStartMark(), node.getEndMark(), node.getScalarStyle());
        }

        static NodeTuple get(NodeTuple nodeTuple) {
            Node keyNode = nodeTuple.getKeyNode();
            Node valueNode = nodeTuple.getValueNode();
            return new NodeTuple(KeyScalarNode.get(keyNode), valueNode);
        }

        private static Node get(Node node) {
            if (node instanceof ScalarNode) {
                return new KeyScalarNode((ScalarNode) node);
            }
            return node;
        }
    }

    /**
     * 限制{@link Tag#TIMESTAMP}标签的{@link Resolver}。
     */
    private static class LimitedResolver extends Resolver {
        @Override
        public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
            if (tag == Tag.TIMESTAMP) {
                return;
            }
            super.addImplicitResolver(tag, regexp, first);
        }
    }
}
